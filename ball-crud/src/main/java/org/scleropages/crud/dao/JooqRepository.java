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

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Keyword;
import org.jooq.Param;
import org.jooq.Select;
import org.jooq.SelectLimitPercentAfterOffsetStep;
import org.jooq.SelectLimitStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.scleropages.crud.FrameworkContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import java.util.List;

import static org.jooq.impl.DSL.*;

/**
 * Support for jOOQ.
 * This interface is complement for {@link org.scleropages.crud.orm.jpa.GenericRepository}.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface JooqRepository {


    /**
     * lookup a available {@link org.jooq.DSLContext} from {@link FrameworkContext}.
     *
     * @return
     */
    default DSLContext dslContext() {
        return FrameworkContext.getBean(DSLContext.class);
    }

    /**
     * Apply spring data {@link Pageable} to current select.
     *
     * @param select
     * @param pageable
     * @return
     */
    default SelectLimitPercentAfterOffsetStep pageable(SelectLimitStep select, Pageable pageable) {
        return select.offset(pageable.getOffset()).limit(pageable.getPageSize());
    }

    /**
     * Create spring data {@link Page}.
     *
     * @param content  query results.
     * @param pageable
     * @param select
     * @param <T>
     * @return
     */
    default <T> Page<T> page(List<T> content, Pageable pageable, SelectLimitStep select) {

        Assert.notNull(select, "select must not be null.");
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            String sql = select.getSQL().toLowerCase();
            String[] splitByFirstFrom = org.springframework.util.StringUtils.split(sql, "from");
            Assert.notNull(splitByFirstFrom, "invalid sql. can not split by 'from' fragment. ");
            sql = " from " + splitByFirstFrom[1];
            sql = StringUtils.substringBefore(sql, "order by");
            String countFragment = splitByFirstFrom[0].contains("distinct") ?
                    StringUtils.replace(splitByFirstFrom[0], "select", "select count(") + ")" :
                    "select count(*)";
            sql = countFragment + sql;
            return (Long) dslContext().fetchOne(sql, select.getBindValues()).get(0);
        });
    }

    /**
     * <code><pre>
     * String sql = "(X = ? and Y = ?)";
     * Object[] bindings = new Object[] { 1, 2 };</pre></code>
     * @param sql
     * @param bindings
     * @return
     */
    default Condition conditionSql(String sql, Object... bindings) {
        return condition(sql, bindings);
    }

    default Condition conditionField(Field<Boolean> field) {
        return condition(field);
    }

    default Condition conditionsAnd(Condition... conditions) {
        return and(conditions);
    }

    default Condition conditionsOr(Condition... conditions) {
        return or(conditions);
    }

    default Condition conditionExists(Select select) {
        return exists(select);
    }

    default Condition conditionNotExists(Select select) {
        return notExists(select);
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
     * @return
     */
    default Condition conditionNo() {
        return noCondition();
    }

    default Condition conditionTrue() {
        return trueCondition();
    }

    default Condition conditionFalse() {
        return falseCondition();
    }

    default Table nameToTable(String... qualifiedName) {
        return table(name(qualifiedName));
    }

    default <T> Field<T> nameToField(Class<T> type, String... qualifiedName) {
        return field(name(qualifiedName),type);
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
     *-------------------------------------------------------------------
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
     *-------------------------------------------------------------------
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
     *-------------------------------------------------------------------
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
     * @param keyWord
     * @return
     */
    default Keyword keyWord(String keyWord){
        return DSL.keyword(keyWord);
    }
}
