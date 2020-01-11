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
package org.scleropages.crud.web;

import org.scleropages.crud.orm.PageableBuilder;
import org.scleropages.crud.orm.SearchFilter;
import org.scleropages.crud.orm.SortBuilder;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Base web controller for business models.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface GenericAction {


    /**
     * Build {@link Pageable} for page search from {@link HttpServletRequest} by default conventions
     *
     * @param request
     * @return
     */
    default Pageable buildPageableFromRequest(HttpServletRequest request) {
        Map<String, Object> pageParams = Servlets.getParametersStartingWith(request, PageableBuilder.PAGE_PREFIX);
        Map<String, Object> sortParams = Servlets.getParametersStartingWith(request, SortBuilder.SORT_PREFIX);
        return PageableBuilder.build(pageParams, SortBuilder.build(sortParams));
    }

    /**
     * Build a group of {@link SearchFilter} for condition search from {@link HttpServletRequest} by default conventions
     *
     * @param request
     * @return
     */
    default Map<String, SearchFilter> buildSearchFilterFromRequest(HttpServletRequest request) {
        Map<String, Object> searchParams = Servlets.getParametersStartingWith(request,
                SearchFilter.SearchFilterBuilder.SEARCH_PREFIX);
        return SearchFilter.SearchFilterBuilder.build(searchParams);
    }
}
