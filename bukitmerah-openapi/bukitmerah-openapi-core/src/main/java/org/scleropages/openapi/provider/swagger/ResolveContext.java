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

import com.google.common.collect.Maps;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ResolveContext {

    private static final String ANNOTATION_REQUEST_MAPPING_METHOD_PATH = "path";
    private static final String ANNOTATION_REQUEST_MAPPING_METHOD_METHOD = "method";
    private static final String ANNOTATION_REQUEST_MAPPING_METHOD_PRODUCES = "produces";
    private static final String ANNOTATION_REQUEST_MAPPING_METHOD_CONSUMES = "consumes";


    private final MergedAnnotation<RequestMapping> baseMapping;
    private final MergedAnnotation<RequestMapping> methodMapping;
    private final SwaggerOpenApi swaggerOpenApi;
    private final List<SchemaResolver> schemaResolvers;
    private final Map<String, Object> attributes = Maps.newHashMap();

    public ResolveContext(MergedAnnotation<RequestMapping> baseMapping, MergedAnnotation<RequestMapping> methodMapping, SwaggerOpenApi swaggerOpenApi, List<SchemaResolver> schemaResolvers) {
        this.baseMapping = baseMapping;
        this.methodMapping = methodMapping;
        this.swaggerOpenApi = swaggerOpenApi;
        this.schemaResolvers = Collections.unmodifiableList(schemaResolvers);
    }

    public SwaggerOpenApi getSwaggerOpenApi() {
        return swaggerOpenApi;
    }


    public String[] getBaseMappingPaths() {
        return baseMapping.getStringArray(ANNOTATION_REQUEST_MAPPING_METHOD_PATH);
    }

    public String[] getMethodMappingPaths() {
        return methodMapping.getStringArray(ANNOTATION_REQUEST_MAPPING_METHOD_PATH);
    }

    public String[] getMethodMappingProduces() {
        return methodMapping.getStringArray(ANNOTATION_REQUEST_MAPPING_METHOD_PRODUCES);
    }

    public String[] getMethodMappingConsumes() {
        return methodMapping.getStringArray(ANNOTATION_REQUEST_MAPPING_METHOD_CONSUMES);
    }

    public RequestMethod[] getRequestMethods() {
        return methodMapping.getEnumArray(ANNOTATION_REQUEST_MAPPING_METHOD_METHOD, RequestMethod.class);
    }

    public List<SchemaResolver> getSchemaResolvers() {
        return schemaResolvers;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean removeAttribute(String name, Object exceptValue) {
        return attributes.remove(name, exceptValue);
    }
}
