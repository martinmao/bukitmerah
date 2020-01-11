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
package org.scleropages.connector.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DefaultPahoClientFactory implements PahoClientFactory, InitializingBean, ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MqttConnectOptions mqttConnectOptions;

    private MqttClientPersistence mqttClientPersistence;



    @Override
    public IMqttClient newClient(String url, String clientId) throws MqttException {
        return new org.eclipse.paho.client.mqttv3.MqttClient(url == null ? "tcp://NO_URL_PROVIDED" : url, clientId, this.mqttClientPersistence);
    }

    @Override
    public IMqttAsyncClient newAsyncClient(String url, String clientId) throws MqttException {
        return new MqttAsyncClient(url == null ? "tcp://NO_URL_PROVIDED" : url, clientId, this.mqttClientPersistence);
    }

    @Override
    public MqttConnectOptions getConnectionOptions() {
        return mqttConnectOptions;
    }

    public void setMqttClientPersistence(MqttClientPersistence mqttClientPersistence) {
        this.mqttClientPersistence = mqttClientPersistence;
    }

    public void setMqttConnectOptions(MqttConnectOptions mqttConnectOptions) {
        this.mqttConnectOptions = mqttConnectOptions;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(mqttConnectOptions, "mqttConnectOptions not provided.");
        if (null == mqttClientPersistence) {
            logger.debug("no mqttClientPersistence set. find from spring context...");
            try {
                applicationContext.getBean(mqttClientPersistence.getClass());
            } catch (Exception e) {
                logger.debug("failure to find mqttClientPersistence from spring context. cause:", e.getMessage());
            }
        }
        if (null == mqttClientPersistence) {
            mqttClientPersistence = new MemoryPersistence();
            logger.debug("use MemoryPersistence as mqttClientPersistence default implementation.");
        }
    }


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
