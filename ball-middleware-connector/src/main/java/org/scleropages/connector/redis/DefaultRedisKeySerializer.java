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

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DefaultRedisKeySerializer implements RedisSerializer<RedisKey> {

    private final RedisSerializer<String> stringRedisSerializer = RedisSerializer.string();


    @Override
    public byte[] serialize(RedisKey redisKey) throws SerializationException {
        return null != redisKey ? stringRedisSerializer.serialize(redisKey.evalKey()) : null;
    }

    @Override
    public RedisKey deserialize(byte[] bytes) throws SerializationException {
        return null != bytes ? RedisKey.RedisKeyBuilder.fromKey(stringRedisSerializer.deserialize(bytes)).build() : null;
    }
}
