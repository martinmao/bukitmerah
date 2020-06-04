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
package org.scleropages.core.util;

import com.google.common.collect.Maps;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class GenericTypes {


    private static final Map<String, Class> cachedClassGenericTypes = Maps.newConcurrentMap();


    public static final Class getClassGenericType(Class clazz, Class interfaceClass, int... index) {
        return getClassGenericType(true, clazz, interfaceClass, index);
    }

    public static final Class getClassGenericType(boolean cacheables, Class clazz, Class interfaceClass, int... index) {
        if (!cacheables) {
            if (null == interfaceClass) {
                return ResolvableType.forClass(clazz).resolveGeneric(index);
            } else {
                return ResolvableType.forClass(interfaceClass, clazz).resolveGeneric(index);
            }
        }
        String computeKey = computeKey(clazz, index);
        return cachedClassGenericTypes.computeIfAbsent(computeKey, key -> {
            if (null == interfaceClass) {
                return ResolvableType.forClass(clazz).resolveGeneric(index);
            } else {
                return ResolvableType.forClass(interfaceClass, clazz).resolveGeneric(index);
            }
        });
    }

    public static final Class getMethodReturnGenericType(Class source, PropertyDescriptor propertyDescriptor, int... index) {
        Class<?> resolveGeneric = ResolvableType.forMethodReturnType(propertyDescriptor.getReadMethod()).resolveGeneric(index);
        if (null == resolveGeneric) {
            Field field = ReflectionUtils.findField(source, propertyDescriptor.getName());
            if (null != field)
                resolveGeneric = ResolvableType.forField(field).resolveGeneric(index);
        }
        return resolveGeneric;
    }

    private static final String computeKey(Class clazz, int... indexes) {
        StringBuilder sb = new StringBuilder(clazz.getName()).append('_');
        for (int index :
                indexes) {
            sb.append(index).append('.');
        }
        return sb.toString();
    }

}
