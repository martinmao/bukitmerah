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
package org.scleropages.crud.orm;

import com.google.common.collect.Maps;
import org.mapstruct.factory.Mappers;
import org.scleropages.core.util.GenericTypes;
import org.scleropages.crud.FrameworkContext;
import org.scleropages.crud.GenericManager;
import org.scleropages.crud.orm.jpa.GenericRepository;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * Utility class used for lookup associated {@link ModelMapper} by given class type.<br>
 * The class type can be actual {@link ModelMapper} sub classes or {@link GenericManager} (lookup by generic-type argument[2]) or {@link GenericRepository} (lookup by generic-type argument[1]).
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class ModelMapperRepository {

    private static final Map<Class, ModelMapper> cachedModelMappers = Maps.newConcurrentMap();

    /**
     * return associated {@link ModelMapper} by given class type.
     *
     * @param clazz
     * @return
     */
    public final static ModelMapper getRequiredModelMapper(final Class clazz) {
        return cachedModelMappers.computeIfAbsent(clazz, input -> {
            if (ClassUtils.isAssignable(ModelMapper.class, clazz)) {
                return lookup(clazz, null);
            } else if (ClassUtils.isAssignable(GenericManager.class, clazz)) {
                Class actualMapperType = GenericTypes.getClassGenericType(clazz, GenericManager.class, 2);
                return lookup(actualMapperType, clazz);
            } else if (ClassUtils.isAssignable(GenericRepository.class, clazz)) {
                Class actualMapperType = GenericTypes.getClassGenericType(clazz, GenericRepository.class, 1);
                return lookup(actualMapperType, clazz);
            }
            throw new IllegalStateException("not supported parameter type: " + clazz);
        });
    }

    private final static ModelMapper lookup(final Class actualMapperType, final Class declarationClazz) {
        if (null == actualMapperType) {
            if (null != declarationClazz) {
                throw new IllegalArgumentException("no declaration generic-type of ModelMapper found by given class: " + declarationClazz);
            }
            throw new IllegalArgumentException("actual ModelMapper type must not be null.");
        }
        if (ClassUtils.isAssignable(ModelMapper.class, actualMapperType)) {
            Object bean = FrameworkContext.getBean(actualMapperType);
            ModelMapper modelMapper = (ModelMapper) (bean != null ? bean : Mappers.getMapper(actualMapperType));
            Assert.notNull(modelMapper, "ModelMapper not found by given type: " + actualMapperType);
            return modelMapper;
        } else if (null == declarationClazz) {
            throw new IllegalArgumentException("not an instanceof ModelMapper by given type: " + actualMapperType);
        }
        throw new IllegalArgumentException("declared generic-type :" + actualMapperType + " not is an ModelMapper at " + declarationClazz);
    }
}