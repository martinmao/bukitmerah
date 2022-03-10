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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 该实现提供了线程绑定的方式获取数据源的key，同时支持自动加载容器中配置的数据源，默认使用bean definition id作为Key.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class FrameworkRoutingDataSource extends AbstractRoutingDataSource implements ApplicationContextAware {


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final InheritableThreadLocal<Object> currentDataSourceKey =
            new InheritableThreadLocal<>();

    private boolean autoLookupDataSources = true;

    public static Object getCurrentDataSourceKey() {
        return currentDataSourceKey.get();
    }

    public static void setCurrentDataSourceKey(Object key) {
        currentDataSourceKey.set(key);
    }

    public static void clear() {
        currentDataSourceKey.remove();
    }

    private ApplicationContext applicationContext;

    @Override
    protected Object determineCurrentLookupKey() {
        return getCurrentDataSourceKey();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        setTargetDataSources(lookupDataSources());
        super.afterPropertiesSet();
    }

    private Map<Object, Object> detectedDataSources;

    private String defaultDataSourceBeanId;

    protected Map<Object, Object> lookupDataSources() {
        if (!autoLookupDataSources)
            return null;
        Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);
        Assert.notEmpty(dataSources, "no data-source found from spring context.");
        detectedDataSources = Maps.newHashMap();
        dataSources.forEach((s, dataSource) -> {
            if (!(dataSource instanceof FrameworkRoutingDataSource))
                detectedDataSources.put(s, dataSource);
        });
        logger.debug("successfully detected {} data-sources.....", detectedDataSources.keySet());

        Assert.hasText(defaultDataSourceBeanId, "defaultDataSourceBeanId must not be null.");
        Assert.isTrue(detectedDataSources.containsKey(defaultDataSourceBeanId), "no default data-source found by given id: " + defaultDataSourceBeanId);
        DataSource defaultDataSource = (DataSource) detectedDataSources.get(defaultDataSourceBeanId);
        setDefaultTargetDataSource(defaultDataSource);
        logger.debug("use {} as default data-source.", defaultDataSourceBeanId);
        return detectedDataSources;
    }

    public void setDefaultDataSourceBeanId(String defaultDataSourceBeanId) {
        this.defaultDataSourceBeanId = defaultDataSourceBeanId;
    }

    public void setAutoLookupDataSources(boolean autoLookupDataSources) {
        this.autoLookupDataSources = autoLookupDataSources;
    }
}
