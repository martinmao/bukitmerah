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
package org.scleropages.crud.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.scleropages.core.util.GenericTypes;
import org.scleropages.core.util.Reflections2;
import org.scleropages.crud.FrameworkContext;
import org.scleropages.crud.orm.SearchFilter;
import org.scleropages.crud.orm.jpa.JpaContexts;
import org.scleropages.crud.orm.jpa.JpaContexts.ManagedTypeModel;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;

/**
 * Support for jOOQ.
 * <pre>
 * This interface can complement for {@link org.scleropages.crud.orm.jpa.GenericRepository}. all method defined start with 'dsl'.
 * many of method arguments is a subclasses of Reactive({@link org.reactivestreams.Publisher}) implementations(eg:{@link Select} subclasses). but that is not compatibility with spring data-jpa.
 * it will throw "org.springframework.dao.InvalidDataAccessApiUsageException: Reactive Repositories are not supported by JPA."
 * so wrapped these arguments as a {@link Supplier}.
 * The declaration of generic type of:
 * 'T' is Jooq {@link Table} implementation.
 * 'R' is Jooq {@link Record} implementation.
 * 'E' is JPA entity type (annotated with {@link javax.persistence.Entity}) .
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@NoRepositoryBean
public interface JooqRepository<T extends Table, R extends Record, E> {


    /**
     * map a jooq record to given entity.
     *
     * <pre>
     *     支持的关系：
     *     BASIC属性直接设置到目标实体
     *     EMBEDDED属性会创建目标实体的关联实体对象并设置到目标属性
     *     MANY_TO_ONE属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性（已经存在则直接设置）
     *     ONE_TO_ONE属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性（已经存在则直接设置）
     *     部分支持的规则：
     *     ONE_TO_MANY 无法将结果集中的记录进行合并绑定到一的一方，只能将目标实体（多的一方）进行MANY_TO_ONE设置.即目标实体必须为多的一方（且作为关系维护方
     *     不支持的关系：
     *     MANY_TO_MANY
     *     ELEMENT_COLLECTION
     * </pre>
     *
     * @param sourceRecord
     * @param targetEntity
     */
    default void dslRecordInto(R sourceRecord, E targetEntity) {

        Assert.notNull(sourceRecord, "sourceRecord must not be null.");
        Assert.notNull(targetEntity, "targetEntity must not be null.");
        //sourceRecord.into(targetEntity); Only the javax.persistence.Column annotation is used and understood for jOOQ.

        Class<?> targetEntityClass = targetEntity.getClass();
        Assert.isTrue(JpaContexts.isEntity(targetEntity), "not a entity instance: " + targetEntity);
        ManagedTypeModel<?> targetEntityModel = JpaContexts.getManagedTypeModel(targetEntityClass);
        Map<String, EntityType> tableEntityTypes = JpaContexts.databaseTableEntityTypes();

        Stream.of(sourceRecord.fields()).forEach(field -> {
            Object fieldValue = field.getValue(sourceRecord);
            if (null == fieldValue)//null value always not mapped.
                return;
            String[] qualifiedName = field.getQualifiedName().getName();
            if (qualifiedName.length < 2)
                throw new IllegalArgumentException("field qualified name not contains table name: " + field.getQualifiedName());
            String tableName = qualifiedName[0];
            String fieldName = qualifiedName[1];
            //先从目标实体中查看column对应的属性，其中 BASIC，EMBEDDED属性直接设置
            Attribute<?, Object> fieldAttribute = targetEntityModel.attributeByDatabaseColumn(fieldName);
            if (null != fieldAttribute) {
                PersistentAttributeType persistentAttributeType = fieldAttribute.getPersistentAttributeType();
                if (Objects.equals(persistentAttributeType, PersistentAttributeType.BASIC)) {
                    Reflections2.invokeSet(targetEntity, fieldAttribute.getName(), dslGetEntityBasicAttributeValue(field, fieldValue));
                } else if (Objects.equals(persistentAttributeType, PersistentAttributeType.EMBEDDED)) {
                    dslMapAssociatedAttribute(targetEntity, field, fieldName, fieldValue, fieldAttribute);
                }
            } else {//如果column name对应的属性在目标实体中无法找到，则说明该column来源于其关联实体，从table进行实体发现并关联
                Attribute<?, Object> associatedAttribute = targetEntityModel.attributeByDatabaseTable(tableName);
                Assert.notNull(associatedAttribute, "can not found attribute associated table: " + tableName + " from: " + targetEntityClass);
                Assert.isTrue(!associatedAttribute.isCollection(), "not support collection attribute from: " + associatedAttribute.getName() + " with field: " + field);
                dslMapAssociatedAttribute(targetEntity, field, fieldName, fieldValue, associatedAttribute);
            }
        });
    }

    /**
     * overrides this method how to convert a record field value as entity basic property. by default nothing to do return directly.
     *
     * @param value
     * @return
     */
    default Object dslGetEntityBasicAttributeValue(Field field, Object jooqFieldValue) {
        return jooqFieldValue;
    }


    /**
     * overrides this method how to map a record value to entity associated property.
     * 默认根据field类型关联的java type 从jpa 上下文中查找对应 entity metadata 并进行匹配设置属性.
     * 适用于多对一，一对一，EMBEDDED 类型映射
     *
     * @param targetEntity
     * @param field
     * @param fieldName
     * @param fieldValue
     * @param fieldAttribute
     */
    default void dslMapAssociatedAttribute(E targetEntity, Field field, String fieldName, Object fieldValue, Attribute<?, Object> fieldAttribute) {
        if (null == fieldValue)
            return;
        ManagedTypeModel<Object> associatedTypeMode = JpaContexts.getManagedTypeModel(fieldAttribute.getJavaType());
        Attribute<Object, Object> associatedFieldAttribute = associatedTypeMode.attributeByDatabaseColumn(fieldName);
        if (null != associatedFieldAttribute)
            Reflections2.invokeSet(targetEntity, fieldAttribute.getName() + "." + associatedFieldAttribute.getName(), dslGetEntityBasicAttributeValue(field, fieldValue));
    }


    /**
     * lookup a available {@link org.jooq.DSLContext} from {@link FrameworkContext}.
     *
     * @return
     */
    default DSLContext dslContext() {
        return FrameworkContext.getBean(DSLContext.class);
    }


    /**
     * Apply spring data {@link Pageable} to given select.
     *
     * @param select
     * @param pageable
     * @return
     */
    default void dslPageable(Supplier<SelectQuery> select, Pageable pageable) {
        Assert.notNull(select, "select mut not be null.");
        Assert.notNull(pageable, "pageable must not be null.");
        SelectQuery query = select.get();
        if (null != pageable.getSort()) {
            List<OrderField> orderFields = Lists.newArrayList();
            pageable.getSort().forEach(order -> {
                Field orderField = dslNameToField(order.getProperty());
                orderFields.add(order.getDirection().isAscending() ? orderField.asc() : orderField.desc());
            });
            query.addOrderBy((orderFields));
        }
        if (pageable.isPaged()) {
            query.addOffset(pageable.getOffset());
            query.addLimit(pageable.getPageSize());
        }
    }


    /**
     * Create spring data {@link Page} by given content(any query result).
     *
     * @param content
     * @param pageable
     * @param select
     * @param useCountWrapped
     * @param <T>
     * @return
     */
    default <E> Page<E> dslPage(List<E> content, Pageable pageable, Supplier<SelectQuery> select, boolean useCountWrapped) {
        Assert.notNull(select, "select must not be null.");
        return PageableExecutionUtils.getPage(content, pageable, () -> dslFetchCount(select, useCountWrapped));
    }


    /**
     * Create spring data {@link Page} by given select
     *
     * @param select
     * @param pageable
     * @param useCountWrapped
     * @return
     */
    default Page<? extends Record> dslPage(Supplier<SelectQuery> select, Pageable pageable, boolean useCountWrapped) {
        dslPageable(select, pageable);
        return dslPage(select.get().fetch(), pageable, select, useCountWrapped);
    }


    default Page<? extends Record> dslPage(Supplier<SelectQuery> select, Pageable pageable, Map<String, SearchFilter> searchFilter, boolean useCountWrapped) {
        Assert.notEmpty(searchFilter, "searchFilter must not be empty.");
        Class entityClass = GenericTypes.getClassGenericType(getClass(), JooqRepository.class, 2);
        Assert.notNull(entityClass, "generic type(E for entityClass) not declared: " + getClass());
        ManagedTypeModel<?> entityMetaModel = JpaContexts.getManagedTypeModel(entityClass);
        Condition condition = dslConditionTrue();

        searchFilter.forEach((property, filter) -> {
            Assert.isTrue(entityMetaModel.isSingularAttribute(property), "not a singular persist(basic,many-to-one,one-to-one) property: " + property + " from: " + entityClass);
            SingularAttribute<?, Object> attribute = entityMetaModel.singularAttribute(property);
            Member javaMember = attribute.getJavaMember();
            Assert.isAssignable(AnnotatedElement.class, javaMember.getClass(), "property's member not an AnnotatedElement: " + property + " from: " + entityClass);
            AnnotationUtils.findAnnotation((AnnotatedElement) javaMember, Column.class);
        });
        SelectQuery query = select.get();
        query.addConditions(condition);
        return dslPage(select, pageable, useCountWrapped);
    }


    /**
     * query number of count results by given select.
     *
     * @param select
     * @param useCountWrapped use select count(*) from (given select).
     * @return
     */
    default Long dslFetchCount(Supplier<SelectQuery> select, boolean useCountWrapped) {
        SelectQuery query = select.get();
        if (useCountWrapped)
            return Long.valueOf(dslContext().select(DSL.count()).from(query.asTable()).fetchOne().value1());
        String sql = query.getSQL().toLowerCase();
        String[] splitByFirstFrom = org.springframework.util.StringUtils.split(sql, "from");
        Assert.notNull(splitByFirstFrom, "invalid sql. can not split by 'from' fragment. ");
        sql = " from " + splitByFirstFrom[1];
        sql = StringUtils.substringBefore(sql, "order by");
        String countFragment = splitByFirstFrom[0].contains("distinct") ?
                StringUtils.replace(splitByFirstFrom[0], "select", "select count(") + ")" :
                "select count(*)";
        sql = countFragment + sql;
        return Long.valueOf(dslContext().fetchOne(sql, query.getBindValues()).get(0).toString());
    }


    /**
     * <code><pre>
     * String sql = "(X = ? and Y = ?)";
     * Object[] bindings = new Object[] { 1, 2 };</pre></code>
     *
     * @param sql
     * @param bindings
     * @return
     */
    default Condition dslConditionSql(String sql, Object... bindings) {
        return condition(sql, bindings);
    }

    /**
     * <code><pre>
     * field.eq(25);
     * </pre></code>
     *
     * @param field
     * @return
     */
    default Condition dslConditionField(Field<Boolean> field) {
        return condition(field);
    }

    /**
     * conjunction a set of conditions use 'and' operator.
     *
     * @param conditions
     * @return
     */
    default Condition dslConditionsAnd(Condition... conditions) {
        return and(conditions);
    }

    /**
     * conjunction a set of conditions use 'or' operator.
     *
     * @param conditions
     * @return
     */
    default Condition dslConditionsOr(Condition... conditions) {
        return or(conditions);
    }


    default Condition dslConditionExists(Supplier<Select> select) {
        return exists(select.get());
    }

    default Condition dslConditionNotExists(Supplier<Select> select) {
        return notExists(select.get());
    }

    default Condition dslConditionNot(Condition condition) {
        return not(condition);
    }


    /**
     * <code><pre>
     *
     * DSLContext ctx = ...;
     *
     * Result<?> result =
     * ctx.select(T.A, T.B)
     *    .from(T)
     *
     *    // We always need this predicate
     *    .where(T.C.eq(1))
     *
     *    // This is only added conditionally
     *    .and(something
     *       ? T.D.eq(2)
     *       : DSL.noCondition())
     *    .fetch();
     *
     * </pre></code>
     *
     * @return
     */
    default Condition dslConditionNo() {
        return noCondition();
    }

    /**
     * create condition always return true.
     *
     * @return
     */
    default Condition dslConditionTrue() {
        return trueCondition();
    }

    /**
     * create condition always return false.
     *
     * @return
     */
    default Condition dslConditionFalse() {
        return falseCondition();
    }


    /**
     * get associated {@link Table} for current repository.
     *
     * @return
     */
    default T dslTable() {
        return (T) JooqGeneratedObjectRepository.getRequiredTable(getClass());
    }

    /**
     * get {@link Field} from current associated {@link Table}
     *
     * @param name
     * @return
     */
    default Field<T> dslField(String name) {
        return dslTable().field(name);
    }

    /**
     * create new record from current associated {@link Table}
     *
     * @return
     */
    default R dslNewRecord() {
        return (R) dslContext().newRecord(dslTable());
    }

    /**
     * create table by given qualifiedNames
     *
     * @param qualifiedName
     * @return
     */
    default Table dslNameToTable(String... qualifiedNames) {
        return table(name(qualifiedNames));
    }

    /**
     * get field by given qualifiedNames(type-safety.)
     *
     * @param type
     * @param qualifiedNames
     * @param <T>
     * @return
     */
    default <T> Field<T> dslNameToField(Class<T> type, String... qualifiedNames) {
        return field(name(qualifiedNames), type);
    }

    /**
     * get field by given qualifiedNames
     *
     * @param qualifiedNames
     * @return
     */
    default Field dslNameToField(String... qualifiedNames) {
        return field(name(qualifiedNames));
    }

    /**
     * get field by given {@link Table} and field name
     *
     * @param qualifiedNames
     * @return
     */
    default Field dslNameToField(Table table, String fieldName) {
        return dslNameToField(table.getName(), fieldName);
    }

    /**
     * get field by given {@link Table} and field name (type-safety.)
     *
     * @param qualifiedNames
     * @return
     */
    default Field<T> dslNameToField(Table table, String fieldName, Class<T> fieldType) {
        return dslNameToField(fieldType, table.getName(), fieldName);
    }

    /**
     * <code><pre>
     *
     * DSLContext ctx = ...;
     *
     * ctx.select(T.A, something ? T.B : DSL.inline("").as(T.B))
     *    .from(T)
     *    .where(T.C.eq(1))
     *    .and(T.D.eq(2))
     *    .fetch();
     *
     * -------------------------------------------------------------------
     *
     *
     * // First union subquery has a conditionally projected column
     * ctx.select(T.A, something ? T.B : DSL.inline("").as(T.B))
     *    .from(T)
     *    .where(T.C.eq(1))
     *    .and(T.D.eq(2))
     *
     *    .union(
     *
     * // Second union subquery has no such conditions
     *     select(U.A, U.B)
     *    .from(U))
     *    .fetch();
     *
     * -------------------------------------------------------------------
     *
     * ctx.select(T.A, T.B)
     *    .from(T)
     *    .union(
     *       something
     *         ? select(U.A, U.B).from(U)
     *         : select(inline(""), inline("")).where(falseCondition())
     *    )
     *    .fetch();
     *
     * -------------------------------------------------------------------
     *
     * ctx.select(
     *       T.A,
     *       T.B,
     *       something ? U.X : inline(""))
     *    .from(
     *       something
     *       ? T.join(U).on(T.Y.eq(U.Y))
     *       : T)
     *    .where(T.C.eq(1))
     *    .and(T.D.eq(2))
     *    .fetch();
     *
     * </pre>
     * </code>
     *
     * @param value
     * @param <T>
     * @return
     */
    default <T> Param<T> dslInline(T value) {
        return DSL.inline(value);
    }

    /**
     * sysdate,current_timestamp...
     *
     * @param keyWord
     * @return
     */
    default Keyword dslKeyWord(String keyWord) {
        return DSL.keyword(keyWord);
    }


    /**
     * utility class used for lookup a jooq generated objects(table,field,key...)
     */
    abstract class JooqGeneratedObjectRepository {


        /*cache jooq tables by JooqRepository class*/
        private static final Map<Class, Table> cachedTables = Maps.newConcurrentMap();

        /**
         * get {@link Table} implementation(code generated) by given repository class.
         *
         * @param jooqRepositoryImpl
         * @return
         */
        private static Table getRequiredTable(Class jooqRepositoryImpl) {
            return cachedTables.computeIfAbsent(jooqRepositoryImpl, key -> {
                Class tableClass = GenericTypes.getClassGenericType(key, JooqRepository.class, 0);
                Assert.notNull(tableClass, "no jooq repository generic-type found: " + key);
                Assert.isAssignable(Table.class, tableClass, key + " not a org.jooq.Table implementation.");
                try {
                    return (Table) tableClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
