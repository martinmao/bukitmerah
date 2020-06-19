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

import org.scleropages.crud.dao.orm.SearchFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.scleropages.crud.dao.orm.SearchFilter.SearchFilterBuilder.SEARCH_PREFIX;

/**
 * Wrapper class for {@link org.scleropages.crud.dao.orm.SearchFilter}'s map.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class WebSearchFilter {

    private final Map<String, SearchFilter> searchFilterMap;

    private WebSearchFilter(Map<String, SearchFilter> searchFilterMap) {
        this.searchFilterMap = searchFilterMap;
    }

    public static final WebSearchFilter of(HttpServletRequest request) {
        Map<String, Object> searchParams = Servlets.getParametersStartingWith(request,
                SEARCH_PREFIX);
        return new WebSearchFilter(SearchFilter.SearchFilterBuilder.build(searchParams));
    }

    public Map<String, SearchFilter> getSearchFilterMap() {
        return searchFilterMap;
    }
}
