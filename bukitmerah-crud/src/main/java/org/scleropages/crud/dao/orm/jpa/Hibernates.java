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
package org.scleropages.crud.dao.orm.jpa;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Hibernates {

    private static Map<Class, List<Field>> ENTITY_REFERENCED_FIELDS = Maps.newConcurrentMap();

//    private static Map<Class, MethodAccess> ENTITY_REFERENCED_FIELDS_ACCESSOR = Maps.newConcurrentMap();

    private static Map<Field, SetAndGet> REFERENCED_FIELDS_SET_AND_GET = Maps.newHashMap();

    private static class SetAndGet {
        private final Method setMethod;
        private final Method getMethod;

        public SetAndGet(Method setMethod, Method getMethod) {
            this.setMethod = setMethod;
            this.getMethod = getMethod;
        }

        public Object invokeGet(Object obj) {
            try {
                return getMethod.invoke(obj);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public Object invokeSet(Object obj, Object setTo) {
            try {
                return setMethod.invoke(obj, setTo);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }


    /**
     * <pre>
     * 用于hibernate entity序列化，处于性能及安全性考虑应禁止所有延迟加载特性。
     * 未初始化({@link Hibernate#isInitialized(Object)}==false)的属性直接置空(null)，避免延迟加载
     * 已初始化的属性根据类型做如下处理：
     * 单引用(HibernateProxy)进一步检查
     * map，转换为java map，并对value进一步检查
     * collection，转换为java collection，并对value进一步检查
     * 以上过程持续递归进行，一旦满足未初始化条件直接置空
     * ！！！NOTE：以上检查是建立在hibernate全局 mappedClasses {@link #mappedEntityClasses()} 基础上进行
     *            即如果非引用关系而设置的属性不会被察觉.
     * </pre>
     *
     * @param entity
     * @return
     */
    public static <T> T forSerialize(T entity) {
        if (entity == null)
            return null;
        Class entityType = HibernateProxyHelper.getClassWithoutInitializingProxy(entity);
        if (!JpaContexts.isEntityType(entityType))//不是entity type 直接返回
            return entity;
        List<Field> mappedFields = getMappedFields(entityType);
        if (mappedFields.size() == 0)//没有找到关系引用字段，直接返回
            return entity;
//        MethodAccess methodAccess = ENTITY_REFERENCED_FIELDS_ACCESSOR.computeIfAbsent(entityType, MethodAccess::get);
        //对关系引用字段一一检查
        mappedFields.forEach(field -> {
            String fieldName = field.getName();
//            String methodSuffix = StringUtils.capitalize(fieldName);
//            int getIndex = methodAccess.getIndex("get" + methodSuffix);
//            int setIndex = methodAccess.getIndex("set" + methodSuffix, field.getType());

            SetAndGet setAndGet = REFERENCED_FIELDS_SET_AND_GET.get(field);
            Class refEntityType = field.getType();//原始java类型
            if (!Hibernate.isPropertyInitialized(entity, fieldName)) {//未初始化属性直接置空
                setAndGet.invokeSet(entity, null);
//                methodAccess.invoke(entity, setIndex, null);//reflectasm 不支持设置null值
                return;
            }
            Object refEntity = setAndGet.invokeGet(entity);

//            Object refEntity = methodAccess.invoke(entity, getIndex);
            if (null == refEntity)
                return;
            if (!Hibernate.isInitialized(refEntity)) {
                setAndGet.invokeSet(entity, null);
//                methodAccess.invoke(entity, setIndex, null);
                return;
            }
            //已初始化属性，join-fetch，lazy load，hibernate.initialize()...进一步判断
            if (refEntity instanceof PersistentCollection) {//集合类型，需逐一转换为java集合类型
                if (ClassUtils.isAssignable(Map.class, refEntityType)) {
                    //map类型，迭代键值对，并对值进一步检查
                    Map javaMap = Maps.newLinkedHashMap();
                    ((Map) refEntity).forEach((key, val) -> javaMap.put(key, forSerialize(val)));
                    setAndGet.invokeSet(entity, javaMap);
//                    methodAccess.invoke(entity, setIndex, javaMap);
                    return;
                }
                if (ClassUtils.isAssignable(Collection.class, refEntityType)) {
                    //collection类型，迭代并对元素进一步检查
                    Collection javaCollection;
                    if (ClassUtils.isAssignable(Set.class, refEntityType)) {
                        javaCollection = Sets.newLinkedHashSet();
                    } else if (ClassUtils.isAssignable(List.class, refEntityType)) {
                        javaCollection = Lists.newArrayList();
                    } else
                        throw new IllegalArgumentException("unknown collection type: " + refEntityType);
                    for (Object val : ((Collection) refEntity))
                        javaCollection.add(forSerialize(val));
                    setAndGet.invokeSet(entity, javaCollection);

//                    methodAccess.invoke(entity, setIndex, javaCollection);
                    return;
                }
                throw new IllegalArgumentException("unknown PersistentCollection for java type: " + refEntityType);
            } else if (refEntity instanceof HibernateProxy) {
                setAndGet.invokeSet(entity, forSerialize(refEntity));
//                methodAccess.invoke(entity, setIndex, forSerialize(refEntity));
                return;
            }
        });
        return entity;
    }



    /**
     * 获取所有关系引用字段
     *
     * @param entityType
     * @return
     */
    public static List<Field> getMappedFields(Class entityType) {
        List<Field> mappedFields = ENTITY_REFERENCED_FIELDS.computeIfAbsent(entityType, clazz -> {
            Field[] declaredFields = clazz.getDeclaredFields();
            List<Field> refs = Lists.newArrayList();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                if (JpaContexts.isEntityType(field.getType())) {
                    refs.add(field);
                } else if (ClassUtils.isAssignable(Collection.class, field.getType()) && JpaContexts.isEntityType(ResolvableType.forField(field).getGeneric(0).resolve())) {
                    refs.add(field);
                } else if (ClassUtils.isAssignable(Map.class, field.getType()) && JpaContexts.isEntityType(ResolvableType.forField(field).getGeneric(1).resolve())) {
                    refs.add(field);
                }
            }
            refs.forEach(field -> {
                String fieldName = field.getName();
                String methodSuffix = StringUtils.capitalize(fieldName);
                try {
                    Method get = clazz.getMethod("get" + methodSuffix);
                    Method set = clazz.getMethod("set" + methodSuffix, field.getType());
                    get.setAccessible(true);
                    set.setAccessible(true);
                    REFERENCED_FIELDS_SET_AND_GET.put(field, new SetAndGet(set, get));

                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            });
            return refs;
        });
        if (mappedFields.size() == 0) {//没有找到关系引用类型则移除
            ENTITY_REFERENCED_FIELDS.remove(entityType);
        }
        return mappedFields;
    }
}
