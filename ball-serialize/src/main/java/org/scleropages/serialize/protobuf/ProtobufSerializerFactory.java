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
package org.scleropages.serialize.protobuf;

import org.scleropages.core.ComponentId;
import org.scleropages.core.util.ClassPathScanner;
import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.scleropages.serialize.SerialIdRegistry;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.spi.CompositeSerializer;
import org.scleropages.core.util.ClassPathScanner.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ProtobufSerializerFactory implements SerializerFactory<InputStream, OutputStream>, ComponentId {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final ProtobufIdRegistry registry = new ProtobufIdRegistry();

    public ProtobufSerializerFactory(final String basePackege) {
        try {
            ClassPathScanner.scanClasses(basePackege, new ScanListener() {
                @Override
                public void onMatch(MetadataReader metadataReader) {
                    try {
                        Entry<Short, Class<?>> registered = registry
                                .register(metadataReader.getClassMetadata().getClassName());
                        logger.debug("registered [{}] as [{}] to protobuf registry.", registered.getValue().getName(),
                                registered.getKey());
                    } catch (ClassNotFoundException | LinkageError e) {
                        throw new IllegalStateException(e);
                    }
                }
            }, ProtobufId.class, false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public DataObjectReader<InputStream> deserialize(InputStream r) {
        ProtobufDataSerializer dataSerializer = new ProtobufDataSerializer(r);
        ProtobufObjectSerializer objectSerializer = new ProtobufObjectSerializer(r, registry);
        return new CompositeSerializer<>(dataSerializer, objectSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream w) {
        ProtobufDataSerializer dataSerializer = new ProtobufDataSerializer(w);
        ProtobufObjectSerializer objectSerializer = new ProtobufObjectSerializer(w, registry);
        return new CompositeSerializer<>(dataSerializer, objectSerializer);
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
        return registry.classesSet();
    }

    @Override
    public byte factoryId() {
        return 0x03;
    }

    @Override
    public String id() {
        return "protobuf";
    }


    @Override
    public SerialIdRegistry getClassRegistry() {
        return registry;
    }
}
