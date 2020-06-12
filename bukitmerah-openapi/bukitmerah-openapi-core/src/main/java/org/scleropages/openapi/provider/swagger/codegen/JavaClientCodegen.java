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
package org.scleropages.openapi.provider.swagger.codegen;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JavaClientCodegen extends org.openapitools.codegen.languages.JavaClientCodegen {

    @Override
    public String getName() {
        return "java-customization";
    }

    @Override
    public String getHelp() {
        return "功能保持与 'java' 一致，但定制化了部分代码生成策略.以满足特定需求";
    }

    @Override
    public String toOperationId(String operationId) {
        if (null != operationId && StringUtils.split(operationId, ".").length > 1) {
            //不止一个'.'，应该是包含了类路径，截取最后一个'.'后的文本作为方法名
            return super.toOperationId(StringUtils.substringAfterLast(operationId, "."));
        }
        return super.toOperationId(operationId);
    }

    @Override
    public String toModelName(String name) {
        if (null != name && StringUtils.split(name, ".").length > 1) {
            //不止一个'.'，应该是包含了类路径，截取最后一个'.'后的文本作为模型名称
            return super.toModelName(StringUtils.substringAfterLast(name, "."));
        }
        return super.toModelName(name);
    }
}
