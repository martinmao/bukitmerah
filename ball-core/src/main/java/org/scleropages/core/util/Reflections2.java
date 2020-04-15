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

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * reflections utility based <a href="https://github.com/EsotericSoftware/reflectasm">reflectasm</a>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Reflections2 {

    private static final String DEFAULT_NESTED_PROPERTY_SEPARATOR = ".";
    private static final String SET_PREFIX = "set";
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final String ARRAY_BOUNDS_LEFT = "[";
    private static final String ARRAY_BOUNDS_RIGHT = "]";
    private static final String MAP_BOUNDS_LEFT = "['";
    private static final String MAP_BOUNDS_RIGHT = "']";

    private static final Map<String, Class<?>> CLASS_CACHE = Maps.newConcurrentMap();
    private static final Map<Class<?>, ConstructorAccess<?>> CONSTRUCTOR_ACCESS_CACHE = Maps.newConcurrentMap();
    private static final Map<Class<?>, FieldAccess> FIELD_ACCESS_CACHE = Maps.newConcurrentMap();
    private static final Map<Class<?>, MethodAccess> METHOD_ACCESS_CACHE = Maps.newConcurrentMap();

    public static Object newInstance(String clazz) {
        return newInstance(getClass(clazz));
    }

    public static Class<?> getClass(String name) {
        return CLASS_CACHE.computeIfAbsent(name, s -> {
            try {
                return ClassUtils.forName(name, Reflections.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz) {
        return (T) CONSTRUCTOR_ACCESS_CACHE.computeIfAbsent(clazz, s -> ConstructorAccess.get(clazz)).newInstance();
    }

    public static void invokeSet(Object target, String expression, Object value) {
        if (-1 == StringUtils.indexOf(expression, DEFAULT_NESTED_PROPERTY_SEPARATOR)) {
            invokeSimpleSet(target, expression, value);
            return;
        }
        String[] expressions = org.springframework.util.StringUtils.split(expression,
                DEFAULT_NESTED_PROPERTY_SEPARATOR);

        String currentProperty = expressions[0];
        String nextExpression = expressions[1];

        Object currentValue = invokeSimpleGet(target, currentProperty);
        if (null == currentValue) {
            currentValue = constructorProperty(target, currentProperty);
        }
        invokeSet(currentValue, nextExpression, value);
    }

    public static Object invokeGet(Object target, String expression) {
        if (-1 == StringUtils.indexOf(expression, DEFAULT_NESTED_PROPERTY_SEPARATOR)) {
            return invokeSimpleGet(target, expression);
        }

        return null;
    }

    // entity.tag1='123' ----> Entity;
    // entities[0].tag1='123' ----> List<Entity>/Entity[];
    // entitiesMapping['123'].tag1='123'---> Map<String,Entity>
    private static Object constructorProperty(Object target, String property) {
        PropertyDef propertyDef = getPropertyDef(target.getClass(), property);
        Class<?> propertyType = propertyDef.getPropertyType();
        Object toReturn = null;
        Object toSet = null;
        if (Map.class.isAssignableFrom(propertyType)) {
            Map<Object, Object> map = constructorMapProperty(propertyDef);
            Class<?> valueType = propertyDef.getResolvableType().asMap().resolveGeneric(1);
            Assert.notNull(valueType, "Can't determined map value type of: " + propertyDef);
            Object mapValue = newInstance(valueType);
            map.put(getMapKey(propertyDef, property), mapValue);
            toSet = map;
            toReturn = mapValue;
        } else if (Collection.class.isAssignableFrom(propertyType)) {
            Collection<Object> coll = constructorCollectionProperty(propertyDef);
            Class<?> itemType = propertyDef.getResolvableType().asCollection().resolveGeneric(0);
            Assert.notNull(itemType, "Can't determined collection item type of: " + propertyDef);
            Object item = newInstance(itemType);
            if (coll instanceof List) {
                List<Object> list = (List<Object>) coll;
                list.add(getArrayIndex(property), item);
            } else
                coll.add(item);
            toSet = coll;
            toReturn = item;
        } else if (propertyType.isArray()) {
            Object array = constructorArrayProperty(propertyDef);
            Class<?> itemType = array.getClass().getComponentType();
            Assert.notNull(itemType, "Can't determined array item type of: " + propertyDef);
            Object item = newInstance(itemType);
            ArrayReflections.set(array, getArrayIndex(property), item, itemType);
            toSet = array;
            toReturn = item;
        } else {
            toSet = toReturn = newInstance(propertyDef.getPropertyType());
        }
        invokeSimpleSet(target, property, toSet);
        return toReturn;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> constructorMapProperty(PropertyDef propertyDef) {

        Class<?> clazz = propertyDef.getPropertyType();

        if (!clazz.isInterface()) {
            try {
                return (Map<Object, Object>) newInstance(clazz);
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Could not instantiate map class [" + clazz.getName() + "]: " + ex.getMessage());
            }
        } else if (SortedMap.class.equals(clazz)) {
            return new TreeMap<>();
        } else {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> constructorCollectionProperty(PropertyDef propertyDef) {
        Class<?> clazz = propertyDef.getPropertyType();
        if (!clazz.isInterface()) {
            try {
                return (Collection<Object>) newInstance(clazz);
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Could not instantiate collection class [" + clazz.getName() + "]: " + ex.getMessage());
            }
        } else if (List.class.equals(clazz)) {
            return new ArrayList<>();
        } else if (SortedSet.class.equals(clazz)) {
            return new TreeSet<>();
        } else {
            return new LinkedHashSet<>();
        }
    }

    private static Object constructorArrayProperty(PropertyDef propertyDef) {
        Class<?> clazz = propertyDef.getPropertyType();
        Class<?> componentType = clazz.getComponentType();
        return Array.newInstance(componentType, 1);
    }

    protected static FieldAccess getFieldAccess(Class<?> clazz) {
        return FIELD_ACCESS_CACHE.computeIfAbsent(clazz,s-> FieldAccess.get(clazz));
    }

    private static MethodAccess getMethodAccess(Class<?> clazz) {
        return METHOD_ACCESS_CACHE.computeIfAbsent(clazz,s-> MethodAccess.get(clazz));
    }

    @SuppressWarnings("unchecked")
    private static void invokeSimpleSet(Object target, String name, Object value) {
        Assert.notNull(target, "target object must not be null.");
        Assert.hasText(name, "property name must not empty text.");
        Class<?> clazz = target.getClass();
        if (target instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) target;
            map.put(getMapKey(getPropertyDef(clazz, name), name), value);
            return;
        }
        if (target instanceof List) {
            List<Object> list = (List<Object>) target;
            list.set(getArrayIndex(name), value);
            return;
        }
        if (target instanceof Collection) {
            Collection<Object> coll = (Collection<Object>) target;
            coll.add(value);
            return;
        }
        if (target.getClass().isArray()) {
            ArrayReflections.set(target, getArrayIndex(name), value, value.getClass());
            return;
        }
        MethodAccess methodAccess = getMethodAccess(clazz);
        PropertyDef propertyDef = getPropertyDef(clazz, name);
        methodAccess.invoke(target, propertyDef.getWidx(methodAccess), value);
    }

    private static Object invokeSimpleGet(Object target, String name) {
        Assert.notNull(target, "target object must not be null.");
        Assert.hasText(name, "proeprty name must not empty text.");
        Class<?> clazz = target.getClass();
        MethodAccess methodAccess = getMethodAccess(clazz);
        PropertyDef propertyDef = getPropertyDef(clazz, name);
        return methodAccess.invoke(target, propertyDef.getRidx(methodAccess));
    }

    private static Object getMapKey(PropertyDef propertyDef, String name) {
        String key = StringUtils.substringBetween(name, MAP_BOUNDS_LEFT, MAP_BOUNDS_RIGHT);
        Class<?> keyType = propertyDef.getResolvableType().asMap().resolveGeneric(0);
        Assert.notNull(keyType, "Can't determined key type of: " + propertyDef);
        try {
            return convertWrapperTypeIfNecessary(key, keyType);
        } catch (Exception e) {
            throw new IllegalArgumentException("failure to get map key from: " + propertyDef, e);
        }
    }

    private static int getArrayIndex(String name) {
        String idx = StringUtils.substringBetween(name, ARRAY_BOUNDS_LEFT, ARRAY_BOUNDS_RIGHT);
        try {
            return Integer.parseInt(idx);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid array index: " + name, e);
        }
    }

    private static Object convertWrapperTypeIfNecessary(String text, Class<?> requiredWrapperType) {
        Assert.isTrue(ClassUtils.isPrimitiveWrapper(requiredWrapperType),
                requiredWrapperType + " not a valid wrapper type.");
        if (requiredWrapperType.isAssignableFrom(String.class))
            return text;
        text = null != text ? text : "";
        if (requiredWrapperType.isAssignableFrom(Integer.class))
            return Integer.parseInt(text);
        if (requiredWrapperType.isAssignableFrom(Long.class))
            return Long.parseLong(text);
        if (requiredWrapperType.isAssignableFrom(Double.class))
            return Double.parseDouble(text);
        if (requiredWrapperType.isAssignableFrom(Short.class))
            return Short.parseShort(text);
        if (requiredWrapperType.isAssignableFrom(Float.class))
            return Float.parseFloat(text);
        if (requiredWrapperType.isAssignableFrom(Byte.class))
            return Byte.parseByte(text);
        if (requiredWrapperType.isAssignableFrom(Character.class) && text.length() > 0)
            return text.charAt(0);
        else
            throw new IllegalArgumentException("un supported type convert: " + requiredWrapperType);
    }

    private static PropertyDef getPropertyDef(Class<?> clazz, String property) {
        String proDefKey = propertyDefCacheKey(clazz, property);
        PropertyDef propertyDef = PROPERTY_DEF_CACHE.get(proDefKey);
        if (null == propertyDef) {
            propertyDef = new PropertyDef(clazz, property);
            PROPERTY_DEF_CACHE.put(proDefKey, propertyDef);
        }
        return propertyDef;
    }

    private static final Map<String, PropertyDef> PROPERTY_DEF_CACHE = Maps.newConcurrentMap();

    private static String propertyDefCacheKey(Class<?> clazz, String property) {
        return clazz.getName() + DEFAULT_NESTED_PROPERTY_SEPARATOR + property;
    }

    public static class PropertyDef {

        private final Class<?> clazz;
        private final String propertyName;
        private final Field field;
        private final String readMethodName;
        private final String writeMethodName;

        private volatile Method readMethod;
        private volatile Method writeMethod;

        private volatile ResolvableType resolvableType;

        private volatile int ridx = -1; // read method index of MethodAccess
        private volatile int widx = -1; // write method index of MethodAccess
        private volatile int fidx = -1; // field index of FieldAccess

        public ResolvableType getResolvableType() {
            if (null == resolvableType)
                resolvableType = ResolvableType.forField(field);
            return resolvableType;
        }

        public int getRidx(MethodAccess methodAccess) {
            if (this.ridx == -1) {
                ridx = methodAccess.getIndex(readMethodName);
            }
            return this.ridx;
        }

        public int getWidx(MethodAccess methodAccess) {
            if (this.widx == -1) {
                widx = methodAccess.getIndex(writeMethodName, getPropertyType());
            }
            return this.widx;
        }

        public int getFidx(FieldAccess fieldAccess) {
            if (this.fidx == -1) {
                fidx = fieldAccess.getIndex(field);
            }
            return fidx;
        }

        public Method getReadMethod() {
            if (null == readMethod) {
                readMethod = getDeclaredMethod(clazz, readMethodName);
            }
            return readMethod;
        }

        public Method getWriteMethod() {
            if (null == writeMethod) {
                writeMethod = getDeclaredMethod(clazz, writeMethodName, field.getType());
            }
            return writeMethod;
        }

        public PropertyDef(Class<?> clazz, String propertyName) {
            this(clazz, propertyName, false, false);
        }

        public PropertyDef(Class<?> clazz, String propertyName, boolean makeAccess, boolean checkrw) {
            Assert.notNull(clazz, "clazz must not be null.");
            Assert.hasText(propertyName, "propertyName must not be empty text.");
            this.clazz = clazz;
            this.propertyName = resolvePropertyName(propertyName);
            this.field = getDeclaredField(clazz, this.propertyName);
            this.readMethodName = readMethodName(field);
            this.writeMethodName = writeMethodName(field);
            if (makeAccess)
                makeAccessible(this.field);
            if (checkrw) {
                getReadMethod();
                getWriteMethod();
            }
        }

        private String resolvePropertyName(String propertyName) {
            return StringUtils.substringBefore(
                    StringUtils.substringBefore(propertyName, DEFAULT_NESTED_PROPERTY_SEPARATOR), ARRAY_BOUNDS_LEFT);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Class<?> getPropertyType() {
            return field.getType();
        }

        protected void makeAccessible(final Field field) {
            if (!Modifier.isPublic(field.getModifiers())
                    || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                field.setAccessible(true);
            }
        }

        protected Field getDeclaredField(Class<?> clazz, final String propetyName) {
            final Class<?> current = clazz;
            for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    return clazz.getDeclaredField(propetyName);
                } catch (NoSuchFieldException e) {
                }
            }
            throw new IllegalArgumentException(
                    "Can't find field[" + propetyName + "] from class: " + current.getName());
        }

        protected Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
            final Class<?> current = clazz;
            for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
                try {
                    return clazz.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException e) {
                }
            }
            throw new IllegalArgumentException("Can't find method[" + methodName + "(" + Arrays.toString(parameterTypes)
                    + ")] from class: " + current.getName());
        }

        private String writeMethodName(Field field) {
            return SET_PREFIX + org.springframework.util.StringUtils.capitalize(field.getName());
        }

        private String readMethodName(Field field) {
            return (field.getType() == boolean.class || field.getType() == Boolean.class ? IS_PREFIX : GET_PREFIX)
                    + org.springframework.util.StringUtils.capitalize(field.getName());

        }

        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        public String toString() {
            return getClazz().getName() + "." + getResolvableType() + " " + getPropertyName();
        }
    }

    public static class ArrayReflections {
        public static Object set(final Object array, final int index, final Object element, final Class<?> clazz) {
            if (array == null) {
                if (index != 0) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Length: 0");
                }
                final Object joinedArray = Array.newInstance(clazz, 1);
                Array.set(joinedArray, 0, element);
                return joinedArray;
            }
            final int length = Array.getLength(array);
            if (index > length || index < 0) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
            }
            final Object result = Array.newInstance(clazz, length + 1);
            System.arraycopy(array, 0, result, 0, index);
            Array.set(result, index, element);
            if (index < length) {
                System.arraycopy(array, index, result, index + 1, length - index);
            }
            return result;
        }
    }
}
