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
package org.scleropages.crud.dao.orm.jpa.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class GenericHibernateCustomizer implements HibernatePropertiesCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericHibernateCustomizer.class);

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.putIfAbsent("hibernate.session_factory.interceptor", GenericHibernateInterceptor.class.getName());
        LOGGER.info("hibernate.session_factory.interceptor->[{}]", GenericHibernateInterceptor.class.getName());
    }
}
