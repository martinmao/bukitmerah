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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.scleropages.core.util.Namings;
import org.scleropages.crud.dao.orm.PageableBuilder;
import org.scleropages.crud.dao.orm.SearchFilter;
import org.scleropages.crud.exception.BizExceptionHttpView;
import org.scleropages.crud.web.WebSearchFilter;
import org.scleropages.openapi.OpenApi;
import org.scleropages.openapi.annotation.ApiIgnore;
import org.scleropages.openapi.annotation.ApiModel;
import org.scleropages.openapi.annotation.ApiParam;
import org.scleropages.openapi.provider.OpenApiReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 基于spring mvc 的 {@link OpenApiReader}实现（解析并生成 {@link OpenAPI}定义）.
 * 设计策略上，暴露全部注解了 {@link org.springframework.stereotype.Controller} 作为 {@link PathItem}.（前后端分离的开发模式中几乎所有接口都对外暴露).
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SpringMvcOpenApiReader implements OpenApiReader {


    private static final String SUCCESS_RESPONSE_STATUS = "200";
    private static final String BAD_REQUEST_RESPONSE_STATUS = "400";
    private static final String BIZ_STATE_VIOLATION_STATUS = "300";


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${openapi.title:[set your title via 'openapi.title']}")
    private String title;
    @Value("${openapi.desc:[set your description via 'openapi.desc']}")
    private String desc;
    @Value("${openapi.version:[set your version via 'openapi.version']}")
    private String version;
    @Value("${openapi.terms_of_service_url:[set your terms of service url via 'openapi.terms_of_service_url']}")
    private String termsOfServiceUrl;
    @Value("${openapi.contact_name:[set your contact name via 'openapi.contact_name']}")
    private String contactName;
    @Value("${openapi.contact_url:[set your contact url via 'openapi.contact_url']}")
    private String contactUrl;
    @Value("${openapi.contact_email:[set your contact email via 'openapi.contact_email']}")
    private String contactEmail;
    @Value("${openapi.license:[set your license via 'openapi.license']}")
    private String license;
    @Value("${openapi.license_url:[set your license url via 'openapi.license_url']}")
    private String licenseUrl;
    @Value("${openapi.gateway:[set your host via 'openapi.gateway']}")
    private String gateway;


    @Value("#{ @environment['openapi.render.format'] ?: 'yaml' }")
    private String openApiRenderFormat;

    @Value("#{ @environment['openapi.render.pretty'] ?: false }")
    private boolean openApiRenderPretty;

    @Value("#{ @environment['openapi.consume.media-type'] ?: 'application/json' }")
    private String defaultConsumeMediaType;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final List<SchemaResolver> schemaResolvers = Lists.newArrayList();

    private JacksonProperties jacksonProperties;

    @Override
    public OpenApi read(String basePackage, List<Class<?>> classes) {
        for (SchemaResolver schemaResolver : schemaResolvers) {
            schemaResolver.reset();
        }
        SwaggerOpenApi swaggerOpenApi = createOpenApi(basePackage, createOpenAPI(basePackage));

        classes.forEach(clazz -> {
            if (logger.isDebugEnabled())
                logger.debug("resolving class: {}", clazz.getSimpleName());
            if (!readableClass(clazz)) {
                logger.debug("ignore class: {} to resolve.", clazz.getSimpleName());
                return;
            }
            Tag tag = new Tag();
            tag.setName(clazz.getSimpleName());
            swaggerOpenApi.nativeOpenApi().addTagsItem(tag);
            Method[] methods = ReflectionUtils.getDeclaredMethods(clazz);

            Comparator comparator = ComparatorUtils.naturalComparator();
            Arrays.sort(methods, (o1, o2) -> {
                MergedAnnotation<RequestMapping> o1m = MergedAnnotations.from(o1).get(RequestMapping.class);
                MergedAnnotation<RequestMapping> o2m = MergedAnnotations.from(o2).get(RequestMapping.class);

                String path1 = o1m.isPresent() ? ArrayUtils.toString(o1m.getStringArray("path")) : o1.getName();
                String path2 = o2m.isPresent() ? ArrayUtils.toString(o2m.getStringArray("path")) : o2.getName();
                return comparator.compare(path2, path1);
            });
            for (Method method : methods) {
                if (ClassUtils.isUserLevelMethod(method)) {
                    if (!readableMethod(method)) {
                        logger.debug("ignore method: {} to resolve.", method.getName());
                        continue;
                    }
                    resolveControllerMethod(clazz, method, swaggerOpenApi);
                } else {
                    logger.debug("not an user level method: {} ignore to resolve.", method.getName());
                }
            }
        });
        resolveSchemas(swaggerOpenApi);
        return swaggerOpenApi;
    }


    /**
     * 解析controller method 并将解析到的内容直接应用的 {@link OpenAPI} 上下文，对子类提供部分扩展点在特定OAI对象创建后的后置处理(postXXXXCreation).
     *
     * @param controllerClass
     * @param controllerMethod
     */
    protected void resolveControllerMethod(final Class controllerClass, final Method controllerMethod, SwaggerOpenApi swaggerOpenApi) {

        MergedAnnotation<RequestMapping> baseMapping = MergedAnnotations.from(controllerClass).get(RequestMapping.class);
        MergedAnnotation<RequestMapping> methodMapping = MergedAnnotations.from(controllerMethod).get(RequestMapping.class);

        ResolveContext resolveContext = new ResolveContext(baseMapping, methodMapping, swaggerOpenApi, schemaResolvers);

        //compute and merge request path(controller path * method path)
        String[] paths = computePath(resolveContext);
        for (String path : paths) {// foreach path create path item....
            PathItem pathItem = swaggerOpenApi.nativeOpenApi().getPaths().computeIfAbsent(path, s -> new PathItem());
            try {
                postPathItemCreation(pathItem);
                Operation operation = createOperation(controllerMethod, resolveContext);
                operation.addTagsItem(controllerClass.getSimpleName());
                bindOperationToPathItem(pathItem, operation, resolveContext);
                resolveControllerMethodArguments(controllerMethod, resolveContext);
            } catch (Exception e) {
                logger.warn("failure to build operation for path: " + pathItem, e);
            }
        }
        if (logger.isDebugEnabled() && paths.length > 0) {
            logger.debug("add path:{} to {}.{}", ArrayUtils.toString(paths), controllerClass.getSimpleName(), controllerMethod.getName());
        }
    }


    protected void resolveControllerMethodArguments(final Method controllerMethod, ResolveContext resolveContext) {
        MethodParameter[] methodParameters = getMethodParameters(controllerMethod);
        for (int i = 0; i < methodParameters.length; i++) {
            resolveControllerMethodParameter(controllerMethod, methodParameters[i], resolveContext, i);
        }
    }

    private MethodParameter[] getMethodParameters(final Method controllerMethod) {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(controllerMethod);
        List<MethodParameter> methodParameters = Lists.newArrayList();
        int count = bridgedMethod.getParameterCount();
        for (int i = 0; i < count; i++) {
            MethodParameter methodParameter = new SynthesizingMethodParameter(controllerMethod, i);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);

            if (null != methodParameter.getParameterAnnotation(PathVariable.class)) {
                methodParameters.add(methodParameter);
                continue;
            }
            if (null != methodParameter.getParameterAnnotation(RequestParam.class)) {
                methodParameters.add(methodParameter);
                continue;
            }
            if (null != methodParameter.getParameterAnnotation(RequestBody.class)) {
                methodParameters.add(methodParameter);
                continue;
            }
            if (null != methodParameter.getParameterAnnotation(ApiModel.class)) {
                methodParameters.add(methodParameter);
                continue;
            }
            if (null != methodParameter.getParameterAnnotation(ApiParam.class)) {
                methodParameters.add(methodParameter);
                continue;
            }
            if (ClassUtils.isAssignable(Pageable.class, methodParameter.getParameterType())||ClassUtils.isAssignable(WebSearchFilter.class, methodParameter.getParameterType())) {
                methodParameters.add(methodParameter);
                continue;
            }
        }
        methodParameters.add(new SynthesizingMethodParameter(controllerMethod, -1));//方法返回值
        return methodParameters.toArray(new MethodParameter[methodParameters.size()]);
    }

    protected void resolveControllerMethodParameter(final Method controllerMethod, final MethodParameter methodParameter, ResolveContext resolveContext, int index) {
        Operation operation = resolveContext.getSwaggerOpenApi().getOperation(controllerMethod);
        PathVariable pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);
        if (null != pathVariable) {
            operation.addParametersItem(createPathParameter(pathVariable, methodParameter, resolveContext, index));
            return;
        }
        RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
        if (null != requestParam) {
            operation.addParametersItem(createQueryParameter(requestParam, methodParameter, resolveContext));
            return;
        }
        RequestBody requestBody = methodParameter.getParameterAnnotation(RequestBody.class);
        if (null != requestBody) {
            operation.setRequestBody(createRequestBody(requestBody, methodParameter, resolveContext));
            return;
        }
        //spring mvc 参数封装时，必须指定@ApiIgnore才可作为参数进行解析.
        ApiParam apiParam = methodParameter.getParameterAnnotation(ApiParam.class);
        if (null != apiParam) {
            ApiIgnore apiIgnore = methodParameter.getParameterAnnotation(ApiIgnore.class);
            Class<?> parameterType = methodParameter.getParameterType();
            SwaggerOpenApi swaggerOpenApi = resolveContext.getSwaggerOpenApi();
            SchemaUtil.createSchema(methodParameter, resolveContext);
            Schema schema = swaggerOpenApi.getSchema(parameterType,
                    null != apiIgnore && ArrayUtils.isNotEmpty(apiIgnore.value()) ? apiIgnore.value()[0] : parameterType);
            Map<String, Schema> properties = schema.getProperties();
            properties.forEach((name, propertySchema) -> {
                QueryParameter parameter = new QueryParameter();
                parameter.setRequired(false);
                parameter.setName(name);
                Schema parameterSchema = new Schema();
                parameterSchema.setName(propertySchema.getName());
                parameterSchema.setType(propertySchema.getType());
                parameterSchema.setFormat(propertySchema.getFormat());
                parameter.setSchema(parameterSchema);
                operation.addParametersItem(parameter);
                postParameterCreation(methodParameter, parameter, schema);
            });
        }
        if (ClassUtils.isAssignable(Pageable.class, methodParameter.getParameterType())) {
            createQueryParameter(PageableBuilder.PAGE_PREFIX + PageableBuilder.PAGE_NUMBER_PARAM_NAME, methodParameter, resolveContext, false);
            createQueryParameter(PageableBuilder.PAGE_PREFIX + PageableBuilder.PAGE_SIZE_PARAM_NAME, methodParameter, resolveContext, false);
        }
        if (ClassUtils.isAssignable(WebSearchFilter.class, methodParameter.getParameterType())) {
            createQueryParameter(SearchFilter.SearchFilterBuilder.SEARCH_PREFIX + "{expression}", methodParameter, resolveContext, false);
        }
        if (methodParameter.getParameterIndex() == -1) {
            operation.setResponses(createApiResponses(methodParameter, resolveContext));
        }
    }

    protected ApiResponses createApiResponses(MethodParameter methodParameter, ResolveContext resolveContext) {

        ApiResponses apiResponses = new ApiResponses();

        apiResponses.addApiResponse(SUCCESS_RESPONSE_STATUS, createResponse(methodParameter, resolveContext, () -> SchemaUtil.createSchema(methodParameter, resolveContext), "Successfully.", true));
        Supplier<Schema> errorSchemaSupplier = () -> SchemaUtil.createSchema(BizExceptionHttpView.class, resolveContext);
        apiResponses.addApiResponse(BAD_REQUEST_RESPONSE_STATUS, createResponse(methodParameter, resolveContext, errorSchemaSupplier, "Bad request.", false));
        apiResponses.addApiResponse(BIZ_STATE_VIOLATION_STATUS, createResponse(methodParameter, resolveContext, errorSchemaSupplier, "Biz state violation.", false));
        return apiResponses;
    }

    protected ApiResponse createResponse(MethodParameter methodParameter, ResolveContext resolveContext, Supplier<Schema> schemaSupplier, String desc, boolean checkVoid) {
        ApiResponse successResponse = new ApiResponse();
        Content content = new Content();
        successResponse.setContent(content);
        if (checkVoid) {
            if (!Objects.equals(methodParameter.getParameterType().getName(), "void")) {
                processContent(resolveContext.getMethodMappingProduces(), content, schemaSupplier.get());
            }
        } else {
            processContent(resolveContext.getMethodMappingProduces(), content, schemaSupplier.get());
        }
        successResponse.setDescription(desc);
        return successResponse;
    }

    protected io.swagger.v3.oas.models.parameters.RequestBody createRequestBody(RequestBody requestBody, MethodParameter methodParameter, ResolveContext resolveContext) {
        io.swagger.v3.oas.models.parameters.RequestBody swaggerBody = new io.swagger.v3.oas.models.parameters.RequestBody();
        swaggerBody.setRequired(requestBody.required());
        Content content = new Content();
        swaggerBody.setContent(content);
        Schema schema = SchemaUtil.createSchema(methodParameter, resolveContext);
        processContent(resolveContext.getMethodMappingConsumes(), content, schema);
        return swaggerBody;
    }

    protected void processContent(String[] mediaTypes, Content content, Schema schema) {
        if (ArrayUtils.isEmpty(mediaTypes)) {
            mediaTypes = new String[]{defaultConsumeMediaType};
        }
        for (String consume : mediaTypes) {
            MediaType mediaType = new MediaType();
            mediaType.setSchema(schema);
            content.addMediaType(consume, mediaType);
        }
    }

    protected Parameter createQueryParameter(RequestParam requestParam, MethodParameter methodParameter, ResolveContext resolveContext) {
        QueryParameter parameter = new QueryParameter();
        String parameterName = MergedAnnotations.from(requestParam).get(RequestParam.class).getString("name");
        postParameterCreationInternal(parameterName, methodParameter, parameter, requestParam.required(), resolveContext);
        return parameter;
    }

    protected Parameter createQueryParameter(String name, MethodParameter methodParameter, ResolveContext resolveContext, boolean required) {
        QueryParameter parameter = new QueryParameter();
        postParameterCreationInternal(name, methodParameter, parameter, required, resolveContext);
        return parameter;
    }


    protected Parameter createPathParameter(PathVariable pathVariable, MethodParameter methodParameter, ResolveContext resolveContext, int index) {
        PathParameter parameter = new PathParameter();
        String parameterName = MergedAnnotations.from(pathVariable).get(PathVariable.class).getString("name");
        if (StringUtils.isBlank(parameterName)) {
            String[] path = resolveContext.getMethodMappingPaths();
            if (ArrayUtils.isNotEmpty(path)) {
                String[] paths = StringUtils.substringsBetween(path[0], "{", "}");
                if (null != paths && paths.length >= index) {
                    parameterName = paths[index];
                }
            }
        }
        postParameterCreationInternal(parameterName, methodParameter, parameter, pathVariable.required(), resolveContext);
        return parameter;
    }

    protected final void postParameterCreationInternal(String parameterName, MethodParameter methodParameter, Parameter parameter, boolean required, ResolveContext resolveContext) {
        if (StringUtils.isBlank(parameterName))
            parameterName = methodParameter.getParameterName();
        if (StringUtils.isBlank(parameterName))
            parameterName = "arg" + methodParameter.getParameterIndex();
        parameter.setName(parameterName);
        parameter.setRequired(required);
        Schema schema = SchemaUtil.createSchema(methodParameter, resolveContext);
        parameter.setSchema(schema);
        postParameterCreation(methodParameter, parameter, schema);
    }

    protected final Operation createOperation(final Method controllerMethod, ResolveContext resolveContext) {
        SwaggerOpenApi swaggerOpenApi = resolveContext.getSwaggerOpenApi();
        return swaggerOpenApi.computeOperationIfAbsent(controllerMethod, method -> {
            Operation operation = new Operation();
            operation.setParameters(Lists.newArrayList());
            operation.setTags(Lists.newArrayList());
            String qualifiedMethodName = ClassUtils.getQualifiedMethodName(controllerMethod);
            Map<String, Method> operationIdToMethod = swaggerOpenApi.getOperationIdToMethod();
            if (operationIdToMethod.containsKey(qualifiedMethodName))
                qualifiedMethodName += controllerMethod.getParameterCount();
            if (operationIdToMethod.containsKey(qualifiedMethodName)) {
                qualifiedMethodName += ArrayUtils.toString(controllerMethod.getParameterTypes());
            }
            operation.setOperationId(qualifiedMethodName);
            operationIdToMethod.putIfAbsent(qualifiedMethodName, controllerMethod);
            postOperationCreation(operation);
            return operation;
        });
    }

    protected final void bindOperationToPathItem(PathItem pathItem, Operation operation, ResolveContext resolveContext) {
        RequestMethod[] httpMethods = resolveContext.getRequestMethods();
        for (RequestMethod httpMethod : httpMethods) {
            switch (httpMethod) {
                case GET:
                    pathItem.setGet(operation);
                    break;
                case POST:
                    pathItem.setPost(operation);
                    break;
                case PUT:
                    pathItem.setPut(operation);
                    break;
                case DELETE:
                    pathItem.setDelete(operation);
                    break;
                case PATCH:
                    pathItem.setPatch(operation);
                    break;
                case HEAD:
                    pathItem.setHead(operation);
                    break;
                case TRACE:
                    pathItem.setTrace(operation);
                    break;
                case OPTIONS:
                    pathItem.setOptions(operation);
                    break;
            }
        }
    }

    protected String[] computePath(ResolveContext resolveContext) {
        String[] basePaths = resolveContext.getBaseMappingPaths();
        String[] methodPaths = resolveContext.getMethodMappingPaths();
        int methodPathsLength = methodPaths.length;
        int totalPath = basePaths.length * (methodPathsLength > 0 ? methodPathsLength : 1);
        String[] paths = new String[totalPath];
        int i = 0;
        for (String basePath : basePaths) {
            if (basePath.charAt(0) != '/') {
                basePath = '/' + basePath;
            }
            if (methodPathsLength != 0 && basePath.charAt(basePath.length() - 1) != '/') {
                basePath += '/';
            }
            if (methodPathsLength == 0) {
                paths[i] = basePath;
            } else {
                for (String methodPath : methodPaths) {
                    methodPath = StringUtils.removeEnd(StringUtils.removeStart(methodPath, "/"), "/");
                    paths[i] = basePath + methodPath;
                    i++;
                }
            }
        }
        return paths;
    }


    protected boolean readableClass(Class clazz) {
        if (null != AnnotationUtils.findAnnotation(clazz, Hidden.class))
            return false;
        if (null != AnnotationUtils.findAnnotation(clazz, ApiIgnore.class))
            return false;
        return true;
    }

    protected boolean readableMethod(Method method) {
        if (null != AnnotationUtils.findAnnotation(method, Hidden.class))
            return false;
        ApiIgnore methodIgnore = AnnotationUtils.findAnnotation(method, ApiIgnore.class);
        //方法返回值策略如果为空，则忽略处理
        if (null != methodIgnore && ArrayUtils.isEmpty(methodIgnore.value())) {
            return false;
        }
        if (null == AnnotationUtils.findAnnotation(method, RequestMapping.class))
            return false;
        return true;
    }

    protected Info createApiInfo(String basePackage) {
        Info info = new Info();
        info.setTitle(this.title);
        String packageVersion = StringUtils.substringAfterLast(basePackage, ".");
        if (packageVersion.length() > 2
                && (packageVersion.indexOf(0) == 'v' || packageVersion.indexOf(0) == 'V')
                && NumberUtils.isCreatable(String.valueOf(packageVersion.indexOf(1)))) {
            info.setVersion(packageVersion);
        } else {
            info.setVersion(version);
        }
        info.setDescription(this.desc);
        info.setTermsOfService(this.termsOfServiceUrl);
        Contact contact = new Contact();
        contact.setName(this.contactName);
        contact.setEmail(this.contactEmail);
        contact.setUrl(this.contactUrl);
        info.setContact(contact);
        License license = new License();
        license.setName(this.license);
        license.setUrl(this.licenseUrl);
        info.setLicense(license);
        return info;
    }


    protected OpenAPI createOpenAPI(String basePackage) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(createApiInfo(basePackage));

        openAPI.setPaths(new Paths());
        Server server = new Server();
        server.setUrl(gateway);
        openAPI.addServersItem(server);
        return openAPI;
    }

    /*******************************************sub class callback methods.************************************************************/


    protected void postPathItemCreation(PathItem pathItem) {

    }

    protected void postOperationCreation(Operation operation) {
        operation.setSummary(StringUtils.substringAfterLast(operation.getOperationId(), "."));
    }

    protected void postParameterCreation(MethodParameter methodParameter, Parameter parameter, Schema schema) {

    }


    protected SwaggerOpenApi createOpenApi(String basePackage, OpenAPI openAPI) {
        SwaggerOpenApi swaggerOpenApi = new SwaggerOpenApi(basePackage, openAPI, openApiRenderFormat, openApiRenderPretty);
        return swaggerOpenApi;
    }


    protected void resolveSchemas(SwaggerOpenApi openAPI) {
        List<Schema> allSchemas = openAPI.getAllSchemas();
        Components components = new Components();
        allSchemas.forEach(schema -> {
            components.addSchemas(schema.getName(), schema);
            applySnakeCaseNamingStrategy(schema);
        });
        openAPI.nativeOpenApi().setComponents(components);
    }

    protected void applySnakeCaseNamingStrategy(Schema schema) {
        if (!useSnakeCaseName)
            return;
        Map<String, Schema> sourceProperties = schema.getProperties();
        if (MapUtils.isEmpty(sourceProperties))
            return;
        Map<String, Object> targetProperties = Maps.newHashMap();
        sourceProperties.forEach((propertyName, propertySchema) -> {
            targetProperties.put(Namings.snakeCaseName(propertyName), propertySchema);
            if (MapUtils.isNotEmpty(propertySchema.getProperties())) {
                applySnakeCaseNamingStrategy(propertySchema);
            }
        });
        schema.setProperties(targetProperties);
    }

    public void addSchemaResolver(SchemaResolver schemaResolver) {
        this.schemaResolvers.add(schemaResolver);
    }

    private boolean useSnakeCaseName;

    public void setJacksonProperties(JacksonProperties jacksonProperties) {
        if (StringUtils.endsWith(jacksonProperties.getPropertyNamingStrategy(), "SnakeCaseStrategy")) {
            this.useSnakeCaseName = true;
        }
        this.jacksonProperties = jacksonProperties;
    }
}
