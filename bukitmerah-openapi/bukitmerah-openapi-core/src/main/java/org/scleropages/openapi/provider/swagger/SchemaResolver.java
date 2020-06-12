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
package org.scleropages.openapi.provider.swagger;

import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface SchemaResolver {

    static final String PROCESSING_FLAG_PREFIX = "PROCESSING_FLAG_PREFIX.";
    static final Boolean PROCESSING_FLAG = new Boolean(true);

    default boolean support(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        return (!Objects.equals(resolveContext.getAttribute(PROCESSING_FLAG_PREFIX + getClass().getSimpleName()), PROCESSING_FLAG)) && supportInternal(javaType, methodParameter, fieldPropertyDescriptor, resolveContext);
    }

    /**
     * return true if given class can be resolved.
     *
     * @param javaType
     * @param methodParameter
     * @param fieldPropertyDescriptor
     * @param resolveContext
     * @return
     */
    boolean supportInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext);


    default Schema resolve(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        String name = PROCESSING_FLAG_PREFIX + getClass().getSimpleName();
        resolveContext.setAttribute(name, PROCESSING_FLAG);
        try {
            Schema targetSchema = resolveInternal(javaType, methodParameter, fieldPropertyDescriptor, resolveContext);
            ObjectSchema schemaRef = new ObjectSchema();
            schemaRef.$ref(SchemaUtil.DEFAULT_SCHEMAS_PATH + targetSchema.getName());
            return schemaRef;
        } finally {
            Assert.isTrue(resolveContext.removeAttribute(name, PROCESSING_FLAG), "invalid state.");
        }
    }


    /**
     * resolved given class as {@link Schema}.
     * 注意尽返回schema ref，而不是原本的schema，即避免内联返回值 或内联属性. 否则 {@link org.openapitools.codegen.InlineModelResolver} 代码生成会产生难以理解的命名.
     *
     * @param javaType
     * @param methodParameter
     * @param fieldPropertyDescriptor
     * @param resolveContext
     * @return
     */
    Schema resolveInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext);

    /**
     * api method. reset states(defined sub classes.)
     */
    void reset();

}