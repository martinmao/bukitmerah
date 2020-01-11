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
package org.scleropages.serialize.msgpack.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.scleropages.core.ComponentId;
import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.spi.CompositeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackSerializerFactory8 implements SerializerFactory<InputStream, OutputStream>, ComponentId {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;


    public MsgPackSerializerFactory8() {
        this.objectMapper = new ObjectMapper(new MessagePackFactory());
            //序列化时加入属性 描述其类型（java完整类路径）会导致消息剧增，接近json本身大小
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        if (StringUtils.hasText(jacksonClassPropertyTypeName())) {
//            objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, jacksonClassPropertyTypeName());
//        } else
//            objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }

    protected String jacksonClassPropertyTypeName() {
        return null;
    }


    @Override
    public DataObjectReader<InputStream> deserialize(InputStream r) {
        MsgPackDataSerializer8 dataSerializer = new MsgPackDataSerializer8(r);
        MsgPackObjectSerializer8 msgPackObjectSerializer = new MsgPackObjectSerializer8(r, objectMapper);
        return new CompositeSerializer<>(dataSerializer, msgPackObjectSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream w) {
        MsgPackDataSerializer8 dataSerializer = new MsgPackDataSerializer8(w);
        MsgPackObjectSerializer8 msgPackObjectSerializer = new MsgPackObjectSerializer8(w, objectMapper);
        return new CompositeSerializer<>(dataSerializer, msgPackObjectSerializer);
    }

    @Override
    public boolean supportSerialize(Class<?> clazz) {
        return ClassUtils.isAssignable(OutputStream.class, clazz);
    }

    @Override
    public boolean supportDeserialize(Class<?> clazz) {
        return ClassUtils.isAssignable(InputStream.class, clazz);
    }

    @Override
    public Set<Class<?>> serializableClassesSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte factoryId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String id() {
        return "msgpackv8";
    }

    @Override
    public boolean supportClassRegistry() {
        return false;
    }
}
