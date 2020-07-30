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
package org.scleropages.crud;

import org.hibernate.Hibernate;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Base mapstruct (https://mapstruct.org/) definitions for model -> entity (entity -> model) mappings:
 * <B>T=entity,M=model</B>
 * <pre>
 *     @ Mapper(config = ModelMapper.DefaultConfig.class)
 *     public interface CarMapper extends ModelMapper&lt;CarEntity,Car&gt{
 *
 *     }
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface ModelMapper<T, M> {

    @MapperConfig(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    interface DefaultConfig {

    }

    @Mapping(ignore = true, target = "id")
    T mapForSave(M model);


    @Mapping(ignore = true, target = "id")
    void mapForUpdate(M model, @MappingTarget T entity);

    M mapForRead(T entity);

    Iterable<M> mapForReads(Iterable<T> entities);

    Iterable<T> mapForSave(Iterable<M> models);

    /**
     * return true if given entity is not null and initialized by JPA provider.
     *
     * @param entity
     * @return
     */
    default boolean isEntityInitialized(Object... entity) {
        if (null == entity)
            return false;
        for (Object o : entity) {
            if (!Hibernate.isInitialized(o))
                return false;
        }
        return true;
    }
}
