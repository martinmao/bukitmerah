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
package org.scleropages.core;

/**
 * 实现该接口的组件，需提供id名称，id应全局唯一，
 * 在候选组件 {@link AbstractLookupComponent#autoLookupComponentsIfNecessary()}自动注册时，
 * 如果实现了该接口将调用 {@link #id()}方法来设置组件id，否则使用spring beanId
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface ComponentId {

    String id();
}