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
package org.scleropages.openapi.autoconfigure;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.scleropages.openapi.OpenApiContextBuilder;
import org.scleropages.openapi.provider.swagger.BeanComponentApiScanner;
import org.scleropages.openapi.provider.swagger.EntryListSchemaResolver;
import org.scleropages.openapi.provider.swagger.NamedPrimitiveSchemaResolver;
import org.scleropages.openapi.provider.swagger.PageSchemaResolver;
import org.scleropages.openapi.provider.swagger.SchemaResolver;
import org.scleropages.openapi.provider.swagger.SpringMvcOpenApiReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@ConditionalOnProperty(name = "openapi.base-packages")
@ComponentScan(basePackages = {"org.scleropages.openapi.web"})
@AutoConfigureAfter({WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class})
public class SpringMvcOpenApiAutoConfigure implements ApplicationListener<ContextRefreshedEvent>, WebMvcConfigurer {

    @Value("#{ @environment['openapi.base-packages'] ?: 'no_package_provided' }")
    private String basePackages;

    @Value("#{ @environment['openapi.build-on-startup'] ?: true }")
    private boolean buildOpenApiOnStartup;

    @Bean
    @ConditionalOnMissingBean
    public BeanComponentApiScanner beanComponentApiScanner() {
        BeanComponentApiScanner beanComponentApiScanner = new BeanComponentApiScanner();
        beanComponentApiScanner.setIncludeFilters(Lists.newArrayList(new AnnotationTypeFilter(Controller.class)));
        return beanComponentApiScanner;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringMvcOpenApiReader springMvcOpenApiReader(ObjectProvider<SchemaResolver> schemaResolvers, JacksonProperties jacksonProperties) {
        SpringMvcOpenApiReader springMvcOpenApiReader = new SpringMvcOpenApiReader();
        schemaResolvers.orderedStream().forEachOrdered(schemaResolver -> springMvcOpenApiReader.addSchemaResolver(schemaResolver));
        springMvcOpenApiReader.setJacksonProperties(jacksonProperties);
        return springMvcOpenApiReader;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenApiContextBuilder openApiContextBuilder(SpringMvcOpenApiReader springMvcOpenApiReader) {
        OpenApiContextBuilder builder = new OpenApiContextBuilder(beanComponentApiScanner(), springMvcOpenApiReader, StringUtils.split(basePackages, ","));
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean
    public PageSchemaResolver pageSchemaResolver() {
        return new PageSchemaResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public EntryListSchemaResolver entryListSchemaResolver() {
        return new EntryListSchemaResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamedPrimitiveSchemaResolver namedPrimitiveSchemaResolver() {
        return new NamedPrimitiveSchemaResolver();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (buildOpenApiOnStartup)
            event.getApplicationContext().getBean(OpenApiContextBuilder.class).build();
    }


}
