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
        ObjectSchema entryListSchema = new ObjectSchema();
        ArraySchema entriesSchema = new ArraySchema();
        ObjectSchema entriesItemSchema = new ObjectSchema();
        entriesSchema.items(entriesItemSchema);
        entryListSchema.addProperties("items", entriesSchema);
        if (null != fieldPropertyDescriptor) {
            ResolvableType resolvableType = fieldPropertyDescriptor.createResolvableType();
            populateEntriesItemProperties(entriesItemSchema, resolvableType, resolveContext);
        }
        if (null != methodParameter) {
            ResolvableType resolvableType = ResolvableType.forMethodParameter(methodParameter);
            populateEntriesItemProperties(entriesItemSchema, resolvableType, resolveContext);
        }
        return entryListSchema;
    }

    protected void populateEntriesItemProperties(ObjectSchema entriesItemSchema, ResolvableType resolvableType, ResolveContext resolveContext) {
        Class<?> entryKeyType = resolvableType.resolveGeneric(0);
        Class<?> entryValueType = resolvableType.resolveGeneric(1);
        if (null != entryKeyType && null != entryValueType) {
            entriesItemSchema.addProperties("key", SchemaUtil.createSchema(entryKeyType, resolveContext));
            entriesItemSchema.addProperties("value", SchemaUtil.createSchema(entryValueType, resolveContext));
        }
    }

    @Override
    public void reset() {

    }
}
