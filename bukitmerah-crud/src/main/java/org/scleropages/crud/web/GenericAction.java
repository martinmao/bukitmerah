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

import org.scleropages.core.mapper.JsonMapper2;
import org.scleropages.crud.dao.orm.PageableBuilder;
import org.scleropages.crud.dao.orm.SearchFilter;
import org.scleropages.crud.dao.orm.SortBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

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
     * build a object using given json text and excepted type.
     *
     * @param payload
     * @param exceptedType
     * @param <T>
     * @return
     */
    default <T> T buildObjectFromJsonPayload(String payload, Class<T> exceptedType) {
        Assert.hasText(payload, "bad request. payload not allow empty text.");
        return JsonMapper2.fromJson(payload, exceptedType);
    }
}
