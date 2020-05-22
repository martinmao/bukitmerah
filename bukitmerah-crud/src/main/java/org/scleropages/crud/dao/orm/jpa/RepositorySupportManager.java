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
import org.scleropages.crud.GenericManager;
import org.scleropages.crud.exception.BizStateViolationException;
import org.scleropages.crud.types.Available;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * 可以应用于 {@link GenericManager}实现类上. 泛型参数确定匹配的Repository.
 * 基于该匹配的Repository提供一些通用的管理功能。简化 generic manager 开发
 * R 为 {@link org.springframework.data.repository.CrudRepository} 子类.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface RepositorySupportManager<ID, R extends CrudRepository> {

    /**
     * enable or disable a model available state by id.
     *
     * @param id
     * @param enableOrDisable true make to enable or false make to disable.
     */
    @Transactional
    default void enableOrDisableById(ID id, boolean enableOrDisable) {
        Class repositoryClass = GenericTypes.getClassGenericType(getClass(), RepositorySupportManager.class, 1);
        CrudRepository requiredCurdRepository = JpaRepositories.getRequiredCurdRepository(repositoryClass);
        Optional optionalEntity = requiredCurdRepository.findById(id);
        Assert.isTrue(optionalEntity.isPresent(), "given id not found.");
        Object entity = optionalEntity.get();
        if (entity instanceof Available) {
            Available availableEntity = (Available) entity;
            if (availableEntity.availableState() && !enableOrDisable) {
                availableEntity.disable();
                requiredCurdRepository.save(availableEntity);
                return;
            } else if (!availableEntity.availableState() && enableOrDisable) {
                availableEntity.enable();
                requiredCurdRepository.save(availableEntity);
                return;
            }
        } else {
            throw new BizStateViolationException("not support operation for: " + getClass().getName() + "#enableOrDisableById for id: " + id);
        }
    }
}
