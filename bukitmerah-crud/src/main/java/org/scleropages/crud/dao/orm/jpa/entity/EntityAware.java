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
package org.scleropages.crud.dao.orm.jpa.entity;

/**
 * 在manager之间进行关联设置时使用，即不同manager 如果不想让repository产生交集，通过该接口设置依赖关系，此处存在entity 泄露风险，谨慎使用。
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface EntityAware<T> {

    /**
     * 设置关系到目标实体
     * @param t
     */
    void setEntity(T t);
}
