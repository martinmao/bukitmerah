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

import org.scleropages.serialize.SerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
public class RedisConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final RedisConnectionFactory redisConnectionFactory;

    private final SerializerFactory serializerFactory;

    private final DefaultRedisKeySerializer defaultRedisKeySerializer;

    private final DefaultRedisValueSerializer defaultRedisValueSerializer;


    public RedisConfiguration(RedisConnectionFactory redisConnectionFactory, SerializerFactory serializerFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.serializerFactory = serializerFactory;
        defaultRedisKeySerializer = new DefaultRedisKeySerializer();
        defaultRedisValueSerializer = new DefaultRedisValueSerializer(this.serializerFactory);
    }

    @Bean
    public RedisTemplate<RedisKey, String> defaultStringRedisTemplate() {
        RedisTemplate<RedisKey, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(defaultRedisKeySerializer);
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.string());
        return redisTemplate;
    }


    @Bean
    public RedisTemplate<RedisKey, Object> defaultObjectRedisTemplate() {
        RedisTemplate<RedisKey, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(defaultRedisKeySerializer);
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(defaultRedisValueSerializer);
        redisTemplate.setHashValueSerializer(defaultRedisValueSerializer);
        return redisTemplate;
    }


    @Bean
    public DefaultRedisClient defaultRedisClient() {
        DefaultRedisClient defaultRedisClient = new DefaultRedisClient(defaultStringRedisTemplate(), defaultObjectRedisTemplate());
        return defaultRedisClient;
    }


    @Bean
    public RedisClusterManager redisClusterManager(StringRedisTemplate stringRedisTemplate) {
        RedisClusterManager redisClusterManager = new RedisClusterManager(stringRedisTemplate);
        return redisClusterManager;
    }

    @Bean
    RedisSentinelManager redisSentinelManager(StringRedisTemplate stringRedisTemplate) {
        RedisSentinelManager redisSentinelManager = new RedisSentinelManager(stringRedisTemplate);
        return redisSentinelManager;
    }
}
