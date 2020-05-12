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

import org.scleropages.core.util.GenericTypes;
import org.scleropages.crud.dao.orm.SearchFilter;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Map;
import java.util.Optional;

/**
 * Generic Repository based {@link JpaRepository}, provided some common method
 * <p>
 * implementations must defined generic-type in class declaration. <br>
 * <pre>
 * T=entity type
 * ID= id type
 * </pre>
 * <p>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@CacheConfig(cacheResolver = "defaultCacheResolver")
public interface GenericRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {


    /**
     * find page of entities from repository by pageable.
     *
     * @param pageable
     * @return
     */
    default Page<T> findPage(Pageable pageable) {
        return findAll(pageable);
    }

    /**
     * find page of entities from repository by pageable and specification.
     *
     * @param spec
     * @param pageable
     * @return
     */
    default Page<T> findPage(Specification<T> spec, Pageable pageable) {
        return findAll(spec, pageable);
    }

    /**
     * find page of entities from repository by pageable and search filters.
     *
     * @param searchFilters
     * @param pageable
     * @return
     */
    default Page<T> findPage(Map<String, SearchFilter> searchFilters, Pageable pageable) {
        Specification<T> spec = SearchFilterSpecifications.bySearchFilter(searchFilters.values(), GenericTypes.getClassGenericType(getClass(), GenericRepository.class, 2));
        return findPage(spec, pageable);
    }


    /**
     * get one entity from repository by given specification.
     *
     * @param spec
     * @return
     */
    default Optional<T> get(Specification<T> spec) {
        return findOne(spec);
    }

    /**
     * get one entity from repository by id .
     *
     * @param id
     * @return
     */
    default Optional<T> get(ID id) {
        return findById(id);
    }
}
