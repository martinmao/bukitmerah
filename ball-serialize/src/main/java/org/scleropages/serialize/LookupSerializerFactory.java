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
package org.scleropages.serialize;

import org.scleropages.core.AbstractLookupComponent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class LookupSerializerFactory extends AbstractLookupComponent implements SerializerFactory<InputStream, OutputStream> {


    private static final InheritableThreadLocal<Object> currentSerializerFactoryKey =
            new InheritableThreadLocal<>();


    @Override
    public Class getKeyClass() {
        return String.class;
    }

    @Override
    public Class getComponentClass() {
        return SerializerFactory.class;
    }

    @Override
    protected SerializerFactory<InputStream, OutputStream> determineTargetComponent() {
        return (SerializerFactory) super.determineTargetComponent();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return currentSerializerFactoryKey.get();
    }

    @Override
    public void setCurrentLookupKey(Object key) {
        currentSerializerFactoryKey.set(key);
    }

    @Override
    public void resetCurrentLookupKey() {
        currentSerializerFactoryKey.remove();
    }


    @Override
    public DataObjectReader<InputStream> deserialize(InputStream inputStream) {
        return determineTargetComponent().deserialize(inputStream);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream outputStream) {
        return determineTargetComponent().serialize(outputStream);
    }

    @Override
    public boolean supportSerialize(Class<?> clazz) {
        return determineTargetComponent().supportSerialize(clazz);
    }

    @Override
    public boolean supportDeserialize(Class<?> clazz) {
        return determineTargetComponent().supportDeserialize(clazz);
    }

    @Override
    public Set<Class<?>> serializableClassesSet() {
        return determineTargetComponent().serializableClassesSet();
    }

    @Override
    public byte factoryId() {
        return determineTargetComponent().factoryId();
    }

    @Override
    public boolean supportClassRegistry() {
        return determineTargetComponent().supportClassRegistry();
    }
}
