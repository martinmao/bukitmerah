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
package org.scleropages.serialize.jdk;

import org.scleropages.core.ComponentId;
import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.spi.CompositeSerializer;
import org.springframework.util.ClassUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JdkSerializerFactory implements SerializerFactory<InputStream, OutputStream>, ComponentId {


    @Override
    public DataObjectReader<InputStream> deserialize(InputStream inputStream) {
        JdkDataSerializer jdkDataSerializer = new JdkDataSerializer(inputStream);
        JdkObjectSerializer jdkObjectSerializer = new JdkObjectSerializer(inputStream);
        return new CompositeSerializer<>(jdkDataSerializer, jdkObjectSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream outputStream) {
        JdkDataSerializer jdkDataSerializer = new JdkDataSerializer(outputStream);
        JdkObjectSerializer jdkObjectSerializer = new JdkObjectSerializer(outputStream);
        return new CompositeSerializer<>(jdkDataSerializer, jdkObjectSerializer);
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
        return 0x04;
    }

    @Override
    public String id() {
        return "jdk";
    }

    @Override
    public boolean supportClassRegistry() {
        return false;
    }
}
