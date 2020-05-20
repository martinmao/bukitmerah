/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scleropages.connector.amqp;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A composite {@link MessageConverter} that delegates to an actual {@link MessageConverter}
 * based on the contentType header. Supports a default converter when no content type matches.
 * Note: the {@link MessageProperties} requires a content type header to select a converter
 * when used for outbound conversion, but the converter will (generally) override it to match
 * the actual conversion.
 *
 * @author Eric Rizzo
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.4.2
 */
public class ContentTypeDelegatingMessageConverter implements MessageConverter, ApplicationContextAware, InitializingBean {


    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, MessageConverter> delegates = new HashMap();

    private MessageConverter defaultConverter;


    public void setDelegates(Map<String, MessageConverter> delegatesByContentType) {
        this.delegates.clear();
        this.delegates.putAll(delegatesByContentType);
    }

    public Map<String, MessageConverter> getDelegates() {
        return Collections.unmodifiableMap(this.delegates);
    }


    /**
     * Add a delegate converter for the content type.
     *
     * @param contentType      the content type to check.
     * @param messageConverter the {@link MessageConverter} for the content type.
     * @since 1.6
     */
    public void addDelegate(String contentType, MessageConverter messageConverter) {
        this.delegates.put(contentType, messageConverter);
    }

    /**
     * Remove the delegate for the content type.
     *
     * @param contentType the content type key to remove {@link MessageConverter} from delegates.
     * @return the remove {@link MessageConverter}.
     */
    public MessageConverter removeDelegate(String contentType) {
        return this.delegates.remove(contentType);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        String contentType = message.getMessageProperties().getContentType();
        return getConverterForContentType(contentType).fromMessage(message);
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) {
        String contentType = messageProperties.getContentType();
        return getConverterForContentType(contentType).toMessage(object, messageProperties);
    }

    protected MessageConverter getConverterForContentType(String contentType) {
        MessageConverter delegate = getDelegates().get(contentType);
        if (delegate == null) {
            delegate = this.defaultConverter;
        }

        if (delegate == null) {
            throw new MessageConversionException("No delegate converter is specified for content type " + contentType);
        } else {
            return delegate;
        }
    }

    private String defaultConverterBeanName;


    public void setDefaultConverterBeanName(String defaultConverterBeanName) {
        this.defaultConverterBeanName = defaultConverterBeanName;
    }

    private Map<String, String[]> converterMappings;

    public void setConverterMappings(Map<String, String[]> converterMappings) {
        this.converterMappings = converterMappings;
    }


    private ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(defaultConverterBeanName, "defaultConverterBeanName must not empty text.");
        Assert.notEmpty(converterMappings, "converterMappings must not empty.");

        this.defaultConverter = (MessageConverter) applicationContext.getBean(defaultConverterBeanName);
        logger.info("set default converter as :" + defaultConverter.getClass().getName());
        applicationContext.getBeansOfType(MessageConverter.class).forEach((beanId, messageConverter) -> {
            if (messageConverter instanceof ContentTypeDelegatingMessageConverter) return;
            String[] contentTypes = converterMappings.get(beanId);
            if (ArrayUtils.isNotEmpty(contentTypes)) {
                for (String contentType : contentTypes) {
                    addDelegate(contentType, messageConverter);
                    logger.debug("adding mapping [{}] to [{}]", contentType, messageConverter.getClass().getName());
                }
            } else {
                logger.warn("no mapping found for [{}]. ignore to process.", messageConverter.getClass().getName());
            }
        });
    }
}
