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
package org.scleropages.crud.configure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.scleropages.core.mapper.JsonMapper2;
import org.scleropages.crud.FrameworkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@Import({BizExceptionTranslationConfiguration.class, DataSourceRoutingConfiguration.class, GenericLoggingConfiguration.class})
public class CrudFeaturesImporter implements ApplicationListener<ContextRefreshedEvent> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Method, String> defaultCacheNames = Maps.newConcurrentMap();

    @Bean
    public FrameworkContext frameworkContext() {
        return new FrameworkContext();
    }


    @Bean("defaultCacheResolver")
    public CacheResolver cacheResolver(ObjectProvider<CacheManager> cacheManagers) {
        CacheManager ifAvailable = cacheManagers.getIfAvailable();
        if (null != ifAvailable)
            return new SimpleCacheResolver(ifAvailable) {
                @Override
                protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
                    Collection<String> cacheNames = super.getCacheNames(context);
                    if (CollectionUtils.isEmpty(cacheNames)) {
                        String defaultCacheName = defaultCacheNames.computeIfAbsent(
                                context.getMethod(),
                                m -> (computeCacheName(context)));
                        return Lists.newArrayList(defaultCacheName);
                    }
                    return cacheNames;
                }
            };
        return new SimpleCacheResolver();
    }

    protected String computeCacheName(CacheOperationInvocationContext context) {
        return ClassUtils.getQualifiedMethodName(context.getMethod());
//      return AopUtils.getTargetClass(context.getTarget()).getName() + "#" + context.getMethod().getName();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        applicationContext.getBeanProvider(JacksonProperties.class).ifAvailable(jacksonProperties -> {
            applyJacksonConfigureToFastJson(jacksonProperties);
        });
    }

    private void applyJacksonConfigureToFastJson(JacksonProperties jacksonProperties) {
        logger.info("Detect JacksonProperties in spring context. Populating and apply to FastJSON...");
        List<Feature> parserFeatures = Lists.newArrayList();
        List<SerializerFeature> generateFeatures = Lists.newArrayList();
        Optional.ofNullable(jacksonProperties.getDateFormat()).ifPresent(s -> {
            generateFeatures.add(SerializerFeature.WriteDateUseDateFormat);
            JSON.DEFFAULT_DATE_FORMAT = jacksonProperties.getDateFormat();
            if (logger.isTraceEnabled())
                logger.trace("Applying date format: {}", jacksonProperties.getDateFormat());
        });
        Optional.ofNullable(jacksonProperties.getPropertyNamingStrategy()).ifPresent(s -> {
            PropertyNamingStrategy propertyNamingStrategy = null;
            if (Objects.equals(s, "com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy"))
                propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
            if (Objects.equals(s, "com.fasterxml.jackson.databind.PropertyNamingStrategy"))
                propertyNamingStrategy = PropertyNamingStrategy.CamelCase;
            if (Objects.equals(s, "com.fasterxml.jackson.databind.PropertyNamingStrategy.KebabCaseStrategy"))
                propertyNamingStrategy = PropertyNamingStrategy.KebabCase;
            if (Objects.equals(s, "com.fasterxml.jackson.databind.PropertyNamingStrategy.UpperCamelCaseStrategy"))
                propertyNamingStrategy = PropertyNamingStrategy.PascalCase;
            SerializeConfig.globalInstance.propertyNamingStrategy = propertyNamingStrategy;
            if (logger.isTraceEnabled())
                logger.trace("Applying property naming strategy: {}", s);
        });
        Optional.ofNullable(jacksonProperties.getSerialization()).ifPresent(serializationFeatureBooleanMap -> {
            if (serializationFeatureBooleanMap.getOrDefault(SerializationFeature.INDENT_OUTPUT, false))
                generateFeatures.add(SerializerFeature.PrettyFormat);
            if (logger.isTraceEnabled())
                logger.trace("Applying print pretty: {}", serializationFeatureBooleanMap.getOrDefault(SerializationFeature.INDENT_OUTPUT, false));
        });
        JsonMapper2.applyFeatureConfig(parserFeatures, generateFeatures);
    }
}
