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

import org.springframework.data.redis.core.ClusterOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SpringRedisClientSupport implements RedisClient {

    private final RedisTemplate<RedisKey, String> stringRedisTemplate;

    private final RedisTemplate<RedisKey, Object> objectRedisTemplate;

    private final ClusterOperations<RedisKey, String> clusterOperations;

    /*~~~string value operations */
    private final ValueOperations<RedisKey, String> stringValueOperations;

    private final HashOperations<RedisKey, String, String> stringHashOperations;

    private final ListOperations<RedisKey, String> stringListOperations;

    private final SetOperations<RedisKey, String> stringSetOperations;

    private final ZSetOperations<RedisKey, String> stringZSetOperations;

    /*~~~object value operations */
    private final ValueOperations<RedisKey, Object> objectValueOperations;

    private final HashOperations<RedisKey, String, Object> objectHashOperations;

    private final ListOperations<RedisKey, Object> objectListOperations;

    private final SetOperations<RedisKey, Object> objectSetOperations;

    private final ZSetOperations<RedisKey, Object> objectZSetOperations;


    public SpringRedisClientSupport(RedisTemplate<RedisKey, String> stringRedisTemplate, RedisTemplate<RedisKey, Object> objectRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectRedisTemplate = objectRedisTemplate;

        this.clusterOperations = stringRedisTemplate.opsForCluster();
        this.stringValueOperations = stringRedisTemplate.opsForValue();
        this.stringHashOperations = stringRedisTemplate.opsForHash();
        this.stringListOperations = stringRedisTemplate.opsForList();
        this.stringSetOperations = stringRedisTemplate.opsForSet();
        this.stringZSetOperations = stringRedisTemplate.opsForZSet();

        this.objectValueOperations = objectRedisTemplate.opsForValue();
        this.objectHashOperations = objectRedisTemplate.opsForHash();
        this.objectListOperations = objectRedisTemplate.opsForList();
        this.objectSetOperations = objectRedisTemplate.opsForSet();
        this.objectZSetOperations = objectRedisTemplate.opsForZSet();
    }


    @Override
    public ClusterOperations<RedisKey, String> clusterOperations() {
        return clusterOperations;
    }


    @Override
    public ValueOperations<RedisKey, String> stringValueOperations() {
        return stringValueOperations;
    }


    @Override
    public HashOperations<RedisKey, String, String> stringHashOperations() {
        return stringHashOperations;
    }


    @Override
    public ListOperations<RedisKey, String> stringListOperations() {
        return stringListOperations;
    }


    @Override
    public SetOperations<RedisKey, String> stringSetOperations() {
        return stringSetOperations;
    }


    @Override
    public ZSetOperations<RedisKey, String> stringZSetOperations() {
        return stringZSetOperations;
    }


    @Override
    public RedisTemplate<RedisKey, String> stringValueTemplate() {
        return stringRedisTemplate;
    }


    @Override
    public ValueOperations<RedisKey, Object> objectValueOperations() {
        return objectValueOperations;
    }


    @Override
    public HashOperations<RedisKey, String, Object> objectHashOperations() {
        return objectHashOperations;
    }


    @Override
    public ListOperations<RedisKey, Object> objectListOperations() {
        return objectListOperations;
    }


    @Override
    public SetOperations<RedisKey, Object> objectSetOperations() {
        return objectSetOperations;
    }


    @Override
    public ZSetOperations<RedisKey, Object> objectZSetOperations() {
        return objectZSetOperations;
    }


    @Override
    public RedisTemplate<RedisKey, Object> objectValueTemplate() {
        return objectRedisTemplate;
    }


}
