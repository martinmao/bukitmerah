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
import org.scleropages.crud.FrameworkContext;
import org.scleropages.crud.orm.jpa.JpaContexts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.jooq.impl.DSL.*;

/**
 * Support for jOOQ.
 * <pre>
 * This interface can complement for {@link org.scleropages.crud.orm.jpa.GenericRepository}.
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


    default void dslMapEntity(R sourceRecord, E targetEntity) {
        Assert.isTrue(JpaContexts.isEntity(targetEntity), "not a entity instance: " + targetEntity);
        JpaContexts.EntityMetaModel<?> entityMetaModel = JpaContexts.getEntityMetaModel(targetEntity.getClass());
        entityMetaModel.singularBasicAttributes().forEach(singularAttribute -> {
            singularAttribute.getJavaMember();
        });
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
    default void dslPageable(Supplier<SelectFromStep> select, Pageable pageable) {
        Assert.notNull(select, "select mut not be null.");
        Assert.notNull(pageable, "pageable must not be null.");
        if (null != pageable.getSort()) {
            List<OrderField> orderFields = Lists.newArrayList();
            pageable.getSort().forEach(order -> {
                Field orderField = dslNameToField(order.getProperty());
                orderFields.add(order.getDirection().isAscending() ? orderField.asc() : orderField.desc());
            });
            select.get().orderBy(orderFields);
        }
        if (pageable.isPaged())
            select.get().offset(pageable.getOffset()).limit(pageable.getPageSize());
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
    default <E> Page<E> dslPage(List<E> content, Pageable pageable, Supplier<SelectFromStep> select, boolean useCountWrapped) {
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
    default Page<? extends Record> dslPage(Supplier<SelectFromStep> select, Pageable pageable, boolean useCountWrapped) {
        dslPageable(select, pageable);
        return dslPage(select.get().fetch(), pageable, select, useCountWrapped);
    }

    /**
     * query number of count results by given select.
     *
     * @param select
     * @param useCountWrapped use select count(*) from ( given select).
     * @return
     */
    default Long dslFetchCount(Supplier<SelectFromStep> select, boolean useCountWrapped) {
        SelectFromStep step = select.get();
        if (useCountWrapped)
            return Long.valueOf(dslContext().fetchCount(step));
        String sql = step.getSQL().toLowerCase();
        String[] splitByFirstFrom = org.springframework.util.StringUtils.split(sql, "from");
        Assert.notNull(splitByFirstFrom, "invalid sql. can not split by 'from' fragment. ");
        sql = " from " + splitByFirstFrom[1];
        sql = StringUtils.substringBefore(sql, "order by");
        String countFragment = splitByFirstFrom[0].contains("distinct") ?
                StringUtils.replace(splitByFirstFrom[0], "select", "select count(") + ")" :
                "select count(*)";
        sql = countFragment + sql;
        return Long.valueOf(dslContext().fetchOne(sql, step.getBindValues()).get(0).toString());
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
        public static Table getRequiredTable(Class jooqRepositoryImpl) {
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
