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
package org.scleropages.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.collect.Sets;
import org.scleropages.core.ComponentId;
import org.scleropages.core.ObjectCustomizer;
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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class PooledKryoSerializerFactory implements SerializerFactory<InputStream, OutputStream>, ComponentId {


    public interface KryoCustomizer extends ObjectCustomizer<Kryo> {

    }

    private Set<KryoCustomizer> kryoCustomizers = Sets.newHashSet();

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private KryoPool kryoPool;

    private String basePackageToScan;

    private final KryoIdRegistry kryoRegistry = new KryoIdRegistry();

    private boolean registrationRequired;

    private boolean references;

    private AtomicBoolean initFlag = new AtomicBoolean(false);


    public PooledKryoSerializerFactory() {
    }

    public synchronized void initFactory() {
        if (initFlag.compareAndSet(false, true)) {
            registerSystemDefaults();
            if (StringUtils.hasText(basePackageToScan))
                try {
                    ClassPathScanner.scanClasses(basePackageToScan, new ClassPathScanner.ScanListener() {
                        @Override
                        public void onMatch(MetadataReader metadataReader) {
                            try {
                                Entry<Integer, Class<?>> entry = kryoRegistry.register(metadataReader.getClassMetadata().getClassName());
                                logger.debug("registered [{}] as [{}] to kryo registry.", entry.getValue().getName(), entry.getKey());
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }, KryoId.class, false);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            KryoFactory factory = new KryoFactory() {
                public Kryo create() {
                    Kryo kryo = new Kryo();
                    for (Entry<Integer, Class<?>> entry : kryoRegistry.entrySet()) {
                        kryo.register(entry.getValue(), entry.getKey());
                    }
                    afterKryoCreation(kryo);
                    return kryo;
                }

            };
            this.kryoPool = new KryoPool.Builder(factory).build();
        }
    }

    protected void afterKryoCreation(final Kryo kryo) {
        kryo.setRegistrationRequired(registrationRequired);
        kryo.setReferences(references);

        if (null != kryoCustomizers) {
            for (KryoCustomizer customizer :
                    kryoCustomizers) {
                customizer.customize(kryo);
            }
        }
    }

    @Override
    public DataObjectReader<InputStream> deserialize(InputStream r) {
        if (!initFlag.get())
            throw new IllegalStateException("call initFactory first.");
        KryoDataSerializer dataSerializer = new KryoDataSerializer(r);
        KryoObjectSerializer objectSerializer = new KryoObjectSerializer(r, kryoPool);
        return new CompositeSerializer<>(dataSerializer, objectSerializer);
    }

    @Override
    public DataObjectWriter<OutputStream> serialize(OutputStream w) {
        if (!initFlag.get())
            throw new IllegalStateException("call initFactory first.");
        KryoDataSerializer dataSerializer = new KryoDataSerializer(w);
        KryoObjectSerializer objectSerializer = new KryoObjectSerializer(w, kryoPool);
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

    /**
     * if use {@link KryoId} to identity an object. the scanner will auto
     * register this object to kryo class registry.<br>
     *
     * <B>NOTE: {@link KryoId} must >100, because 0~100 already used by
     * system.</B>
     *
     * @param basePackegeToScan
     */
    public void setBasePackageToScan(String basePackageToScan) {
        this.basePackageToScan = basePackageToScan;
    }

    private static final int MAX_SYSTEM_REGISTRATION_ID = 100;
    private static final int MIN_SYSTEM_REGISTRATION_ID = 20;

    private int x = MIN_SYSTEM_REGISTRATION_ID;

    protected int nextId() {
        if (x == MAX_SYSTEM_REGISTRATION_ID)
            x = MIN_SYSTEM_REGISTRATION_ID;
        return x++;
    }

    private void registerSystemDefaults() {
        /* 0-19 already used in kryo primitive types by default. */
        /* 20-100 used for this method register system default. */
        kryoRegistry.register(BigInteger.class, nextId());
        kryoRegistry.register(BigDecimal.class, nextId());
        kryoRegistry.register(Date.class, nextId());
        // kryoRegistry.register(Enum.class, nextId()); don't opened this may cause not work.
        kryoRegistry.register(TimeZone.class, nextId());
        kryoRegistry.register(GregorianCalendar.class, nextId());
        kryoRegistry.register(Locale.class, nextId());
        kryoRegistry.register(URL.class, nextId());
        kryoRegistry.register(Charset.class, nextId());
        kryoRegistry.register(Currency.class, nextId());
        kryoRegistry.register(StringBuffer.class, nextId());
        kryoRegistry.register(StringBuilder.class, nextId());
        kryoRegistry.register(EnumSet.class, nextId());
        kryoRegistry.register(Collections.EMPTY_LIST.getClass(), nextId());
        kryoRegistry.register(Collections.EMPTY_MAP.getClass(), nextId());
        kryoRegistry.register(Collections.EMPTY_SET.getClass(), nextId());
        kryoRegistry.register(Collections.singletonList(null).getClass(), nextId());
        kryoRegistry.register(Collections.singletonMap(null, null).getClass(), nextId());
        kryoRegistry.register(Collections.singleton(null).getClass(), nextId());
        kryoRegistry.register(TreeSet.class, nextId());
        kryoRegistry.register(Collection.class, nextId());
        kryoRegistry.register(TreeMap.class, nextId());
        kryoRegistry.register(HashMap.class, nextId());
        kryoRegistry.register(ArrayList.class, nextId());
        kryoRegistry.register(LinkedList.class, nextId());
        kryoRegistry.register(HashSet.class, nextId());
        kryoRegistry.register(CopyOnWriteArrayList.class, nextId());
        kryoRegistry.register(CopyOnWriteArraySet.class, nextId());
        kryoRegistry.register(ConcurrentHashMap.class, nextId());
        kryoRegistry.register(LinkedHashMap.class, nextId());
        kryoRegistry.register(LinkedHashSet.class, nextId());
    }

    /**
     * If true, an exception is thrown when an unregistered class is
     * encountered. Default is false.
     * <p>
     * If false, when an unregistered class is encountered, its fully qualified
     * class name will be serialized and the
     * {@link #addDefaultSerializer(Class, Class) default serializer} for the
     * class used to serialize the object. Subsequent appearances of the class
     * within the same object graph are serialized as an int id.
     * <p>
     * Registered classes are serialized as an int id, avoiding the overhead of
     * serializing the class name, but have the drawback of needing to know the
     * classes to be serialized up front.
     */
    public void setRegistrationRequired(boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
    }

    public void setReferences(boolean references) {
        this.references = references;
    }

    @Override
    public Set<Class<?>> serializableClassesSet() {
        return kryoRegistry.classesSet();
    }

    @Override
    public byte factoryId() {
        return 0x01;
    }

    @Override
    public String id() {
        return "kryo";
    }

    public void addKryoCustomizers(KryoCustomizer kryoCustomizer) {
        this.kryoCustomizers.add(kryoCustomizer);
    }

    @Override
    public SerialIdRegistry getClassRegistry() {
        return kryoRegistry;
    }
}
