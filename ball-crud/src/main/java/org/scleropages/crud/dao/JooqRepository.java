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
import org.jooq.DSLContext;
import org.jooq.SelectLimitPercentAfterOffsetStep;
import org.jooq.SelectLimitStep;
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
    default SelectLimitPercentAfterOffsetStep applyPageable(SelectLimitStep select, Pageable pageable) {
        return select.offset(pageable.getOffset()).limit(pageable.getPageSize());
    }

    /**
     * Create spring data {@link Page}.
     *
     * @param content query results.
     * @param pageable
     * @param select
     * @param <T>
     * @return
     */
    default <T> Page<T> createPage(List<T> content, Pageable pageable, SelectLimitStep select) {
        Assert.notNull(select, "select must not be null.");
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            String sql = select.getSQL().toLowerCase();
            String[] splitByFirstFrom = org.springframework.util.StringUtils.split(sql, "from");
            Assert.notNull(splitByFirstFrom, "invalid sql. can not split by 'from' fragment. ");
            sql = " from " + splitByFirstFrom[1];
            sql = StringUtils.substringBefore(sql, "order by");
            String selectFragment = splitByFirstFrom[0].contains("distinct") ?
                    StringUtils.replace(splitByFirstFrom[0], "select", "select count(") + ")" :
                    "select count(*)";
            sql = selectFragment + sql;
            return (Long) dslContext().fetchOne(sql, select.getBindValues()).get(0);
        });
    }
}
