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
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * support for spring data {@link Page} as schema. it's will also resolve {@link Page#getContent()} as schema.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class PageSchemaResolver implements SchemaResolver {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean pageSchemaCreated = new AtomicBoolean(false);


    @Override
    public boolean supportInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        return ClassUtils.isAssignable(Page.class, javaType);
    }

    @Override
    public Schema resolveInternal(Class javaType, MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor, ResolveContext resolveContext) {
        if (pageSchemaCreated.compareAndSet(false, true)) {
            SchemaUtil.createSchema(javaType, resolveContext);
        }
        Schema copedSchema = new Schema();
        Schema sourceSchema = resolveContext.getSwaggerOpenApi().getSchema(Page.class, Page.class);
        sourceSchema.getProperties().forEach((k, v) -> copedSchema.addProperties((String) k, (Schema) v));
        ArraySchema contentArray = new ArraySchema();
        copedSchema.addProperties("content", contentArray);
        Class<?> contentType = null;
        Field propertyField = null != fieldPropertyDescriptor ? fieldPropertyDescriptor.getPropertyField() : null;
        if (null != propertyField) {//page 作为对象属性.
            contentType = SchemaUtil.getPropertyConcreteType(new FieldPropertyDescriptor(null, propertyField));
        }
        if (null != methodParameter && methodParameter.getParameterIndex() == -1 && contentType == null) {//page作为返回值
            contentType = SchemaUtil.getParameterConcreteType(methodParameter, ResolvableType.forMethodParameter(methodParameter).resolveGeneric(0));
        }
        Schema contentSchema = SchemaUtil.createSchema(SchemaUtil.getParameterConcreteType(methodParameter, contentType), methodParameter, resolveContext);
        copedSchema.setName(Page.class.getName() + contentType.getSimpleName());
        contentArray.setItems(contentSchema);
        return copedSchema;
    }

    @Override
    public void reset() {
        pageSchemaCreated.set(false);
    }
}
