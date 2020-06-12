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
package org.scleropages.openapi.provider.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.common.primitives.Primitives;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.scleropages.core.util.GenericTypes;

import org.scleropages.openapi.annotation.ApiIgnore;
import org.scleropages.openapi.annotation.ApiModel;
import org.scleropages.openapi.annotation.ApiStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#schema 工具类，提供java class metadata->oai schema转换.<br>
 *
 *
 * <pre>
 *     IMPORTANT NOTICE: jdk1.8 @Repeatable(List.class)支持多注解声明 例如jsr303注解都允许重复，但当前实现不支持，需要将约束合并定义到一个groups中，否则无法正常工作
 *     例如:
 *
 *     &#64;NotEmpty(groups = {CreateModel.class, UpdateModel.class, ReadAclModel.class, ReadEntriesBySpecifyResource.class})
 *     public String getId() {
 *         return id;
 *     }
 *     REMINDER: 后期会增加多注解解析的支持. spring 5.x 提供了 {@link org.springframework.core.annotation.MergedAnnotations}来简化注解合并.
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SchemaUtil {

    private static final String[] IGNORE_PROPERTY_NAMES = new String[]{"class"};

    public static final String DEFAULT_SCHEMAS_PATH = "#/components/schemas/";

    private static final Logger logger = LoggerFactory.getLogger(SchemaUtil.class);

    public static final Schema createSchema(MethodParameter methodParameter, ResolveContext resolveContext) {
        Assert.notNull(methodParameter, "methodParameter must not be null.");
        Assert.notNull(resolveContext, "resolveContext must not be null.");
        return createSchema(getParameterConcreteType(methodParameter, null), methodParameter, null, isIgnorePropertyFieldNotFound(methodParameter.getParameterType()), resolveContext, createMutableGraph());
    }


    public static final Schema createSchema(Class javaType, ResolveContext resolveContext) {
        Assert.notNull(javaType, "javaType must not be null.");
        Assert.notNull(resolveContext, "resolveContext must not be null.");
        return createSchema(javaType, null, null, isIgnorePropertyFieldNotFound(javaType), resolveContext, createMutableGraph());
    }

    /**
     * 该方法仅适用于方法参数（返回值）类型为接口
     *
     * @param implementationType 实现类
     * @param methodParameter    方法参数
     * @param resolveContext     解析上下文
     * @return
     */
    public static final Schema createSchema(Class implementationType, MethodParameter methodParameter, ResolveContext resolveContext) {
        Assert.notNull(implementationType, "implementationType must not be null.");
        Assert.notNull(resolveContext, "resolveContext must not be null.");
        return createSchema(implementationType, methodParameter, null, isIgnorePropertyFieldNotFound(implementationType), resolveContext, createMutableGraph());
    }


    private static final Schema createSchema(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, boolean ignorePropertyFieldNotFound, ResolveContext resolveContext, MutableGraph<Class> graph) {
        Assert.notNull(javaType, "javaType is required.");
        if (isBasicType(javaType)) {
            return createBasicSchema(javaType);
        } else if (isBasicArrayType(javaType)) {
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setItems(createBasicSchema(javaType.getComponentType()));
            return arraySchema;
        } else if (javaType.isArray()) {
            ArraySchema arraySchema = new ArraySchema();
            Class elementType = javaType.getComponentType();
            if (null != fieldPropertyDescriptor) {
                elementType = getPropertyElementConcreteType(fieldPropertyDescriptor);
            }
            arraySchema.setItems(createSchema(elementType, methodParameter, fieldPropertyDescriptor, ignorePropertyFieldNotFound, resolveContext, graph));
            return arraySchema;
        } else if (ClassUtils.isAssignable(Collection.class, javaType)) {
            ArraySchema arraySchema = new ArraySchema();
            Class<?> elementType = null;
            if (null != fieldPropertyDescriptor) {
                elementType = getPropertyElementConcreteType(fieldPropertyDescriptor);
            }
            if (null == elementType && null != methodParameter) {
                elementType = getParameterConcreteType(methodParameter, ResolvableType.forMethodParameter(methodParameter).asCollection().resolveGeneric(0));
            }
            if (null == elementType) {
                if (logger.isDebugEnabled())
                    logger.warn("can not resolve generic-type of java.util.Collection from parameter: [{}] or field: [{}] use object schema directly.", methodParameter, fieldPropertyDescriptor);
                ObjectSchema objectSchema = new ObjectSchema();
                objectSchema.setFormat("object");
                return objectSchema;
            } else {
                arraySchema.setItems(createSchema(elementType, methodParameter, fieldPropertyDescriptor, ignorePropertyFieldNotFound, resolveContext, graph));
            }
            return arraySchema;
        } else if (ClassUtils.isAssignable(Map.class, javaType)) {
//            ArraySchema arraySchema = new ArraySchema();
//            ObjectSchema entrySchema=new ObjectSchema();
//            arraySchema.items(entrySchema);
//            arraySchema.setFormat("map");
//            boolean resolved = false;
//            if (null != fieldPropertyDescriptor) {
//                Map.Entry<Class, Class> entriesType = getEntriesType(fieldPropertyDescriptor);
//                if (null != entriesType) {
//                    entrySchema.addProperties("key",createSchema(entriesType.getKey(), methodParameter, fieldPropertyDescriptor, ignorePropertyFieldNotFound, resolveContext, graph));
//                    entrySchema.addProperties("value", createSchema(entriesType.getValue(), methodParameter, fieldPropertyDescriptor, ignorePropertyFieldNotFound, resolveContext, graph));
//                    resolved = true;
//                }
//            }
//            if (!resolved)
//            logger.warn("can not resolve java.util.Map of parameter: [{}] or field: [{}] use object schema directly.", methodParameter, fieldPropertyDescriptor);
//            return arraySchema;
            if (logger.isDebugEnabled())
                logger.warn("can not resolve java.util.Map of parameter: [{}] or field: [{}]. please use EntryList instead.", methodParameter, fieldPropertyDescriptor);
            ObjectSchema objectSchema = new ObjectSchema();
            objectSchema.setFormat("map");
            return objectSchema;
        } else {
            for (SchemaResolver schemaResolver : resolveContext.getSchemaResolvers()) {
                if (schemaResolver.support(javaType, methodParameter, fieldPropertyDescriptor, resolveContext)) {
                    return schemaResolver.resolve(javaType, methodParameter, fieldPropertyDescriptor, resolveContext);
                }
            }
            return computeObjectSchemaIfAbsent(javaType, readRuleInterfaceType(javaType, methodParameter), methodParameter, ignorePropertyFieldNotFound, resolveContext, graph);
        }
    }


    private static Schema computeObjectSchemaIfAbsent(Class javaType, Class ruleInterfaceType, MethodParameter methodParameter, boolean ignorePropertyFieldNotFound, ResolveContext resolveContext, MutableGraph<Class> graph) {
        List<String> requiredProperties = Lists.newArrayList();
        Schema targetSchema = resolveContext.getSwaggerOpenApi().computeSchemaIfAbsent(javaType, ruleInterfaceType, (cls1, cls2) -> {
            ObjectSchema objectSchema = new ObjectSchema();
            PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(javaType);
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                FieldPropertyDescriptor fieldPropertyDescriptor = new FieldPropertyDescriptor(javaType, propertyDescriptor);
                String propertyName = propertyDescriptor.getName();
                if (readable(fieldPropertyDescriptor, methodParameter, ignorePropertyFieldNotFound)) {
                    Class<?> propertyType = getPropertyConcreteType(fieldPropertyDescriptor);
                    if (isCycleDepends(graph, javaType, propertyType, propertyDescriptor)) {
                        logger.warn("detected cycle depends from {}.{} to {}. ignore to process.", javaType.getSimpleName(), propertyName, propertyType.getSimpleName());
                        continue;
                    }
                    Schema propertySchema = createSchema(propertyType, methodParameter, fieldPropertyDescriptor, ignorePropertyFieldNotFound, resolveContext, graph);
                    if (isRequiredProperty(methodParameter, fieldPropertyDescriptor)) {
                        requiredProperties.add(propertyName);
                    }
                    objectSchema.addProperties(propertyName, propertySchema);
                }
            }
            return objectSchema;
        });
        String targetSchemaName = javaType.equals(ruleInterfaceType) ? javaType.getName() : javaType.getName() + "" + ruleInterfaceType.getSimpleName();
        targetSchemaName = StringUtils.replace(targetSchemaName, "$", "");//内部类符号'$'在openapi规范中被禁止
        targetSchema.setName(targetSchemaName);
        targetSchema.setRequired(requiredProperties);
        ObjectSchema schemaRef = new ObjectSchema();
        schemaRef.$ref(DEFAULT_SCHEMAS_PATH + targetSchemaName);
        return schemaRef;
    }


    /**
     * jdk1.8 @Repeatable(List.class)支持多注解声明 例如jsr303注解都允许重复，但当前实现不支持，需要将约束合并定义到一个groups中，否则无法正常工作
     *
     * @param methodParameter
     * @param fieldPropertyDescriptor
     * @return
     */
    private static boolean isRequiredProperty(MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor) {
        Class[] ignoreClasses = getIgnoreClasses(methodParameter);
        Class[] fieldRequired = null;
        NotNull notNull = findFieldAnnotation(fieldPropertyDescriptor, NotNull.class);
        if (null != notNull) {
            if (ArrayUtils.isEmpty(notNull.groups())) {
                return true;
            } else {
                fieldRequired = notNull.groups();
            }
        }
        if (null == fieldRequired) {
            NotEmpty notEmpty = findFieldAnnotation(fieldPropertyDescriptor, NotEmpty.class);
            if (null != notEmpty) {
                if (ArrayUtils.isEmpty(notEmpty.groups())) {
                    return true;
                } else {
                    fieldRequired = notEmpty.groups();
                }
            }
        }
        if (null == fieldRequired) {
            NotBlank notBlank = findFieldAnnotation(fieldPropertyDescriptor, NotBlank.class);
            if (null != notBlank) {
                if (ArrayUtils.isEmpty(notBlank.groups())) {
                    return true;
                } else {
                    fieldRequired = notBlank.groups();
                }
            }
        }
        if (null != ignoreClasses && null != fieldRequired) {
            if (rulesMatch(fieldRequired, ignoreClasses)) {
                return true;
            }
        }
        return false;
    }

    private static Class readRuleInterfaceType(Class javaType, MethodParameter methodParameter) {
        Class ruleInterfaceType = javaType;
        if (null != methodParameter && methodParameter.getParameterIndex() > -1) {//方法参数规则限定从参数注解获取
            ApiIgnore apiIgnore = methodParameter.getParameterAnnotation(ApiIgnore.class);
            if (null != apiIgnore) {
                Class<?>[] ruleInterfaceTypes = apiIgnore.value();
                if (ArrayUtils.isNotEmpty(ruleInterfaceTypes))
                    ruleInterfaceType = ruleInterfaceTypes[0];
            }
        } else if (null != methodParameter && methodParameter.getParameterIndex() == -1) {//方法返回值规则限定从方法注解获取
            ApiIgnore apiIgnore = methodParameter.getMethodAnnotation(ApiIgnore.class);
            if (null != apiIgnore) {
                Class<?>[] ruleInterfaceTypes = apiIgnore.value();
                if (ArrayUtils.isNotEmpty(ruleInterfaceTypes))
                    ruleInterfaceType = ruleInterfaceTypes[0];
            }
        }
        return ruleInterfaceType;
    }


    private static boolean readable(FieldPropertyDescriptor fieldPropertyDescriptor, MethodParameter methodParameter, boolean ignorePropertyFieldNotFound) {
        for (String ignoreProperty : IGNORE_PROPERTY_NAMES) {
            if (Objects.equals(fieldPropertyDescriptor.getPropertyDescriptor().getName(), ignoreProperty))
                return false;
        }
        if (null != findFieldAnnotation(fieldPropertyDescriptor, Transient.class))
            return false;
        if (!ignorePropertyFieldNotFound && null == fieldPropertyDescriptor.getPropertyField())
            return false;
        ApiIgnore fieldIgnore = findFieldAnnotation(fieldPropertyDescriptor, ApiIgnore.class);
        if (null != fieldIgnore && ArrayUtils.isEmpty(fieldIgnore.value()))//field上没有设置@ApiIgnore.values直接忽略.
            return false;

        Class[] ignoreClasses = getIgnoreClasses(methodParameter);

        //匹配参数@ApiIgnore 配置以及 field @ApiIgnore配置
        if (null != fieldIgnore && null != ignoreClasses) {
            if (rulesMatch(fieldIgnore.value(), ignoreClasses))
                return false;
        }

        //兼容 jsr303匹配参数@ApiIgnore 配置以及 field @Null配置
        Null nill = findFieldAnnotation(fieldPropertyDescriptor, Null.class);
        if (null != nill && ArrayUtils.isEmpty(nill.groups()))//field上没有设置groups直接忽略.
            return false;
        if (null != nill && null != ignoreClasses) {
            if (rulesMatch(nill.groups(), ignoreClasses))
                return false;
        }
        return true;
    }


    private static Class[] getIgnoreClasses(MethodParameter methodParameter) {
        if (null != methodParameter) {
            ApiIgnore parameterIgnore =
                    methodParameter.getParameterIndex() > -1 ?
                            methodParameter.getParameterAnnotation(ApiIgnore.class) : //方法参数
                            methodParameter.getMethodAnnotation(ApiIgnore.class);//方法返回值
            if (null != parameterIgnore)
                return parameterIgnore.value();
        }
        return null;
    }

    private static boolean rulesMatch(Class<?>[] matchFrom, Class<?>[] matchTo) {
        for (Class from : matchFrom) {
            for (Class to : matchTo) {
                if (from.equals(to)) {//只要存在交集就匹配
                    return true;
                }
            }
        }
        return false;
    }


    private static <A> A findFieldAnnotation(FieldPropertyDescriptor fieldPropertyDescriptor, Class<? extends Annotation> annotationClazz) {
        Field field = fieldPropertyDescriptor.getPropertyField();
        PropertyDescriptor propertyDescriptor = fieldPropertyDescriptor.getPropertyDescriptor();
        Annotation annotation = null;
        if (field != null) {
            annotation = AnnotationUtils.findAnnotation(field, annotationClazz);
        }
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(propertyDescriptor.getReadMethod(), annotationClazz);
        }
        return (A) annotation;
    }

    /**
     * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#data-types
     *
     * @param javaType
     * @return
     */
    public static final Schema createBasicSchema(Class javaType) {
        javaType = Primitives.wrap(javaType);
        if (javaType.isAssignableFrom(Long.class)) {
            IntegerSchema integerSchema = new IntegerSchema();
            integerSchema.setFormat("int64");
            return integerSchema;
        }
        if (javaType.isAssignableFrom(Integer.class)) {
            return new IntegerSchema();
        }
        if (javaType.isAssignableFrom(Float.class)) {
            NumberSchema numberSchema = new NumberSchema();
            numberSchema.setFormat("float");
            return numberSchema;
        }
        if (javaType.isAssignableFrom(Double.class)) {
            NumberSchema numberSchema = new NumberSchema();
            numberSchema.setFormat("double");
            return numberSchema;
        }
        if (javaType.isAssignableFrom(String.class)) {
            return new StringSchema();
        }
        if (javaType.isAssignableFrom(Boolean.class)) {
            return new BooleanSchema();
        }
        if (javaType.isAssignableFrom(Date.class)) {
            return new DateTimeSchema();
        }
        if (javaType.isAssignableFrom(Byte.class)) {
            return new StringSchema();
        }
        throw new IllegalArgumentException("unsupported primitive type: " + javaType);
    }

    private static MutableGraph createMutableGraph() {
        return GraphBuilder.directed().allowsSelfLoops(false).build();
    }

    private static boolean isCycleDepends(MutableGraph<Class> dependsGraph, Class refFrom, Class refTo, PropertyDescriptor propertyDescriptor) {

        if (isBasicType(refTo))
            return false;
        if (refTo.isArray()) {
            return isCycleDepends(dependsGraph, refFrom, refTo.getComponentType(), propertyDescriptor);
        }
        if (ClassUtils.isAssignable(Collection.class, refTo)) {
            Class<?> resolveGeneric = GenericTypes.getMethodReturnGenericType(refFrom, propertyDescriptor, 0);
            if (null != resolveGeneric)
                return isCycleDepends(dependsGraph, refFrom, resolveGeneric, propertyDescriptor);
        }
        if (ClassUtils.isAssignable(Map.class, refTo)) {
            return false;
        }
        try {
            dependsGraph.putEdge(refFrom, refTo);//重复的属性类型应是允许的
//            if (!dependsGraph.putEdge(refFrom, refTo)) {
//                return true;
//            }
        } catch (UnsupportedOperationException | IllegalArgumentException e) {//allowsSelfLoops=false
            return true;
        }
        if (Graphs.hasCycle(dependsGraph)) {
            dependsGraph.removeEdge(refFrom, refTo);
            return true;
        }
        return false;
    }

    public static Class getParameterConcreteType(MethodParameter methodParameter, Class javaType) {
        Class parameterType = null != javaType ? javaType : methodParameter.getParameterType();
        if (parameterType == null)
            return null;
        if (parameterType.isInterface()) {
            ApiModel apiModel;
            if (methodParameter.getParameterIndex() > -1) {//参数实现类需要从参数注解上获取
                apiModel = methodParameter.getParameterAnnotation(ApiModel.class);
            } else {
                //返回值实现类需要从方法注释上获取
                apiModel = methodParameter.getMethodAnnotation(ApiModel.class);
            }
            Class concreteType = null != apiModel ? apiModel.value() : null;
            if (null != concreteType && ClassUtils.isAssignable(parameterType, concreteType))
                parameterType = concreteType;
        }
        return parameterType;
    }

    public static Class getPropertyConcreteType(FieldPropertyDescriptor fieldPropertyDescriptor) {
        PropertyDescriptor propertyDescriptor = fieldPropertyDescriptor.getPropertyDescriptor();
        Field propertyField = fieldPropertyDescriptor.getPropertyField();
        Class<?> propertyType = null != propertyDescriptor ? propertyDescriptor.getPropertyType() : propertyField.getType();
        if (propertyType.isInterface()) {
            ApiModel apiModel = findFieldAnnotation(fieldPropertyDescriptor, ApiModel.class);
            if (null != apiModel && ClassUtils.isAssignable(propertyType, apiModel.value())) {
                return apiModel.value();
            }
        }
        return propertyDescriptor.getPropertyType();
    }

    private static Class getPropertyElementConcreteType(FieldPropertyDescriptor fieldPropertyDescriptor) {
        Class elementType = getPropertyElementType(fieldPropertyDescriptor);
        if (null == elementType)
            return null;
        ApiModel apiModel = findFieldAnnotation(fieldPropertyDescriptor, ApiModel.class);
        if (null == apiModel)
            return elementType;
        Class value = apiModel.value();
        if (ClassUtils.isAssignable(elementType, value))
            return value;
        return elementType;
    }

    private static Class getPropertyElementType(FieldPropertyDescriptor fieldPropertyDescriptor) {
        PropertyDescriptor propertyDescriptor = fieldPropertyDescriptor.getPropertyDescriptor();
        Field propertyField = fieldPropertyDescriptor.getPropertyField();
        Class<?> propertyType = null != propertyDescriptor ? propertyDescriptor.getPropertyType() : propertyField.getType();
        if (propertyType.isArray()) {
            return propertyType.getComponentType();
        } else if (ClassUtils.isAssignable(Collection.class, propertyType)) {
            Class<?> resolveGeneric = null;
            if (null != propertyDescriptor) {
                resolveGeneric = ResolvableType.forMethodReturnType(propertyDescriptor.getReadMethod()).asCollection().resolveGeneric(0);
            }
            if (null == resolveGeneric && null != propertyField)
                resolveGeneric = ResolvableType.forField(propertyField).asCollection().resolveGeneric(0);
            if (null != resolveGeneric)
                return resolveGeneric;
            return null;
        }
        throw new IllegalArgumentException("not a array or collection type: " + fieldPropertyDescriptor);
    }


    private static Map.Entry<Class, Class> getEntriesType(FieldPropertyDescriptor fieldPropertyDescriptor) {
        PropertyDescriptor propertyDescriptor = fieldPropertyDescriptor.getPropertyDescriptor();
        Field propertyField = fieldPropertyDescriptor.getPropertyField();
        Class<?> propertyType = null != propertyDescriptor ? propertyDescriptor.getPropertyType() : propertyField.getType();
        if (ClassUtils.isAssignable(Map.class, propertyType)) {
            ResolvableType resolvableType = null;
            if (null != propertyDescriptor) {
                resolvableType = ResolvableType.forMethodReturnType(propertyDescriptor.getReadMethod()).asMap();
            }
            if (null == resolvableType && null != propertyField) {
                resolvableType = ResolvableType.forField(propertyField).asMap();
            }
            if (null != resolvableType)
                return createMapEntry(resolvableType);
            else
                return null;
        }
        throw new IllegalArgumentException("not an map type: " + fieldPropertyDescriptor);
    }

    private static Map.Entry createMapEntry(ResolvableType resolvableType) {
        return new Map.Entry<Class, Class>() {
            @Override
            public Class getKey() {
                return resolvableType.resolveGeneric(0);
            }

            @Override
            public Class getValue() {
                return resolvableType.resolveGeneric(1);
            }

            @Override
            public Class setValue(Class value) {
                throw new IllegalStateException("unsupported operation.");
            }
        };
    }


    public static boolean isBasicType(Class javaType) {
        return ClassUtils.isPrimitiveOrWrapper(javaType) || ClassUtils.isAssignable(String.class, javaType) || ClassUtils.isAssignable(Date.class, javaType);
    }

    public static boolean isBasicArrayType(Class javaType) {
        return ClassUtils.isPrimitiveArray(javaType)
                || ClassUtils.isPrimitiveWrapperArray(javaType)
                || (javaType.isArray() && (
                ClassUtils.isAssignable(String.class, javaType.getComponentType())
                        || ClassUtils.isAssignable(Date.class, javaType.getComponentType())));
    }


    private static final boolean isIgnorePropertyFieldNotFound(Class clazz) {
        ApiStrategy apiStrategy = AnnotationUtils.findAnnotation(clazz, ApiStrategy.class);
        if (null != apiStrategy) {
            return apiStrategy.ignorePropertyFieldNotFound();
        }
        return true;
    }


    public static void main(String[] args) throws JsonProcessingException {

        MutableGraph<Object> mutableGraph = GraphBuilder.directed().allowsSelfLoops(false).build();

        System.out.println(mutableGraph.addNode("root"));
        System.out.println(mutableGraph.putEdge("root", "a"));
        System.out.println(mutableGraph.putEdge("a", "b"));
        System.out.println(mutableGraph.putEdge("a", "c"));
        System.out.println(mutableGraph.putEdge("b", "c"));
        System.out.println(mutableGraph.putEdge("c", "d"));
        System.out.println(mutableGraph.putEdge("d", "b"));
        System.out.println(mutableGraph.putEdge("d", "e"));
        System.out.println(mutableGraph.putEdge("b", "e"));
        System.out.println(mutableGraph.putEdge("e", "a"));
        System.out.println(Graphs.hasCycle(mutableGraph));
        System.out.println(mutableGraph.hasEdgeConnecting("a", "c"));//a,c是否存在直接连接
        System.out.println(mutableGraph.adjacentNodes("a"));//返回a所有相邻节点
        System.out.println(mutableGraph.incidentEdges("a"));//返回a所有直接连线
        System.out.println("-------------");
        System.out.println(mutableGraph.predecessors("b"));//返回b直接前置节点
        System.out.println(mutableGraph.successors("b"));//返回b直接后续节点
        System.out.println(mutableGraph.inDegree("d"));//返回d入口连线数
        System.out.println(mutableGraph.outDegree("d"));//返回d出口连线数
        System.out.println("-------------");
        System.out.println(Graphs.reachableNodes(mutableGraph, "b"));//返回b所有可以达到的节点


        MutableValueGraph<String, String> mutableValueGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();

        System.out.println(mutableValueGraph.putEdgeValue("a", "b", "a->b"));
        System.out.println(mutableValueGraph.putEdgeValue("a", "c", "a->c"));
        System.out.println(mutableValueGraph.putEdgeValue("b", "c", "b->c"));
        System.out.println(mutableValueGraph.putEdgeValue("c", "d", "c->d"));
        System.out.println(mutableValueGraph.putEdgeValue("d", "b", "d->b"));
        System.out.println(Graphs.hasCycle(mutableValueGraph.asGraph()));
        System.out.println(mutableValueGraph.hasEdgeConnecting("a", "c"));
        System.out.println(mutableValueGraph);
    }

}
