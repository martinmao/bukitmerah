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
package org.scleropages.crud.dao.orm;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class PageableBuilder {

    public static final String PAGE_PREFIX = "page_";

    public static final String PAGE_NUMBER_PARAM_NAME = "no";

    public static final String PAGE_SIZE_PARAM_NAME = "size";

    public static final int DEFAULT_PAGE_NO = 1;

    public static final int DEFAULT_PAGE_SIZE = 25;

    public static final Pageable DEFAULT_PAGE = PageRequest.of(DEFAULT_PAGE_NO - 1, DEFAULT_PAGE_SIZE,
            SortBuilder.DEFAULT_SORT);

    public static final Pageable DEFAULT_NOSORT_PAGE = PageRequest.of(DEFAULT_PAGE_NO - 1, DEFAULT_PAGE_SIZE);

    /**
     * Page parameter is page_no=? and page_size=? if parameters not found used
     * default settings.
     *
     * @param pageParams
     * @param sort
     * @return
     */
    public static final Pageable build(Map<String, Object> pageParams, Sort sort) {

        if (CollectionUtils.isEmpty(pageParams) && null == sort)
            return DEFAULT_NOSORT_PAGE;

        Object no = pageParams.get(PAGE_NUMBER_PARAM_NAME);
        Object size = pageParams.get(PAGE_SIZE_PARAM_NAME);

        int pageNumber = no != null ? Integer.parseInt((String) no) : DEFAULT_PAGE_NO;
        int pageSize = size != null ? Integer.parseInt((String) size) : DEFAULT_PAGE_SIZE;

        sort = sort != null ? sort : SortBuilder.DEFAULT_NO_SORT;
        pageNumber = pageNumber == 0 ? 1 : pageNumber;

        /* zero-based page index. */
        return PageRequest.of(pageNumber - 1, pageSize, sort);
    }
}
