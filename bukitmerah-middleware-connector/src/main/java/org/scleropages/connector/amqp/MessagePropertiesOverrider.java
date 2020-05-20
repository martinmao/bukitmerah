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
package org.scleropages.connector.amqp;

import org.springframework.amqp.core.MessageProperties;

/**
 * 实现该接口的 object message 在发送前，有一次修改 {@link org.springframework.amqp.core.MessageProperties}的机会，其应用的优先级高于一切,
 * 例如 对于content-type属性，当前object message 只支持一种序列化技术，此时可以覆盖content-type属性，避免调用传参或配置修改了这个默认设置.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface MessagePropertiesOverrider {

    void overrideMessageProperties(MessageProperties messageProperties);
}
