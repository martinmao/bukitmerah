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
package org.scleropages.crud.dao.orm.jpa;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class PageQueryPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor implements InitializingBean {
    
    @Override
    public void afterPropertiesSet() throws Exception {
        Pointcut pointcut = new AnnotationMatchingPointcut(Service.class, true);
        this.advisor = new DefaultPointcutAdvisor(pointcut, new PageQueryInterceptor());
    }
}
