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
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.apache.commons.lang3.ArrayUtils;
import org.scleropages.core.util.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ClusterCommandExecutor;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterCommands;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.lettuce.LettuceClusterConnection;
import org.springframework.data.redis.core.RedisClusterCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 将redis cluster各管理各功能以mbean形式暴露出去
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@ManagedResource(objectName = "org.scleropages.connector:name=redis-cluster", description = "redis cluster client instance.")
public class RedisClusterManager {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final RedisConnectionFactory redisConnectionFactory;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisClusterManager(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisConnectionFactory = stringRedisTemplate.getRequiredConnectionFactory();
    }

    public <T> T execute(RedisClusterCallback<T> callback) {

        Assert.notNull(callback, "ClusterCallback must not be null!");

        RedisClusterConnection connection = redisConnectionFactory.getClusterConnection();
        try {
            return callback.doInRedis(connection);
        } finally {
            if (null != connection)
                connection.close();
        }
    }

    @ManagedOperation
    public ClusterInfo clusterInfo() {
        return execute(connection -> connection.clusterGetClusterInfo());
    }


    /**
     * 使用非api兼容执行重新加载partitions.当前实现强制依赖lettuce.
     */
    @ManagedOperation
    public void reloadPartitions() {
        logger.info("reload partitions.");
        try {
            AbstractRedisClient abstractRedisClient = (AbstractRedisClient) Reflections.getFieldValue(redisConnectionFactory, "client");
            if (abstractRedisClient instanceof RedisClusterClient) {
                ((RedisClusterClient) abstractRedisClient).reloadPartitions();
            } else {
                logger.warn("not a cluster client configuration.");
            }
        } catch (Exception e) {
            logger.warn("failure to reload partitions. may implementation used redis client(lettuce) not match configured.", e);
        }
    }

    @ManagedOperation(description = "将指定 redis 实例加入redis cluster")
    public void meet(String host, int port) {
        stringRedisTemplate.opsForCluster().meet(fromAddress(host, port));
    }

    @ManagedOperation(description = "对指定node 分配 slots区间")
    public void assignSlots(String nodeId, int start, int end) {
        stringRedisTemplate.opsForCluster().addSlots(fromId(nodeId), new RedisClusterNode.SlotRange(start, end));
    }

    @ManagedOperation(description = "删除指定node 已分配 slots区间")
    public void removeAssignedSlots(String nodeId, int start, int end) {
        execute(connection -> {
            connection.clusterDeleteSlotsInRange(fromId(nodeId), new RedisClusterNode.SlotRange(start, end));
            return null;
        });
    }

    @ManagedOperation(description = "指定节点变为master，原master变为slave")
    public void failover(String toMasterNodeId, boolean force) {
        execute(connection -> {
            if (connection instanceof LettuceClusterConnection) {
                ((LettuceClusterConnection) connection).getClusterCommandExecutor().executeCommandOnSingleNode((LettuceClusterCommandCallback<Object>) client -> {
                    client.clusterFailover(force);
                    return null;
                }, fromId(toMasterNodeId));
            }
            return null;
        });
    }

    @ManagedOperation(description = "重置集群")
    public void clusterReset(boolean forceFlushAll, boolean hard) {
        execute(connection -> {
            if (connection instanceof LettuceClusterConnection) {
                ((LettuceClusterConnection) connection).getClusterCommandExecutor().executeCommandOnAllNodes((LettuceClusterCommandCallback<Object>) client -> {
                    if (forceFlushAll)
                        client.flushall();
                    client.clusterReset(hard);
                    return null;
                });
            }
            return null;
        });
    }

    public interface LettuceClusterCommandCallback<T>
            extends ClusterCommandExecutor.ClusterCommandCallback<io.lettuce.core.cluster.api.sync.RedisClusterCommands<byte[], byte[]>, T> {
    }


    @ManagedOperation(description = "对指定node 分配 slots区间")
    public void replicate(String masterId, String slaveId) {
        execute(connection -> {
            connection.clusterReplicate(fromId(masterId), fromId(slaveId));
            return null;
        });
    }

    @ManagedOperation(description = "redis cluster slot迁移，如果slot数据量巨大 可以使用 migratingSlotPrepare--> migratingSlotProcess(多次)--> migratingSlotDone")
    public void migratingSlot(String sourceId, String targetId, int slot) {

        RedisClusterNode source = fromId(sourceId);
        RedisClusterNode target = fromId(targetId);

        stringRedisTemplate.opsForCluster().reshard(source, slot, target);
    }

    @ManagedOperation(description = "准备redis cluster slot 迁移，设置slot状态")
    public void migratingSlotPrepare(String sourceId, String targetId, int slot) {
        RedisClusterNode source = fromId(sourceId);
        RedisClusterNode target = fromId(targetId);
        execute((RedisClusterCallback<Void>) connection -> {
            connection.clusterSetSlot(target, slot, RedisClusterCommands.AddSlots.IMPORTING);
            connection.clusterSetSlot(source, slot, RedisClusterCommands.AddSlots.MIGRATING);
            return null;
        });
    }

    @ManagedOperation(description = "执行redis cluster slot 迁移，可多次执行直至完毕")
    public void migratingSlotProcess(String sourceId, int slot, int size, long timeout) {

        int _size = -1 == size ? Integer.MAX_VALUE : size;
        long _timeout = -1 == timeout ? Long.MAX_VALUE : timeout;
        RedisClusterNode source = fromId(sourceId);

        execute((RedisClusterCallback<Void>) connection -> {
            List<byte[]> keys = connection.clusterGetKeysInSlot(slot, _size);
            for (byte[] key : keys) {
                connection.migrate(key, source, 0, RedisServerCommands.MigrateOption.COPY, _timeout);
            }
            return null;
        });
    }

    @ManagedOperation(description = "通知redis cluster 所有节点，slot已完成迁移至新节点")
    public void migratingSlotDone(String targetId, int slot) {

        RedisClusterNode target = fromId(targetId);

        execute((RedisClusterCallback<Void>) connection -> {
            Long numberOfKeys = connection.clusterCountKeysInSlot(slot);
            Assert.state(0 == numberOfKeys, "migrating not complete. there are [" + numberOfKeys + "] keys that have not yet been migrated");
            connection.clusterSetSlot(target, slot, RedisClusterCommands.AddSlots.NODE);
            return null;
        });
    }

    @ManagedOperation(description = "查看info，nodeId=-1查看所有节点，section=-1查看所有信息")
    public Properties info(String nodeId, String section) {
        String _section = "-1".equals(section) ? null : section;
        String _nodeId = "-1".equals(nodeId) ? null : nodeId;

        return execute(connection -> {
            if (StringUtils.hasText(_section)) {
                if (StringUtils.hasText(_nodeId))
                    return connection.info(fromId(_nodeId), _section);
                return connection.info(_section);
            } else {
                if (StringUtils.hasText(_nodeId))
                    return connection.info(fromId(_nodeId));
                return connection.info();
            }
        });
    }

    @ManagedOperation(description = "查看config，nodeId=-1查看所有节点，pattern=-1查看所有配置")
    public Properties config(String nodeId, String pattern) {
        String _pattern = "-1".equals(pattern) ? "*" : pattern;
        String _nodeId = "-1".equals(nodeId) ? null : nodeId;
        return execute(connection -> {
            if (StringUtils.hasText(_nodeId))
                return connection.getConfig(fromId(_nodeId), _pattern);
            else
                return connection.getConfig(_pattern);

        });
    }


    @ManagedOperation(description = "获取集群成员列表，本地有缓存，可通过reloadPartitions刷新缓存")
    public Collection<Map<String, Object>> clusterNodes() {

        Map<String, Map<String, Object>> masters = Maps.newHashMap();
        List<Map<String, Object>> slaves = Lists.newArrayList();

        return execute(connection -> {
            Iterable<RedisClusterNode> redisClusterNodes = connection.clusterGetNodes();
            redisClusterNodes.forEach(redisClusterNode -> {
                if (redisClusterNode.isMaster()) {
                    masters.put(redisClusterNode.getId(), clusterNodeAsMap(redisClusterNode));
                } else if (redisClusterNode.isSlave())
                    slaves.add(clusterNodeAsMap(redisClusterNode));
            });
            slaves.forEach(slaveMap -> {
                Map<String, Object> master = masters.get(slaveMap.get("masterId"));
                Assert.notNull(master, "no master found by given masterId: " + slaveMap.get("masterId"));
                List<Map<String, Object>> _slaves = (List<Map<String, Object>>) master.computeIfAbsent("slaves", key -> Lists.newArrayList());
                _slaves.add(slaveMap);
            });
            return masters.values();
        });
    }

    protected RedisClusterNode fromId(String nodeId) {
        return RedisClusterNode.newRedisClusterNode().withId(nodeId).build();
    }

    protected RedisClusterNode fromAddress(String host, int port) {
        return RedisClusterNode.newRedisClusterNode().listeningAt(host, port).build();
    }

    protected Map<String, Object> clusterNodeAsMap(RedisClusterNode redisClusterNode) {
        Map<String, Object> clusterNodeInfo = Maps.newHashMap();
        clusterNodeInfo.put("id", redisClusterNode.getId());
        clusterNodeInfo.put("address", redisClusterNode.getHost() + ":" + redisClusterNode.getPort());
        clusterNodeInfo.put("flags", redisClusterNode.getFlags());
        clusterNodeInfo.put("connected", redisClusterNode.isConnected());
        if (redisClusterNode.isMaster()) {
            int[] slots = redisClusterNode.getSlotRange().getSlotsArray();
            if (ArrayUtils.isNotEmpty(slots)) {
                Arrays.sort(slots);
                clusterNodeInfo.put("slots", "[" + slots[0] + "~" + slots[slots.length - 1] + "]");
            } else
                clusterNodeInfo.put("slots", "[]");

        } else if (redisClusterNode.isSlave()) {
            clusterNodeInfo.put("masterId", redisClusterNode.getMasterId());
        }
        clusterNodeInfo.put("markedAsFail", redisClusterNode.isMarkedAsFail());
        return clusterNodeInfo;
    }
}
