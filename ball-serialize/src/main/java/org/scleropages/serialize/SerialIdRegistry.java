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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Used for serialize implementations. register serialId and class to this
 * registry.<br>
 * <b>NOTE: Thread none safety</b><br>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SerialIdRegistry<ID extends Serializable> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<ID, Class<?>> serialIds = Maps.newHashMap();

    private final Map<Class<?>, ID> serialIds_0 = Maps.newHashMap();

    private volatile Class idType;

    protected Class getIdType() {
        if (idType != null)
            return idType;
        idType = ResolvableType.forClass(SerialIdRegistry.class, getClass()).getGeneric(0).resolve();
        return idType;
    }

    protected abstract Class<? extends Annotation> annotation();

    public Set<Entry<ID, Class<?>>> entrySet() {
        return serialIds.entrySet();
    }

    public Set<Class<?>> classesSet() {
        return serialIds_0.keySet();
    }

    protected abstract ID asId(Object annotationValue);

    public Entry<ID, Class<?>> register(String className) throws ClassNotFoundException, LinkageError {
        Assert.hasText(className, "registered class must not be null.");
        final Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
        final Annotation annotation = clazz.getAnnotation(annotation());
        Assert.notNull(annotation, "registered class missing annotation: " + annotation);
        Object annoValue = AnnotationUtils.getValue(annotation);
        Assert.notNull(annoValue, "annotation value not allowed null.");
        final ID key = asId(annoValue);
        register(clazz, key);
        return new Entry<ID, Class<?>>() {
            @Override
            public ID getKey() {
                return key;
            }

            @Override
            public Class<?> getValue() {
                return clazz;
            }

            @Override
            public Class<?> setValue(Class<?> value) {
                throw new IllegalStateException("not supported.");
            }
        };
    }

    public void register(Class<?> clazz, ID id) {
        if (serialIds.containsKey(id))
            throw new IllegalStateException(id + " already registered from class: " + clazz.getName());
        Class idType = getIdType();
        if (ClassUtils.isAssignable(Number.class, idType) && id instanceof Number) {
            try {
                id = (ID) NumberUtils.convertNumberToTargetClass((Number) id, idType);
            } catch (Exception e) {
                logger.warn("failure to convert registered id {} to target {}", id.getClass(), idType);
            }
        }
        serialIds.put(id, clazz);
        serialIds_0.put(clazz, id);
    }

    public Class<?> get(ID key) {
        return serialIds.get(key);
    }

    public ID get(Class<?> clazz) {
        return serialIds_0.get(clazz);
    }
}
