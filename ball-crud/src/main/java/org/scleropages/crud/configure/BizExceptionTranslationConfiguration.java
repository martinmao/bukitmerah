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

import org.hibernate.engine.spi.SessionImplementor;
import org.scleropages.crud.exception.BizException;
import org.scleropages.crud.exception.BizExceptionHttpView;
import org.scleropages.crud.exception.BizExceptionTranslationInterceptor;
import org.scleropages.crud.exception.BizExceptionTranslator;
import org.scleropages.crud.exception.DataIntegrityViolationExceptionTranslator;
import org.scleropages.crud.exception.ExceptionTranslationPostProcessor;
import org.scleropages.crud.exception.Jsr303ConstraintViolationTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.validation.executable.ExecutableValidator;
import java.util.List;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
public class BizExceptionTranslationConfiguration implements WebMvcConfigurer {

    private final Logger logger = LoggerFactory.getLogger(BizExceptionTranslator.class);

    @Value("#{ @environment['application.biz-exception.stack-tracing-enabled'] ?: false }")
    private boolean bizExceptionStackTracingEnabled;

    @Value("#{ @environment['application.biz-exception.view-media-type'] ?: 'application/json' }")
    private String bizExceptionHttpViewMediaType;

    private HttpMessageConverter httpMessageConverter;

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add((request, response, handler, ex) -> {
            if (ex instanceof BizException) {
                BizException bizException = (BizException) ex;
                BizExceptionHttpView exceptionView = new BizExceptionHttpView(bizException, response);
                exceptionView.render(null, httpMessageConverter);
                if (bizExceptionStackTracingEnabled) {
                    logger.error(ex.getMessage(), ex);
                }
                return new ModelAndView();//return empty view means errors handled.
            }
            return null;//return null means no errors handled.
        });
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.forEach(httpMessageConverter -> {
            if (httpMessageConverter.getSupportedMediaTypes().contains(MediaType.valueOf(bizExceptionHttpViewMediaType))) {
                this.httpMessageConverter = httpMessageConverter;
                return;
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public BizExceptionTranslationInterceptor exceptionTranslationInterceptor(ObjectProvider<BizExceptionTranslator> exceptionTranslators) {
        BizExceptionTranslationInterceptor exceptionTranslationInterceptor = new BizExceptionTranslationInterceptor();
        exceptionTranslators.orderedStream().forEach(exceptionTranslator -> exceptionTranslationInterceptor.getExceptionTranslators().add(exceptionTranslator));
        return exceptionTranslationInterceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionTranslationPostProcessor exceptionTranslationPostProcessor(Environment environment, BizExceptionTranslationInterceptor exceptionTranslationInterceptor) {
        ExceptionTranslationPostProcessor processor = new ExceptionTranslationPostProcessor();
        processor.setExceptionTranslationInterceptor(exceptionTranslationInterceptor);
        boolean proxyTargetClass = environment.getProperty("spring.aop.proxy-target-class", Boolean.class, true);
        processor.setProxyTargetClass(proxyTargetClass);
        return processor;
    }

    @Bean
    @ConditionalOnClass(ExecutableValidator.class)
    @ConditionalOnResource(resources = "classpath:META-INF/services/javax.validation.spi.ValidationProvider")
    public Jsr303ConstraintViolationTranslator jsr303ConstraintViolationTranslator() {
        return new Jsr303ConstraintViolationTranslator();
    }

    @Bean
    @ConditionalOnClass({LocalContainerEntityManagerFactoryBean.class, EntityManager.class, SessionImplementor.class})
    public DataIntegrityViolationExceptionTranslator dataIntegrityViolationExceptionTranslator(ObjectProvider<DataSource> dataSource) {
        return new DataIntegrityViolationExceptionTranslator(dataSource.getIfAvailable());
    }
}
