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
package org.scleropages.core;

import com.google.common.collect.Maps;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class LookupComponentInterceptor implements MethodInterceptor, ApplicationContextAware, InitializingBean {

    /**
     * componentClass map to AbstractLookupComponent
     */
    private Map<Class, AbstractLookupComponent> lookupComponents;


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ComponentLookup annotation = AnnotationUtils.findAnnotation(invocation.getMethod(), ComponentLookup.class);
        if (null == annotation)
            annotation = AnnotationUtils.findAnnotation(invocation.getMethod().getDeclaringClass(), ComponentLookup.class);
        if (logger.isTraceEnabled())
            logger.trace("find method (or class) annotated [@ComponentLookup] from [{}]", invocation.getMethod());
        if (null == annotation) {
            if (logger.isDebugEnabled()) {
                logger.warn("can't find annotation @ComponentLookup on method(and class):", invocation.getMethod());
            }
            return invocation.proceed();
        }
        return proceedInLookupComponents(parseComponentLookupAnnotation(annotation, invocation), invocation);
    }


    protected Map<String, AbstractLookupComponent> parseComponentLookupAnnotation(ComponentLookup annotation, MethodInvocation invocation) throws ClassNotFoundException {

        Map<String, AbstractLookupComponent> currentLookupComponents = Maps.newHashMap();

        String componentKey = annotation.value();
        Class componentClass = annotation.componentType();
        AbstractLookupComponent lookupComponent = lookupComponents.get(componentClass);
        Assert.state(null != lookupComponent, "lookupComponent not found for component: " + componentClass + " on method " + invocation.getMethod() + " from @ComponentLookup defined.");
        currentLookupComponents.put(componentKey, lookupComponent);

        for (String additionalComponent : annotation.additionalComponents()) {
            StringTokenizer tokenizer = new StringTokenizer(additionalComponent, "=");
            Assert.state(tokenizer.countTokens() == 2, "invalid additionalComponents define[" + additionalComponent + "] on method: " + invocation.getMethod());
            String additionalComponentKey = tokenizer.nextToken();
            Class additionalComponentClass = ClassUtils.forName(tokenizer.nextToken(), getClass().getClassLoader());
            AbstractLookupComponent additionalLookupComponent = lookupComponents.get(additionalComponentClass);
            Assert.state(null != additionalLookupComponent, "additionalLookupComponent not found for component: " + additionalComponentClass + " on method " + invocation.getMethod() + " from @ComponentLookup defined.");
            currentLookupComponents.put(additionalComponentKey, additionalLookupComponent);
        }

        return currentLookupComponents;
    }

    protected Object proceedInLookupComponents(Map<String, AbstractLookupComponent> currentLookupComponents, MethodInvocation invocation) throws Throwable {
        currentLookupComponents.forEach((componentKey, abstractLookupComponent) -> {
            try {
                abstractLookupComponent.setCurrentLookupKey(componentKey);
                logger.debug("Setting {} current-key as: {}", abstractLookupComponent.getClass().getName(), componentKey);
            } catch (Exception e) {
                logger.warn(
                        "Detected a failure execution on method " + abstractLookupComponent.getClass().getName()
                                + ".setCurrentLookupKey(). Do not throws out any exception in sub classes of AbstractLookupComponent",
                        e);
            }
        });
        try {
            return invocation.proceed();
        } finally {
            currentLookupComponents.forEach((componentKey, abstractLookupComponent) -> {
                try {
                    abstractLookupComponent.resetCurrentLookupKey();
                } catch (Exception e) {
                    logger.warn(
                            "Detected a failure execution on method " + abstractLookupComponent.getClass().getName()
                                    + ".resetCurrentLookupKey(). Do not throws out any exception in sub classes of AbstractLookupComponent",
                            e);
                }
                logger.debug("Resetting {} current-key", abstractLookupComponent.getClass().getName());
            });
        }
    }

    public void setLookupComponents(List<AbstractLookupComponent> lookupComponents) {
        Assert.notEmpty(lookupComponents, "lookupComponents must not be empty.");
        this.lookupComponents = Maps.newHashMap();
        lookupComponents.forEach(abstractLookupComponent -> this.lookupComponents.put(abstractLookupComponent.getComponentClass(), abstractLookupComponent));
    }

    public void addLookupComponent(AbstractLookupComponent abstractLookupComponent) {
        if (this.lookupComponents == null)
            this.lookupComponents = Maps.newHashMap();
        this.lookupComponents.put(abstractLookupComponent.getComponentClass(), abstractLookupComponent);
    }


    private ApplicationContext applicationContext;

    private boolean autoLookupComponents = true;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (autoLookupComponents)
            applicationContext.getBeansOfType(AbstractLookupComponent.class).forEach((beanId, abstractLookupComponent) -> {
                addLookupComponent(abstractLookupComponent);
                logger.debug("auto-registered lookup component: [{}] as target: [{}]",
                        abstractLookupComponent.getClass().getName(), abstractLookupComponent.getComponentClass().getName());
            });
    }
}
