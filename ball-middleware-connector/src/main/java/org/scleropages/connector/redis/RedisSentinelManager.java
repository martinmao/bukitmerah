/**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.connector.redis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@ManagedResource(objectName = "org.scleropages.connector:name=redis-sentinel", description = "redis client instance.")
public class RedisSentinelManager {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final RedisConnectionFactory redisConnectionFactory;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSentinelManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisConnectionFactory = stringRedisTemplate.getRequiredConnectionFactory();
    }

    public interface RedisSentinelCallback<T> {
        T doInRedis(RedisSentinelConnection sentinelConnection);
    }

    protected <T> T execute(RedisSentinelCallback<T> callback) {

        Assert.notNull(callback, "ClusterCallback must not be null!");

        RedisSentinelConnection connection = redisConnectionFactory.getSentinelConnection();
        try {
            return callback.doInRedis(connection);
        } finally {
            try {
                if (null != connection)
                    connection.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    @ManagedOperation
    public List<Map<String, Object>> nodes() {

        List<Map<String, Object>> masters = Lists.newArrayList();
        return execute(sentinelConnection -> {
            sentinelConnection.masters().forEach(redisServer -> {
                RedisServer.INFO[] values = RedisServer.INFO.values();
                Map<String, Object> master = Maps.newHashMap();
                for (RedisServer.INFO info : values) {
                    master.put(info.name().toLowerCase(), redisServer.get(info));
                }
                masters.add(master);
                master.put("slaves", sentinelConnection.slaves(redisServer));
            });
            return masters;
        });
    }

    @ManagedOperation(description = "指定节点变为master，原master变为slave")
    public void failover(String toMasterNodeId) {
        execute(sentinelConnection -> {
            sentinelConnection.failover(RedisNode.newRedisNode().withId(toMasterNodeId).build());
            return null;
        });
    }
}
