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

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.scleropages.crud.types.EntryList;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class EntryListSchemaResolver implements SchemaResolver {
    @Override
    public boolean supportInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        return ClassUtils.isAssignable(EntryList.class, javaType);
    }

    @Override
    public Schema resolveInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {

        Class ruleInterface = javaType;
        if (null != fieldPropertyDescriptor) {
            ruleInterface = fieldPropertyDescriptor.getPropertyField().getDeclaringClass();
        }
        if (null == ruleInterface && null != methodParameter) {
            ruleInterface = methodParameter.getDeclaringClass();
        }
        Schema schema = resolveContext.getSwaggerOpenApi().computeSchemaIfAbsent(javaType, ruleInterface, (cls1, cls2) -> {
            ObjectSchema entryListSchema = new ObjectSchema();
            ArraySchema entriesSchema = new ArraySchema();
            ObjectSchema entriesItemSchema = new ObjectSchema();
            entriesSchema.items(entriesItemSchema);
            entryListSchema.addProperties("items", entriesSchema);
            Map.Entry<Class, Class> genericTypeOfEntriesItemSchema = getGenericTypesOfEntriesItemSchema(methodParameter, fieldPropertyDescriptor);
            entriesItemSchema.addProperties("key", SchemaUtil.createSchema(genericTypeOfEntriesItemSchema.getKey(), resolveContext));
            entriesItemSchema.addProperties("value", SchemaUtil.createSchema(genericTypeOfEntriesItemSchema.getValue(), resolveContext));
            return entryListSchema;
        });
        schema.setName(javaType.getName() + ruleInterface.getSimpleName());
        return schema;
    }

    protected Map.Entry<Class, Class> getGenericTypesOfEntriesItemSchema(MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor) {
        Class keyType = String.class;
        Class valueType = String.class;
        ResolvableType resolvableType = null;
        if (null != fieldPropertyDescriptor) {
            resolvableType = fieldPropertyDescriptor.createResolvableType();
        }
        if (null != methodParameter) {
            resolvableType = ResolvableType.forMethodParameter(methodParameter);
        }
        if (null != resolvableType) {
            keyType = resolvableType.resolveGeneric(0);
            valueType = resolvableType.resolveGeneric(1);
        }
        Class finalKeyType = keyType;
        Class finalValueType = valueType;
        return new Map.Entry<Class, Class>() {
            @Override
            public Class getKey() {
                return finalKeyType;
            }

            @Override
            public Class getValue() {
                return finalValueType;
            }

            @Override
            public Class setValue(Class value) {
                throw new IllegalStateException();
            }
        };
    }

    @Override
    public void reset() {

    }
}
