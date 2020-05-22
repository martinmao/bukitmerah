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

import com.google.common.collect.Maps;
import org.scleropages.crud.FrameworkContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class JpaRepositories {

    private static final Map<Class, CrudRepository> repositories = Maps.newConcurrentMap();


    protected static CrudRepository getRequiredCurdRepository(Class repositoryClass) {
        if (ClassUtils.isAssignable(CrudRepository.class, repositoryClass)) {
            return repositories.computeIfAbsent(repositoryClass, key -> (CrudRepository) FrameworkContext.getBean(repositoryClass));
        }
        throw new IllegalStateException("not an CrudRepository implementation repositoryClass: "+repositoryClass);
    }
}
