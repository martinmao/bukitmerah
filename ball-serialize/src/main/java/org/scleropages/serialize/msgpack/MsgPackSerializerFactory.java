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
package org.scleropages.serialize.msgpack;

import org.scleropages.core.ComponentId;
import org.scleropages.core.util.ClassPathScanner;
import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.scleropages.serialize.SerialIdRegistry;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.spi.CompositeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackSerializerFactory implements SerializerFactory<InputStream, OutputStream>, ComponentId {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private String basePackageToScan;

    private MsgPackIdRegistry msgPackRegistry;

    public MsgPackSerializerFactory() {

    }

    private AtomicBoolean initFlag = new AtomicBoolean(false);

    public synchronized void initFactory() {
        if (initFlag.compareAndSet(false, true)) {
            msgPackRegistry = new MsgPackIdRegistry();
            registerSystemDefaults();
            if (StringUtils.hasText(basePackageToScan)) {
                try {
                    ClassPathScanner.scanClasses(basePackageToScan, new ClassPathScanner.ScanListener() {
                        @Override
                        public void onMatch(MetadataReader metadataReader) {
                            try {
                                Entry<Integer, Class<?>> entry = msgPackRegistry
                                        .register(metadataReader.getClassMetadata().getClassName());
                                MsgPackSupport.register(entry.getValue());
                                logger.debug("registered [{}] as [{}] to msgpack registry.", entry.getValue().getName(),
                                        entry.getKey());
                            } catch (ClassNotFoundException | LinkageError e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }, MsgPackId.class, false);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static final short MAX_SYSTEM_REGISTRATION_ID = 100;
    private static final short MIN_SYSTEM_REGISTRATION_ID = 10;

    private int x = MIN_SYSTEM_REGISTRATION_ID;

    protected int nextId() {
        if (x == MAX_SYSTEM_REGISTRATION_ID)
            x = MIN_SYSTEM_REGISTRATION_ID;
        return x++;
    }

    private void registerSystemDefaults() {
        /* 0-100 used for this method register system default. */
        msgPackRegistry.registerWithoutPack(boolean.class, nextId());
        msgPackRegistry.registerWithoutPack(Boolean.class, nextId());
        msgPackRegistry.registerWithoutPack(byte.class, nextId());
        msgPackRegistry.registerWithoutPack(Byte.class, nextId());
        msgPackRegistry.registerWithoutPack(short.class, nextId());
        msgPackRegistry.registerWithoutPack(Short.class, nextId());
        msgPackRegistry.registerWithoutPack(int.class, nextId());
        msgPackRegistry.registerWithoutPack(Integer.class, nextId());
        msgPackRegistry.registerWithoutPack(long.class, nextId());
        msgPackRegistry.registerWithoutPack(Long.class, nextId());
        msgPackRegistry.registerWithoutPack(float.class, nextId());
        msgPackRegistry.registerWithoutPack(Float.class, nextId());
        msgPackRegistry.registerWithoutPack(double.class, nextId());
        msgPackRegistry.registerWithoutPack(Double.class, nextId());
        msgPackRegistry.registerWithoutPack(BigInteger.class, nextId());
        msgPackRegistry.registerWithoutPack(char.class, nextId());
        msgPackRegistry.registerWithoutPack(Character.class, nextId());
        msgPackRegistry.registerWithoutPack(boolean[].class, nextId());
        msgPackRegistry.registerWithoutPack(short[].class, nextId());
        msgPackRegistry.registerWithoutPack(int[].class, nextId());
        msgPackRegistry.registerWithoutPack(long[].class, nextId());
        msgPackRegistry.registerWithoutPack(float[].class, nextId());
        msgPackRegistry.registerWithoutPack(double[].class, nextId());
        msgPackRegistry.registerWithoutPack(String.class, nextId());
        msgPackRegistry.registerWithoutPack(byte[].class, nextId());
        msgPackRegistry.registerWithoutPack(BigDecimal.class, nextId());
        msgPackRegistry.registerWithoutPack(Date.class, nextId());
        /* collection types */
        msgPackRegistry.registerWithoutPack(Set.class, nextId());
        msgPackRegistry.registerWithoutPack(Map.class, nextId());
        msgPackRegistry.registerWithoutPack(List.class, nextId());
        msgPackRegistry.registerWithoutPack(Collection.class, nextId());
    }

    @Override
    public DataObjectReader<InputStream> deserialize(InputStream r) {
        if (!initFlag.get())
            throw new IllegalStateException("call initFactory first.");
        // StreamSerializer dataSerializer = new StreamSerializer(r);
        MsgPackDataSerializer dataSerializer = new MsgPackDataSerializer(r);
        MsgPackObjectSerializer msgPackObjectSerializer = new MsgPackObjectSerializer(r, msgPackRegistry);
        return new CompositeSerializer<>(dataSerializer, msgPackObjectSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream w) {
        if (!initFlag.get())
            throw new IllegalStateException("call initFactory first.");
        // StreamSerializer dataSerializer = new StreamSerializer(w);
        MsgPackDataSerializer dataSerializer = new MsgPackDataSerializer(w);
        MsgPackObjectSerializer msgPackObjectSerializer = new MsgPackObjectSerializer(w, msgPackRegistry);
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

    public void setBasePackageToScan(String basePackageToScan) {
        this.basePackageToScan = basePackageToScan;
    }

    @Override
    public Set<Class<?>> serializableClassesSet() {
        return msgPackRegistry.classesSet();
    }

    @Override
    public byte factoryId() {
        return 0x02;
    }

    @Override
    public String id() {
        return "msgpack";
    }


    @Override
    public SerialIdRegistry getClassRegistry() {
        return msgPackRegistry;
    }

}
