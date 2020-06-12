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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.collections.ComparatorUtils;
import org.scleropages.openapi.OpenApi;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SwaggerOpenApi implements OpenApi<OpenAPI> {

    private static final String OPENAPI_RENDER_FORMAT_YAML = "yaml";
    private static final String OPENAPI_RENDER_FORMAT_JSON = "json";

    private static final PrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    private static final PrettyPrinter nonePrettyPrinter = new MinimalPrettyPrinter();


    private final String basePackage;
    private final OpenAPI openAPI;
    private final String openApiRenderFormat;
    private final boolean openApiRenderPretty;

    private Map<Method, Operation> operations = Maps.newHashMap();

    private Map<String, Method> operationIdToMethod = Maps.newHashMap();

    /**
     * cache all schema definitions.
     * type class->rule interface class(User.class,User.Update.class)->schema
     */
    private final Map<Class, Map<String, Schema>> javaTypeToSchemas = Maps.newHashMap();

    public List<Schema> getAllSchemas() {
        List<Schema> schemas = Lists.newArrayList();
        javaTypeToSchemas.forEach((typeClazz, classSchemaMap) -> {
            classSchemaMap.forEach((ruleInterfaceClazz, schema) -> {
                schemas.add(schema);
            });
        });
        Collections.sort(schemas, (o1, o2) -> ComparatorUtils.naturalComparator().compare(o1.getName(), o2.getName()));
        return Collections.unmodifiableList(schemas);
    }

    public Schema getSchema(Class javaType, Class ruleType) {
        return javaTypeToSchemas.get(javaType).get(ruleType.getName());
    }

    public SwaggerOpenApi(String basePackage, OpenAPI openAPI, String openApiRenderFormat, boolean openApiRenderPretty) {
        this.basePackage = basePackage;
        this.openAPI = openAPI;
        this.openApiRenderFormat = openApiRenderFormat;
        this.openApiRenderPretty = openApiRenderPretty;
    }

    @Override
    public String openapi() {
        return openAPI.getOpenapi();
    }

    @Override
    public String id() {
        return basePackage;
    }

    @Override
    public OpenAPI nativeOpenApi() {
        return openAPI;
    }

    @Override
    public String render() {
        ObjectMapper mapper = null;
        if (OPENAPI_RENDER_FORMAT_YAML.equalsIgnoreCase(openApiRenderFormat)) {
            mapper = Yaml.mapper();
        } else if (OPENAPI_RENDER_FORMAT_JSON.equalsIgnoreCase(openApiRenderFormat)) {
            mapper = Json.mapper();
        }
        if (null != mapper) {
            try {
                return mapper.writer(openApiRenderPretty ? prettyPrinter : nonePrettyPrinter).writeValueAsString(openAPI);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("failure to render open api for: " + id(), e);
            }
        } else {
            return openAPI.toString();
        }
    }

    public Operation getOperation(Method method) {

        return operations.get(method);
    }

    public Operation computeOperationIfAbsent(Method method, Function<Method, Operation> mappingFunction) {
        return operations.computeIfAbsent(method, mappingFunction);
    }

    public Schema computeSchemaIfAbsent(Class typeClass, Class ruleInterface, BiFunction<Class, Class, ? extends Schema> schemaLoader) {
        return javaTypeToSchemas.computeIfAbsent(typeClass, clazz -> Maps.newHashMap()).computeIfAbsent(ruleInterface.getName(), clazz -> schemaLoader.apply(typeClass, ruleInterface));
    }

    public Schema computeSchemaIfAbsent(Class typeClass, String id, BiFunction<Class, String, ? extends Schema> schemaLoader) {
        return javaTypeToSchemas.computeIfAbsent(typeClass, clazz -> Maps.newHashMap()).computeIfAbsent(id, clazz -> schemaLoader.apply(typeClass, id));
    }


    public Map<String, Method> getOperationIdToMethod() {
        return operationIdToMethod;
    }
}
