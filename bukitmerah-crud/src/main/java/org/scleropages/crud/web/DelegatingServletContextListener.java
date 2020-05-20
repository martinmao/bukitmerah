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
package org.scleropages.crud.web;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.OrderComparator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DelegatingServletContextListener implements ServletContextListener {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private List<ServletContextListener> servletContextListeners;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        for (ServletContextListener servletContextListener : findServletContextListener(sce)) {
            synchronized (servletContextListener) {
                servletContextListener.contextInitialized(sce);
            }

        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (ServletContextListener servletContextListener : findServletContextListener(sce)) {
            synchronized (servletContextListener) {
                servletContextListener.contextDestroyed(sce);
            }
        }
    }

    protected List<ServletContextListener> findServletContextListener(ServletContextEvent sce) {
        if (servletContextListeners == null) {
            WebApplicationContext webApplicationContext = WebApplicationContextUtils
                    .getWebApplicationContext(sce.getServletContext());
            Map<String, ServletContextListener> findedServletContextListeners = webApplicationContext
                    .getBeansOfType(ServletContextListener.class);
            servletContextListeners = Collections.unmodifiableList(Lists.newArrayList(findedServletContextListeners.values()));
            OrderComparator.sort(servletContextListeners);
            return servletContextListeners;
        }
        return servletContextListeners;

    }
}
