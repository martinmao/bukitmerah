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
        Class<?> contentType = getPageContentType(methodParameter, fieldPropertyDescriptor);
        Schema contentSchema = SchemaUtil.createSchema(contentType, methodParameter, resolveContext);
        Schema pageSchema = resolveContext.getSwaggerOpenApi().getSchema(Page.class, Page.class);


        return resolveContext.getSwaggerOpenApi().computeSchemaIfAbsent(Page.class, contentType, (cls1, cls2) -> {
            Schema pageContentSchema = new Schema();
            pageSchema.getProperties().forEach((k, v) -> pageContentSchema.addProperties((String) k, (Schema) v));
            ArraySchema contentArray = new ArraySchema();
            pageContentSchema.addProperties("content", contentArray);
            pageContentSchema.setName(contentType.getPackage().getName()+"." + Page.class.getSimpleName() + contentType.getSimpleName());
            contentArray.setItems(contentSchema);
            return pageContentSchema;
        });
    }

    protected Class getPageContentType(MethodParameter methodParameter, FieldPropertyDescriptor fieldPropertyDescriptor) {
        Field propertyField = null != fieldPropertyDescriptor ? fieldPropertyDescriptor.getPropertyField() : null;
        if (null != propertyField) {//page 作为对象属性.
            return SchemaUtil.getPropertyConcreteType(new FieldPropertyDescriptor(null, propertyField));
        }
        if (null != methodParameter && methodParameter.getParameterIndex() == -1) {//page作为返回值
            return SchemaUtil.getParameterConcreteType(methodParameter, ResolvableType.forMethodParameter(methodParameter).resolveGeneric(0));
        }
        return Object.class;
    }

    @Override
    public void reset() {
        pageSchemaCreated.set(false);
    }
}
