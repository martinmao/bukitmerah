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
import io.swagger.v3.oas.models.media.StringSchema;
import org.scleropages.crud.types.NamedPrimitive;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class NamedPrimitiveSchemaResolver implements SchemaResolver {


    @Override
    public boolean supportInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        return ClassUtils.isAssignable(NamedPrimitive.class, javaType);
    }

    @Override
    public Schema resolveInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {

        Class valueType = null != methodParameter ? ResolvableType.forMethodParameter(methodParameter).resolveGeneric(0) : String.class;

        Class finalValueType = valueType != null ? valueType : String.class;

        Schema schema = resolveContext.getSwaggerOpenApi().computeSchemaIfAbsent(javaType, valueType, (cls1, cls2) -> {
            ObjectSchema namedPrimitive = new ObjectSchema();
            namedPrimitive.addProperties("name", new StringSchema());
            namedPrimitive.addProperties("value", SchemaUtil.createSchema(finalValueType, resolveContext));
            return namedPrimitive;
        });
        schema.setName(javaType.getName() + finalValueType.getSimpleName());
        return schema;
    }

    @Override
    public void reset() {

    }
}
