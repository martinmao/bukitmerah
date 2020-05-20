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
package org.scleropages.connector.amqp;

import com.google.common.collect.Maps;
import com.rabbitmq.client.Channel;
import org.scleropages.serialize.SerializerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.Jackson2XmlMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@Configuration
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
public class RabbitMqConfiguration {

    @Value("#{ @environment['rabbitmq.serialize.default'] ?: 'serialize' }")
    private String defaultProvider;

    @ConditionalOnProperty(name = "rabbitmq.serialize.jackson-json.enabled")
    @ConditionalOnMissingBean
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        return jackson2JsonMessageConverter;
    }

    @ConditionalOnProperty(name = "rabbitmq.serialize.jackson-xml.enabled")
    @ConditionalOnMissingBean
    @Bean
    public Jackson2XmlMessageConverter jackson2XmlMessageConverter() {
        Jackson2XmlMessageConverter jackson2XmlMessageConverter = new Jackson2XmlMessageConverter();
        return jackson2XmlMessageConverter;
    }

    @ConditionalOnProperty(name = "rabbitmq.serialize.simple.enabled")
    @ConditionalOnMissingBean
    @Bean
    public SimpleMessageConverter simpleMessageConverter() {
        SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();
        return simpleMessageConverter;
    }

    @ConditionalOnProperty(name = "rabbitmq.serialize.serialize.enabled")
    @ConditionalOnMissingBean
    @Bean
    public SerializerMessageConverter serializerMessageConverter(SerializerFactory serializerFactory) {
        SerializerMessageConverter serializerMessageConverter = new SerializerMessageConverter(serializerFactory);
        return serializerMessageConverter;
    }

    private static final Map<String, String> SERIALIZER_PROVIDERS = Maps.newHashMap();

    private static final Map<String, String[]> PROVIDERS_CONTENT_TYPE = Maps.newHashMap();

    static {
        SERIALIZER_PROVIDERS.put("jackson-json", "jackson2JsonMessageConverter");
        SERIALIZER_PROVIDERS.put("jackson-xml", "jackson2XmlMessageConverter");
        SERIALIZER_PROVIDERS.put("simple", "simpleMessageConverter");
        SERIALIZER_PROVIDERS.put("serialize", "serializerMessageConverter");

        PROVIDERS_CONTENT_TYPE.put("jackson2JsonMessageConverter", new String[]{MessageProperties.CONTENT_TYPE_JSON});
        PROVIDERS_CONTENT_TYPE.put("jackson2XmlMessageConverter", new String[]{MessageProperties.CONTENT_TYPE_XML});
        PROVIDERS_CONTENT_TYPE.put("simpleMessageConverter", new String[]{MessageProperties.CONTENT_TYPE_TEXT_PLAIN, MessageProperties.CONTENT_TYPE_BYTES, MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT});
        PROVIDERS_CONTENT_TYPE.put("serializerMessageConverter", new String[]{SerializerMessageConverter.DEFAULT_CONTENT_TYPE});
    }

    @Primary
    @Bean
    @ConditionalOnMissingBean
    public ContentTypeDelegatingMessageConverter contentTypeDelegatingMessageConverter() {
        ContentTypeDelegatingMessageConverter messageConverter = new ContentTypeDelegatingMessageConverter();
        messageConverter.setConverterMappings(PROVIDERS_CONTENT_TYPE);
        String defaultConverterBeanId = SERIALIZER_PROVIDERS.get(defaultProvider);
        messageConverter.setDefaultConverterBeanName(defaultConverterBeanId);
        return messageConverter;
    }

    @Value("#{ @environment['rabbitmq.rpc.async-client.auto-startup'] ?: true }")
    private boolean asyncAmqpTemplateAutoStartup;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "rabbitmq.rpc.async-client.enabled")
    public AsyncRabbitTemplate asyncAmqpTemplate(RabbitTemplate template) {
        AsyncRabbitTemplate asyncAmqpTemplate = new AsyncRabbitTemplate(template);
        asyncAmqpTemplate.setAutoStartup(asyncAmqpTemplateAutoStartup);
        return asyncAmqpTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultRabbitMqClient rabbitMqClient(AmqpAdmin admin, AmqpTemplate template, MessageConverter messageConverter, AsyncRabbitTemplate asyncAmqpTemplate) {
        DefaultRabbitMqClient client = new DefaultRabbitMqClient((RabbitAdmin) admin, (RabbitTemplate) template, messageConverter);
        client.setAsyncAmqpTemplate(asyncAmqpTemplate);
        return client;
    }

    @ConfigurationProperties("cluster.amqp.message-properties")
    @Bean
    @ConditionalOnMissingBean
    public MessageProperties clusterMessageProperties() {
        return new MessageProperties();
    }
}
