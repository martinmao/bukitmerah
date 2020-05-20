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
package org.scleropages.crud.dao.jdbc;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class RoutingDataSourceInterceptor implements MethodInterceptor {


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        DataSourceRouting annotation = AnnotationUtils.findAnnotation(invocation.getMethod(), DataSourceRouting.class);
        if (null == annotation)
            annotation = AnnotationUtils.findAnnotation(invocation.getMethod().getDeclaringClass(), DataSourceRouting.class);
        if (logger.isTraceEnabled())
            logger.trace("find method (or class) annotated [@DataSourceRouting] from [{}]", invocation.getMethod());
        String dataSourceKey = null != annotation ? annotation.value() : null;
        FrameworkRoutingDataSource.setCurrentDataSourceKey(dataSourceKey);
        logger.debug("Setting data-source key as: [{}]", dataSourceKey);
        try {
            return invocation.proceed();
        } finally {
            FrameworkRoutingDataSource.clear();
            logger.debug("Resetting data-source key from: [{}]", dataSourceKey);
        }
    }
}
