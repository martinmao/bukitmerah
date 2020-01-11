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

import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ComponentLookupSourceAdvisor extends StaticMethodMatcherPointcutAdvisor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());


    public ComponentLookupSourceAdvisor(Advice advice) {
        super(advice);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        boolean matches = !ClassUtils.isCglibProxyClass(targetClass) && isAnnotationOnClassOrMethod(method, targetClass);
        if (logger.isTraceEnabled() && matches)
            logger.trace("Adding join-point to [{}] for @ComponentLookup", method);
        return matches;
    }

    private boolean isAnnotationOnClassOrMethod(Method method, Class targetClass) {
        return AnnotationUtils.findAnnotation(targetClass, ComponentLookup.class) != null
                || AnnotationUtils.findAnnotation(AopUtils.getMostSpecificMethod(method, targetClass), ComponentLookup.class) != null;
    }
}
