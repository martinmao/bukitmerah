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

import org.scleropages.openapi.provider.OpenApiScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class BeanComponentApiScanner implements OpenApiScanner, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware, InitializingBean {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BeanComponentApiScanner.class);

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader beanClassLoader;

    private List<TypeFilter> includeFilters;

    @Override
    public List<Class<?>> scan(String basePackage) {
        List<Class<?>> candidateClasses = Lists.newArrayList();
        ClassPathScanningCandidateComponentProvider scanner = createScanner();
        int candidateClassSize = candidateClasses.size();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("scanning open api from package: {}.current size: {}", basePackage, candidateClassSize);
        }
        scanner.findCandidateComponents(basePackage).forEach(candidateComponent -> {
            try {
                Class<?> candidateClass = ClassUtils.forName(candidateComponent.getBeanClassName(), beanClassLoader);
                candidateClasses.add(candidateClass);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("failure load open-api candidate class from: " + candidateComponent.getBeanClassName(), e);
            }
        });
        if (LOGGER.isTraceEnabled())
            LOGGER.trace("scanned [{}] open-api classes: {} from package: {}", candidateClasses.size(), ClassUtils.classNamesToString(candidateClasses), basePackage);
        return candidateClasses;
    }


    protected ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider classPathScanningCandidateComponentProvider = new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(
                    AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
        classPathScanningCandidateComponentProvider.setResourceLoader(resourceLoader);
        includeFilters.forEach(typeFilter -> classPathScanningCandidateComponentProvider.addIncludeFilter(typeFilter));
        return classPathScanningCandidateComponentProvider;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    public void setIncludeFilters(List<TypeFilter> includeFilters) {
        this.includeFilters = includeFilters;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(includeFilters, "include filters must not empty.");
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
}
