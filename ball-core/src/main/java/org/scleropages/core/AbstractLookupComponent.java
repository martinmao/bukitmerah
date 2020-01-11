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
import org.scleropages.core.util.GenericTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * referenced from {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource}.
 * for any component. and find one from multiple components by {@link #determineCurrentLookupKey()},
 * {@link #setCurrentLookupKey(Object)}, {@link #resetCurrentLookupKey()}.
 * sub classes must make sure implementation these methods with Ad-hoc (such as use jdk {@link ThreadLocal}).
 * <pre>
 * impl example:
 *
 *      private static final ThreadLocal currentComponentKey =new ThreadLocal();
 *
 *      public void setCurrentLookupKey(K key){
 *          currentDataSourceKey.set(key);
 *      }
 *
 *      public void resetCurrentLookupKey(){
 *          currentComponentKey.remove();
 *      }
 *
 *      protected Object determineCurrentLookupKey(){
 *          return currentComponentKey.get();
 *      }
 * </pre>
 *
 * <pre>
 * use example:
 *
 *         subLookupComponent.setCurrentLookupKey(dataSourceKey);
 *         try {
 *             do sometings.....
 *         } finally {
 *             subLookupComponent.resetCurrentLookupKey();
 *         }
 *
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class AbstractLookupComponent<K, V> implements /*FactoryBean,*/ InitializingBean, ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Object, Object> targetComponents;

    private V defaultComponent;

    private boolean lenientFallback = true;

    private Map<K, V> resolvedComponents;

    private V resolvedDefaultComponent;

    private Class keyClass;
    private Class componentClass;

    private boolean autoLookupComponents = true;

    public AbstractLookupComponent() {
        this.keyClass = GenericTypes.getClassGenericType(getClass(), AbstractLookupComponent.class, 0);
        this.componentClass = GenericTypes.getClassGenericType(getClass(), AbstractLookupComponent.class, 1);
        if (logger.isDebugEnabled() && null != keyClass && null != componentClass) {
            logger.warn("Can't determine Generic Type of class: {}.If you see this message. you must make sure implementation getKeyClass() and getComponentClass() method.", getClass().getName());
        }
    }

    public void setTargetComponents(Map<Object, Object> targetComponents) {
        this.targetComponents = targetComponents;
    }

    public void setDefaultComponent(V defaultComponent) {
        this.defaultComponent = defaultComponent;
    }


    /**
     * 是否宽大处理，找不到合适的组件则返回默认的组件，默认true
     *
     * @param lenientFallback
     */
    public void setLenientFallback(boolean lenientFallback) {
        this.lenientFallback = lenientFallback;
    }


    @Override
    public void afterPropertiesSet() {
        autoLookupComponentsIfNecessary();
        if (this.targetComponents == null) {
            throw new IllegalArgumentException("Property 'targetComponents' is required");
        }
        this.resolvedComponents = createResolvedComponents();
        this.targetComponents.forEach((key, value) -> {
            K lookupKey = resolveSpecifiedLookupKey(key);
            V component = resolveSpecifiedComponent(value);
            this.resolvedComponents.put(lookupKey, component);
        });
        if (this.defaultComponent != null) {
            this.resolvedDefaultComponent = resolveSpecifiedComponent(this.defaultComponent);
        }
    }

    protected void autoLookupComponentsIfNecessary() {
        if (!autoLookupComponents)
            return;
        Map<String, V> beansOfType = applicationContext.getBeansOfType(getComponentClass());
        if (null == targetComponents)
            targetComponents = Maps.newHashMap();
        beansOfType.forEach((beanId, bean) -> {
            if (!ClassUtils.isAssignableValue(getClass(), bean)) {//except for self
                String componentId = obtainId(beanId, bean);
                targetComponents.put(componentId, bean);
                logger.debug("auto-lookup component [{}] and registered to [{}: {}]", bean.getClass().getName(), componentId, getClass().getName());
            }
        });
    }

    protected String obtainId(String beanId, Object bean) {
        return bean instanceof ComponentId ? ((ComponentId) bean).id() : beanId;
    }

    protected K resolveSpecifiedLookupKey(Object lookupKey) {
        if (ClassUtils.isAssignableValue(getKeyClass(), lookupKey)) {
            return (K) lookupKey;
        }
        throw new IllegalArgumentException(
                "Illegal component key - only [" + getKeyClass().getName() + "] supported.");
    }


    protected V resolveSpecifiedComponent(Object component) throws IllegalArgumentException {
        if (ClassUtils.isAssignableValue(getComponentClass(), component)) {
            logger.debug("lookup component [{}] and registered to target-components", component.getClass().getName());
            return (V) component;
        } else if (component instanceof String) {
            Object bean = applicationContext.getBean((String) component);
            if (ClassUtils.isAssignableValue(getComponentClass(), bean)) {
                logger.debug("lookup component [{}] and registered to target-components", bean.getClass().getName());
                return (V) bean;
            }
            throw new IllegalArgumentException("Illegal component [" + bean.getClass().getName() + "] - not instance of [" + getComponentClass().getName() + "].");
        } else {
            throw new IllegalArgumentException(
                    "Illegal component - only [" + getComponentClass().getName() + "] and String supported: " + component);
        }
    }


    protected V determineTargetComponent() {
        Assert.notNull(this.resolvedComponents, " components not initialized");
        Object lookupKey = determineCurrentLookupKey();
        V component = this.resolvedComponents.get(lookupKey);
        if (component == null && (this.lenientFallback || lookupKey == null)) {
            component = this.resolvedDefaultComponent;
        }
        if (component == null) {
            throw new IllegalStateException("Cannot determine target component[" + getComponentClass().getName() + "] for lookup key [" + lookupKey + "]");
        }
        return component;
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    protected abstract K determineCurrentLookupKey();

    public abstract void setCurrentLookupKey(K key);

    public abstract void resetCurrentLookupKey();

    public Class<K> getKeyClass() {
        return keyClass;
    }

    public Class<V> getComponentClass() {
        return componentClass;
    }

    protected Map<K, V> createResolvedComponents() {
        return new HashMap<>(targetComponents.size());
    }

    public void setAutoLookupComponents(boolean autoLookupComponents) {
        this.autoLookupComponents = autoLookupComponents;
    }

//    @Override
//    public V getObject() throws Exception {
//        return (V) this;
//    }
//
//    @Override
//    public Class<?> getObjectType() {
//        return getComponentClass();
//    }
}
