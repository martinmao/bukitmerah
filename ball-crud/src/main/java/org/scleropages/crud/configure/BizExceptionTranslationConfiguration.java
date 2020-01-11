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

import org.scleropages.crud.exception.ExceptionTranslationInterceptor;
import org.scleropages.crud.exception.ExceptionTranslationPostProcessor;
import org.scleropages.crud.exception.ExceptionTranslator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
public class BizExceptionTranslationConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public ExceptionTranslationInterceptor exceptionTranslationInterceptor(ObjectProvider<ExceptionTranslator> exceptionTranslators) {
        ExceptionTranslationInterceptor exceptionTranslationInterceptor = new ExceptionTranslationInterceptor();
        exceptionTranslators.orderedStream().forEach(exceptionTranslator -> exceptionTranslationInterceptor.getExceptionTranslators().add(exceptionTranslator));
        return exceptionTranslationInterceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionTranslationPostProcessor exceptionTranslationPostProcessor(Environment environment, ExceptionTranslationInterceptor exceptionTranslationInterceptor) {
        ExceptionTranslationPostProcessor processor = new ExceptionTranslationPostProcessor();
        processor.setExceptionTranslationInterceptor(exceptionTranslationInterceptor);
        boolean proxyTargetClass = environment.getProperty("spring.aop.proxy-target-class", Boolean.class, true);
        processor.setProxyTargetClass(proxyTargetClass);
        return processor;
    }
}
