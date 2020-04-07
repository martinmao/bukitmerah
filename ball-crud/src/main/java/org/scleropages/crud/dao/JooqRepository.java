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
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.scleropages.crud.FrameworkContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Supplier;

import static org.jooq.impl.DSL.*;

/**
 * Support for jOOQ.
 * <pre>
 * This interface can complement for {@link org.scleropages.crud.orm.jpa.GenericRepository}.
 * many of method arguments is a subclasses of Reactive({@link org.reactivestreams.Publisher}) implementations(eg:{@link Select} subclasses). but that is not compatibility with spring data-jpa.
 * it will throw "org.springframework.dao.InvalidDataAccessApiUsageException: Reactive Repositories are not supported by JPA."
 * so wrapped these arguments as a {@link Supplier}.
 * The declaration of generic type of 0 is Jooq {@link Table} implementation. and 1 is entity type (annotated with {@link javax.persistence.Entity}) .
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@NoRepositoryBean
public interface JooqRepository<R, T> {


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
    default void pageable(Supplier<SelectFromStep> select, Pageable pageable) {
        Assert.notNull(select, "select mut not be null.");
        Assert.notNull(pageable, "pageable must not be null.");
        if (null != pageable.getSort()) {
            List<OrderField> orderFields = Lists.newArrayList();
            pageable.getSort().forEach(order -> {
                Field orderField = nameToField(order.getProperty());
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
    default <E> Page<E> page(List<E> content, Pageable pageable, Supplier<SelectFromStep> select, boolean useCountWrapped) {
        Assert.notNull(select, "select must not be null.");
        return PageableExecutionUtils.getPage(content, pageable, () -> fetchCount(select, useCountWrapped));
    }


    /**
     * Create spring data {@link Page} by given select
     *
     * @param select
     * @param pageable
     * @param useCountWrapped
     * @return
     */
    default Page<? extends Record> page(Supplier<SelectFromStep> select, Pageable pageable, boolean useCountWrapped) {
        pageable(select, pageable);
        return page(select.get().fetch(), pageable, select, useCountWrapped);
    }

    /**
     * query number of count results by given select.
     *
     * @param select
     * @param useCountWrapped use select count(*) from ( given select).
     * @return
     */
    default Long fetchCount(Supplier<SelectFromStep> select, boolean useCountWrapped) {
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
    default Condition conditionSql(String sql, Object... bindings) {
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
    default Condition conditionField(Field<Boolean> field) {
        return condition(field);
    }

    /**
     * conjunction a set of conditions use 'and' operator.
     *
     * @param conditions
     * @return
     */
    default Condition conditionsAnd(Condition... conditions) {
        return and(conditions);
    }

    /**
     * conjunction a set of conditions use 'or' operator.
     *
     * @param conditions
     * @return
     */
    default Condition conditionsOr(Condition... conditions) {
        return or(conditions);
    }


    default Condition conditionExists(Supplier<Select> select) {
        return exists(select.get());
    }

    default Condition conditionNotExists(Supplier<Select> select) {
        return notExists(select.get());
    }

    default Condition conditionNot(Condition condition) {
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
    default Condition conditionNo() {
        return noCondition();
    }

    /**
     * create condition always return true.
     *
     * @return
     */
    default Condition conditionTrue() {
        return trueCondition();
    }

    /**
     * create condition always return false.
     *
     * @return
     */
    default Condition conditionFalse() {
        return falseCondition();
    }

    /**
     * create table by given qualifiedNames
     *
     * @param qualifiedName
     * @return
     */
    default Table nameToTable(String... qualifiedNames) {
        return table(name(qualifiedNames));
    }

    /**
     * create field by given qualifiedNames(type-safety.)
     *
     * @param type
     * @param qualifiedNames
     * @param <T>
     * @return
     */
    default <T> Field<T> nameToField(Class<T> type, String... qualifiedNames) {
        return field(name(qualifiedNames), type);
    }

    /**
     * create field by given qualifiedNames
     *
     * @param qualifiedNames
     * @return
     */
    default Field nameToField(String... qualifiedNames) {
        return field(name(qualifiedNames));
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
    default <T> Param<T> inline(T value) {
        return DSL.inline(value);
    }

    /**
     * sysdate,current_timestamp...
     *
     * @param keyWord
     * @return
     */
    default Keyword keyWord(String keyWord) {
        return DSL.keyword(keyWord);
    }
}
