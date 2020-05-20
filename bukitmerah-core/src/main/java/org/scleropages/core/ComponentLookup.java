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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * 组件动态切换.可切换组件需实现 {@link AbstractLookupComponent}
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ComponentLookup {


    /**
     * 组件类型
     *
     * @return
     */
    Class componentType();

    /**
     * 组件id，组件如果实现了 {@link ComponentId#id()} 作为id，否则使用spring beanId<br>
     * NOTE：如果在注册时并未使用自动注册 {@link AbstractLookupComponent#autoLookupComponentsIfNecessary()}，
     * 则id以注册时配置为准 {@link AbstractLookupComponent#setTargetComponents(Map)}为准
     *
     * @return
     */
    String value();


    /**
     * 其他多个附加的components ，格式 id=componentClass
     *
     * @return
     */
    String[] additionalComponents() default {};
}
