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
package org.scleropages.serialize.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scleropages.core.ComponentId;
import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.spi.CompositeSerializer;
import org.scleropages.serialize.spi.UnsupportedDataSerializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JacksonSerializerFactory implements SerializerFactory<InputStream, OutputStream>, ComponentId {


    private final ObjectMapper objectMapper;

    private final UnsupportedDataSerializer unsupportedDataSerializer = new UnsupportedDataSerializer();

    public JacksonSerializerFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JacksonSerializerFactory() {
        this.objectMapper = new ObjectMapper();
        postProcessDefaultObjectMapper();
    }

    protected void postProcessDefaultObjectMapper() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (StringUtils.hasText(jacksonClassPropertyTypeName())) {
            objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.NON_FINAL, jacksonClassPropertyTypeName());
        } else
            objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    }


    protected String jacksonClassPropertyTypeName() {
        return null;
    }


    @Override
    public DataObjectReader<InputStream> deserialize(InputStream inputStream) {
        JacksonObjectStreamSerializer jacksonObjectStreamSerializer = new JacksonObjectStreamSerializer(inputStream, objectMapper);
        return new CompositeSerializer<>(unsupportedDataSerializer, jacksonObjectStreamSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream outputStream) {
        JacksonObjectStreamSerializer jacksonObjectStreamSerializer = new JacksonObjectStreamSerializer(outputStream, objectMapper);
        return new CompositeSerializer<>(unsupportedDataSerializer, jacksonObjectStreamSerializer);
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
        return 0x05;
    }

    @Override
    public String id() {
        return "jackson";
    }

    @Override
    public boolean supportClassRegistry() {
        return false;
    }
}
