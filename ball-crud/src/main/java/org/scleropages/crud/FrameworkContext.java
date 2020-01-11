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
package org.scleropages.crud;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Utility class used for obtain some global context object from JEE-framework
 * environment.
 */
@Component
public class FrameworkContext implements ApplicationContextAware {

    private static volatile ApplicationContext springContext = null;

    private static final int INITIALIZING = -1;
    private static final int READY = 0;
    private static final int ERROR = 1;


    private static final AtomicInteger readyFlag = new AtomicInteger(INITIALIZING);


    /**
     * find spring context.
     *
     * @return spring application context
     * @throws IllegalStateException If can't find spring context.
     */
    public static ApplicationContext getSpringApplicationContext() {
        if (null != springContext)
            return springContext;
        if (readyFlag.get() == ERROR)
            throw new IllegalStateException("Can't found spring context.");
        if (readyFlag.compareAndSet(INITIALIZING, READY)) {
            springContext = ContextLoader.getCurrentWebApplicationContext();
            if (springContext == null) {
                readyFlag.set(ERROR);
                throw new IllegalStateException("Can't found spring context.");
            }
        }
        return getSpringApplicationContext();
    }

    public static WebApplicationContext getSpringWebApplicationContext(ServletContext servletContext) {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
    }

    /**
     * find servlet context.
     *
     * @return servlet context
     * @throws IllegalStateException If can't find servlet context.
     */
    public static ServletContext getServletContext() {
        WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        if (null == ctx)
            throw new IllegalStateException("Can't found spring WebApplicationContext.");
        return ctx.getServletContext();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (null == applicationContext)
            throw new IllegalStateException("applicationContext must not be null.");
        if (readyFlag.compareAndSet(INITIALIZING, READY)) {
            springContext = applicationContext;
        }
    }

    public static <T> T getBean(String name) {
        return (T) getSpringApplicationContext().getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return getSpringApplicationContext().getBean(clazz);
    }

    public static FactoryBean getFactoryBean(String name) {
        return (FactoryBean) getSpringApplicationContext().getBean("&" + name);
    }

    public static <T> Map<String, T> getFactoryBeans(Class clazz) {
        return getFactoryBeans(getSpringApplicationContext(), clazz);
    }

    public static <T> Map<String, T> getFactoryBeans(ApplicationContext applicationContext, Class clazz) {
        String[] factoryBeanNames = applicationContext.getBeanNamesForType(clazz);
        Map<String, T> factoryBeans = new HashMap<>(factoryBeanNames.length);
        Stream.of(factoryBeanNames).forEach(s -> factoryBeans.put(s, (T) applicationContext.getBean("&" + s)));
        return factoryBeans;
    }

}
