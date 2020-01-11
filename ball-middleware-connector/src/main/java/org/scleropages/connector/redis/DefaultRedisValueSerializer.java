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
import org.scleropages.serialize.SerializerFactoryUtil;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DefaultRedisValueSerializer implements RedisSerializer<Object> {


    private final SerializerFactory<InputStream, OutputStream> serializerFactory;


    public DefaultRedisValueSerializer(SerializerFactory<InputStream, OutputStream> serializerFactory) {
        this.serializerFactory = serializerFactory;
    }

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        try {
            return null != o ? SerializerFactoryUtil.serialize(serializerFactory, o) : null;
        } catch (IOException e) {
            throw new SerializationException("failure to serialize.", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        try {
            return null != bytes ? SerializerFactoryUtil.deserialize(serializerFactory, bytes) : null;
        } catch (Exception e) {
            throw new SerializationException("failure to deserialize.", e);
        }
    }
}
