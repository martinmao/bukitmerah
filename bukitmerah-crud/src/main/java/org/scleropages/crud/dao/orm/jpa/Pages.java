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
package org.scleropages.crud.dao.orm.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * utility class for spring data page.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Pages {

    /**
     * wrap spring data page as serializable page.
     *
     * @param page
     * @return
     */
    public static Page serializablePage(Page page) {
        if (page instanceof org.scleropages.crud.dao.orm.jpa.PageImpl)
            return page;
        Assert.isInstanceOf(PageImpl.class, page, "given page must be standard spring page.");
        return new org.scleropages.crud.dao.orm.jpa.PageImpl((PageImpl) page);
    }

    /**
     * create an serializable pageable for rpc request.
     *
     * @return
     */
    public static Pageable serializablePageable(Pageable pageable) {
        if (pageable instanceof PageRequestImpl) {
            return pageable;
        }
        return new PageRequestImpl(pageable);
    }
}
