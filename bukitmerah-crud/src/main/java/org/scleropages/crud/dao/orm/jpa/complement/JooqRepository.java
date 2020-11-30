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
package org.scleropages.crud.dao.orm.jpa.complement;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.scleropages.core.util.GenericTypes;
import org.scleropages.core.util.Reflections2;
import org.scleropages.crud.FrameworkContext;
import org.scleropages.crud.dao.orm.SearchFilter;
import org.scleropages.crud.dao.orm.jpa.JpaContexts;
import org.scleropages.crud.dao.orm.jpa.JpaContexts.ManagedTypeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static javax.persistence.metamodel.Attribute.PersistentAttributeType.*;
import static org.jooq.impl.DSL.*;

/**
 * Support for jOOQ.
 * <pre>
 * This interface can complement for {@link org.scleropages.crud.dao.orm.jpa.GenericRepository}. all method defined start with 'dsl'.
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
     * SPI接口，在{@link #dslRecordInto(Record, Object, ReferenceEntityAssembler)}中使用
     */
    interface ReferenceEntityAssembler<X> {


        /**
         * 根据给定的表创建目标实体，并将依赖关系应用到目标实体<br>
         * 默认情况下，将根据root entity中的依赖关系进行查找匹配的目标实体，仅支持单引用(one to one,many to one)创建映射，不支持集合属性（one to many）的映射<br>
         * 如果不处理该表，返回null.
         *
         * @param rootEntity      根实体
         * @param rootEntityModel 根实体元数据模型
         * @param table           表名
         * @param record          数据记录
         * @return 表对应的目标实体
         */
        default Object apply(X rootEntity, ManagedTypeModel<X> rootEntityModel, String table, Record record) {
            return applyInternal(rootEntity, rootEntityModel, table, record);
        }


        /**
         * 默认实现，不应该覆写该方法
         *
         * @param rootEntity
         * @param rootEntityModel
         * @param table
         * @param record
         * @return
         */
        default Object applyInternal(X rootEntity, ManagedTypeModel<X> rootEntityModel, String table, Record record) {
            Attribute<?, Object> tableField = rootEntityModel.attributeByDatabaseTable(table);
            Assert.notNull(tableField, () -> "no referenced(singular) table [" + table + "] found from entity: " + rootEntity.getClass());
            Object o = Reflections2.newInstance(tableField.getJavaType());
            Reflections2.invokeSet(rootEntity, tableField.getName(), o);
            return o;
        }


        /**
         * 扩展改类实现如何将引用持久化属性值(外键值)应用到目标实体,默认情况下创建关联实体，将外键值设置到目标实体关联实体的id属性上，并将关联实体与目标实体进行关联.
         *
         * @return
         */
        default void applyReferenceIdToTargetEntity(Object targetEntity, Attribute refAttribute, Field field, Object fieldValue) {
            applyReferenceIdToTargetEntityInternal(targetEntity, refAttribute, field, fieldValue);
        }

        /**
         * 默认实现，不应该覆写该方法
         *
         * @param targetEntity
         * @param refAttribute
         * @param field
         * @param fieldValue
         */
        default void applyReferenceIdToTargetEntityInternal(Object targetEntity, Attribute refAttribute, Field field, Object fieldValue) {
            if (null == fieldValue)
                return;
            Class targetReferencedEntityClazz = refAttribute.getJavaType();
            Attribute targetReferencedEntityId = JpaContexts.getManagedTypeModel(targetReferencedEntityClazz).getAttributeOfId();
            Object o = Reflections2.newInstance(targetReferencedEntityClazz);
            Reflections2.invokeSet(o, targetReferencedEntityId.getName(), fieldValue);
            Reflections2.invokeSet(targetEntity, refAttribute.getName(), o);
        }


    }


    /**
     * map a jooq record to given entity.
     * jOOQ默认record into entity,仅支持声明了@Column的属性.即basic field。该方法提供了更多的映射方案
     * <pre>
     *     支持的关系：
     *     BASIC属性直接设置到目标实体
     *     EMBEDDED属性会创建目标实体的关联实体对象并设置到目标属性
     *     MANY_TO_ONE 属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性
     *     ONE_TO_ONE 属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性
     *     不支持的关系：
     *     ONE_TO_MANY
     *     MANY_TO_MANY
     *     ELEMENT_COLLECTION
     * </pre>
     * NOTE:
     * <pre>
     *      map过程不进行类型转换而直接设置到目标属性，如遇到类型问题自行覆写{@link #dslGetEntityBasicAttributeValue(Field, Object)}
     *      默认不支持两层(包括)以上的关联（仅会处理与root entity关联类型）.多层次关系映射策略可以通过 referenceEntityAssembler 进行指定
     * </pre>
     *
     * @param sourceRecord
     * @param rootEntity
     * @param referenceEntityAssembler
     * @param <X>
     */
    default <X> void dslRecordInto(Record sourceRecord, X rootEntity, ReferenceEntityAssembler referenceEntityAssembler) {
        Assert.notNull(sourceRecord, "sourceRecord must not be null.");
        Assert.notNull(rootEntity, "targetEntity must not be null.");
        Assert.notNull(referenceEntityAssembler, "referenceEntityAssembler must not be null.");
        Assert.isTrue(JpaContexts.isEntity(rootEntity), "not a entity instance: " + rootEntity);

        Class rootEntityClazz = rootEntity.getClass();

        ManagedTypeModel<?> rootEntityModel = JpaContexts.getManagedTypeModel(rootEntityClazz);

        Map<String, Object> tableToEntity = Maps.newHashMap();//table name map to entity instance.

        Stream.of(sourceRecord.fields()).forEach(field -> {
            Object value = field.getValue(sourceRecord);
            if (null == value)
                return;
            String[] qualifiedName = field.getQualifiedName().getName();
            Assert.isTrue(qualifiedName.length == 2, () -> "invalid field qualified name [" + Arrays.toString(qualifiedName) + "]. required format: '<TABLE_NAME>.<FIELD_NAME>'. ");
            String tableName = qualifiedName[0];
            String fieldName = qualifiedName[1];
            Object targetEntity = tableToEntity.computeIfAbsent(tableName, k -> {
                if (k.equalsIgnoreCase(rootEntityModel.table())) {
                    return rootEntity;
                }
                Object apply = referenceEntityAssembler.apply(rootEntity, rootEntityModel, k, sourceRecord);
//                Assert.notNull(apply, "referenceEntityAssembler eval result must not be null for table: " + k);
                if (null == apply)
                    return StringUtils.EMPTY;
                return apply;
            });
            if (Objects.equals(targetEntity, StringUtils.EMPTY)) {
                return;
            }
            Class<?> targetEntityClazz = targetEntity.getClass();
            ManagedTypeModel<?> entityModel = JpaContexts.getManagedTypeModel(targetEntityClazz);
            Attribute<?, Object> columnAttribute = entityModel.attributeByDatabaseColumn(fieldName);
            Assert.notNull(columnAttribute, () -> "no entity attribute(singular) found by column: " + fieldName + " from entity: " + targetEntityClazz);
            PersistentAttributeType persistentType = columnAttribute.getPersistentAttributeType();
            if (persistentType == BASIC) {
                populateBasicFieldValueToEntity(field, value, targetEntity, columnAttribute);
            } else if (persistentType == EMBEDDED) {
                dslMapEmbeddedAttribute(targetEntity, field, fieldName, value, columnAttribute);
            } else if (persistentType == MANY_TO_ONE || persistentType == ONE_TO_ONE) {
                referenceEntityAssembler.applyReferenceIdToTargetEntity(targetEntity, columnAttribute, field, value);
            } else {
                JooqRepositoryUtil.logger.warn("not support mapping for {} to entity: {}", Arrays.toString(qualifiedName), targetEntity.getClass().getSimpleName());
            }
        });
    }

    /**
     * map a jooq record to given entity.
     * jOOQ默认record into entity,仅支持声明了@Column的属性.即basic field。该方法提供了更多的映射方案
     * <pre>
     *     支持的关系：
     *     BASIC属性直接设置到目标实体
     *     EMBEDDED属性会创建目标实体的关联实体对象并设置到目标属性
     *     MANY_TO_ONE 属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性
     *     ONE_TO_ONE 属性的目标实体必须为关系维护方，会创建关联实体对象并设置到目标属性
     *     不支持的关系：
     *     ONE_TO_MANY
     *     MANY_TO_MANY
     *     ELEMENT_COLLECTION
     * </pre>
     * NOTE:
     * <pre>
     *      map过程不进行类型转换而直接设置到目标属性，如遇到类型问题自行覆写{@link #dslGetEntityBasicAttributeValue(Field, Object)}
     *      默认不支持两层(包括)以上的关联（仅会处理与root entity关联类型）.多层次关系映射策略可以通过 referenceEntityAssembler 进行指定.参考方法： {@link #dslRecordInto(Record, Object, ReferenceEntityAssembler)}
     * </pre>
     *
     * @param sourceRecord
     * @param rootEntity
     * @param <X>
     */
    default <X> void dslRecordInto(Record sourceRecord, X rootEntity) {
        dslRecordInto(sourceRecord, rootEntity, JooqRepositoryUtil.DEFAULT_REFERENCE_ENTITY_ASSEMBLER);
    }


    default void populateBasicFieldValueToEntity(Field field, Object fieldValue, Object targetEntity, Attribute fieldAttribute) {
        try {
            Object value = dslGetEntityBasicAttributeValue(field, fieldValue);
            if (null == value)
                return;
            Reflections2.invokeSet(targetEntity, fieldAttribute.getName(), value);
        } catch (ClassCastException ex) {
            throw new IllegalStateException("incompatible type from: " + field + " to entity property: " + fieldAttribute.getName() + ". you must overrides dslGetEntityBasicAttributeValue for type conversion.", ex);
        }
    }

    /**
     * overrides this method how to convert a record field value as entity basic property. by default nothing to do return directly.
     *
     * @param field
     * @param jooqFieldValue
     * @return null if no values to mapped.
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
    default void dslMapEmbeddedAttribute(Object targetEntity, Field field, String fieldName, Object fieldValue, Attribute<?, Object> fieldAttribute) {
        Object value = dslGetEntityBasicAttributeValue(field, fieldValue);
        if (null == value)
            return;
        ManagedTypeModel<Object> associatedTypeMode = JpaContexts.getManagedTypeModel(fieldAttribute.getJavaType());
        Attribute<Object, Object> associatedFieldAttribute = associatedTypeMode.attributeByDatabaseColumn(fieldName);
        if (null != associatedFieldAttribute) {
            String innerName = associatedFieldAttribute.getName();
            try {
                Reflections2.invokeSet(targetEntity, fieldAttribute.getName() + "." + innerName, value);
            } catch (ClassCastException ex) {
                throw new IllegalStateException("incompatible type from: " + field + " to entity property: " + innerName + ". you must overrides dslGetEntityBasicAttributeValue for type conversion.", ex);
            }
        }
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
     * @param applySort true will apply sort to select query.
     */
    default void dslPageable(Supplier<SelectQuery> select, Pageable pageable, boolean applySort) {
        Assert.notNull(select, "select mut not be null.");
        Assert.notNull(select.get(), "select not supply.");
        Assert.notNull(pageable, "pageable must not be null.");
        SelectQuery query = select.get();
        if (applySort && null != pageable.getSort()) {
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
     * @param <E>
     * @return
     */
    default <E> Page<E> dslPage(List<E> content, Pageable pageable, Supplier<SelectQuery> select, boolean useCountWrapped) {
        return PageableExecutionUtils.getPage(content, pageable, () -> dslCountQuery(select, useCountWrapped));
    }


    /**
     * Create spring data {@link Page} by given content(any query result).
     *
     * @param content
     * @param pageable
     * @param countSql
     * @param countSqlBindValues
     * @param <E>
     * @return
     */
    default <E> Page<E> dslPage(List<E> content, Pageable pageable, String countSql, List<Object> countSqlBindValues) {
        return PageableExecutionUtils.getPage(content, pageable, () -> dslCountQuery(countSql, countSqlBindValues));
    }


    /**
     * Create spring data {@link Page} from query result by given select.
     *
     * @param select
     * @param pageable
     * @param useCountWrapped
     * @param applySort
     * @return
     */
    default Page<? extends Record> dslPage(Supplier<SelectQuery> select, Pageable pageable, boolean useCountWrapped, boolean applySort) {
        SelectQuery<Record> selectQuery = select.get();
        String countSql = selectQuery.getSQL();//sql for count query.
        List<Object> bindValues = selectQuery.getBindValues();//bind values for count query.
        dslPageable(select, pageable, applySort);//apply spring data pageable to source query.
        return dslPage(select.get().fetch(), pageable, select, useCountWrapped);
    }


    /**
     * create spring data {@link Page} from query result by given select and group of {@link SearchFilter}s.
     *
     * @param select
     * @param pageable
     * @param searchFilter
     * @param useCountWrapped
     * @return
     */
    default Page<? extends Record> dslPage(Supplier<SelectQuery> select, Pageable pageable, Map<String, SearchFilter> searchFilter, boolean useCountWrapped, boolean applySort) {
        Assert.notNull(select, "select must not be null.");
        Assert.notNull(select.get(), "select not supply.");
        Assert.notNull(searchFilter, "searchFilter must not be null.");
        Condition condition = JpaSupportJooqConditions.bySearchFilters(select.get(), JpaContexts.getManagedTypeModel(dslEntityType()), searchFilter.values());
        select.get().addConditions(condition);
        return dslPage(select, pageable, useCountWrapped, applySort);
    }

    default Class<E> dslEntityType() {
        return GenericTypes.getClassGenericType(getClass(), JooqRepository.class, 2);
    }


    /**
     * query number of count results by given select.
     *
     * @param select
     * @param useCountWrapped use select count(*) from (given select).
     * @return
     */
    default Long dslCountQuery(Supplier<SelectQuery> select, boolean useCountWrapped) {
        SelectQuery query = select.get();
        if (useCountWrapped)
            return Long.valueOf(dslContext().select(DSL.count()).from(query.asTable()).fetchOne().value1());
        return dslCountQuery(query.getSQL(), query.getBindValues());
    }

    /**
     * query number of count results by given sql.
     *
     * @param sql
     * @param bindValues
     * @return
     */
    default Long dslCountQuery(String sql, List<Object> bindValues) {
        Assert.hasText(sql, "sql must not be empty for count query.");
        sql = sql.toLowerCase();
        String[] splitByFirstFrom = org.springframework.util.StringUtils.split(sql, "from");
        Assert.notNull(splitByFirstFrom, "invalid sql. can not split by 'from' fragment. ");
        sql = " from " + splitByFirstFrom[1];
        sql = StringUtils.substringBefore(sql, "order by");
        String countFragment = splitByFirstFrom[0].contains("distinct") ?
                StringUtils.replace(splitByFirstFrom[0], "select", "select count(") + ")" :
                "select count(*)";
        sql = countFragment + sql;
        return Long.valueOf(dslContext().fetchOne(sql, null != bindValues ? bindValues.toArray(new Object[bindValues.size()]) : null).get(0).toString());
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
     * conjunction a set of conditions use 'and' operator.
     *
     * @param conditions
     * @return
     */
    default Condition dslConditionsAnd(Collection<Condition> conditions) {
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
    default Field dslField(String name) {
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
     * @param qualifiedNames
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
     * @param <F>
     * @return
     */
    default <F> Field<F> dslNameToField(Class<F> type, String... qualifiedNames) {
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
     * @param table
     * @param fieldName
     * @return
     */
    default Field dslNameToField(Table table, String fieldName) {
        return dslNameToField(table.getName(), fieldName);
    }

    /**
     * get field by given {@link Table} and field name (type-safety.)
     *
     * @param table
     * @param fieldName
     * @param fieldType
     * @param <F>
     * @return
     */
    default <F> Field<F> dslNameToField(Table table, String fieldName, Class<F> fieldType) {
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


    abstract class JooqRepositoryUtil {
        private static Logger logger = LoggerFactory.getLogger(JooqRepository.class.getName());
        private static final ReferenceEntityAssembler DEFAULT_REFERENCE_ENTITY_ASSEMBLER = new ReferenceEntityAssembler() {
        };
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
