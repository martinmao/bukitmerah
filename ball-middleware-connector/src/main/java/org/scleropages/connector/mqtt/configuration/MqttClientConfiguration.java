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
package org.scleropages.connector.mqtt.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.scleropages.connector.mqtt.DefaultMqttClient;
import org.scleropages.connector.mqtt.DefaultPahoClientFactory;
import org.scleropages.connector.mqtt.PahoClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@ConditionalOnProperty(name = "mqtt.client.enabled")
public class MqttClientConfiguration {

    @ConfigurationProperties("mqtt.client")
    @Bean
    @ConditionalOnMissingBean
    public MqttConnectOptions mqttConnectOptions() {
        return new MqttConnectOptions();
    }

    @Bean
    @ConditionalOnMissingBean
    public PahoClientFactory mqttPahoClientFactory() {
        DefaultPahoClientFactory pahoClientFactory = new DefaultPahoClientFactory();
        pahoClientFactory.setMqttConnectOptions(mqttConnectOptions());
        return pahoClientFactory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultMqttClient defaultMqttClient() {
        DefaultMqttClient mqttClient = new DefaultMqttClient(mqttPahoClientFactory());
        return mqttClient;
    }
}
