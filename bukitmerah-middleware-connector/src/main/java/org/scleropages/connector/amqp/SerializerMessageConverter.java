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

import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.SerializerFactoryUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SerializerMessageConverter extends AbstractMessageConverter {

    public static final String DEFAULT_CONTENT_TYPE = "application/scleropages-stream";

    private final SerializerFactory<InputStream, OutputStream> serializerFactory;


    public SerializerMessageConverter(SerializerFactory<InputStream, OutputStream> serializerFactory) {
        this.serializerFactory = serializerFactory;
    }

    @Override
    protected Message createMessage(Object object, MessageProperties messageProperties) {
        try {
            byte[] payload = SerializerFactoryUtil.serialize(serializerFactory, object);
            if (messageProperties.getContentLength() == 0)
                messageProperties.setContentLength(payload.length);
            if (!StringUtils.hasText(messageProperties.getContentType()))
                messageProperties.setContentType(DEFAULT_CONTENT_TYPE);
            return new Message(payload, messageProperties);
        } catch (IOException e) {
            throw new IllegalStateException("failure to serialize message.", e);
        }
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        try {
            return SerializerFactoryUtil.deserialize(serializerFactory, message.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("failure to deserialize message.", e);
        }
    }
}
