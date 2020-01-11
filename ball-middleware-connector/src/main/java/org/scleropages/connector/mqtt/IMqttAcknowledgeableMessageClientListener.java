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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.SerializerFactoryUtil;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class IMqttAcknowledgeableMessageClientListener implements IMqttMessageListener {


    private IMqttAsyncClient client;

    private SerializerFactory<InputStream, OutputStream> serializerFactory;

    @Override
    public final void messageArrived(String topic, MqttMessage message) throws Exception {
        Assert.notNull(client, "internal error. client is null.");
        messageArrivedInternal(topic, new AcknowledgeableMqttMessage(message, client));
    }

    abstract protected void messageArrivedInternal(String topic, AcknowledgeableMqttMessage message) throws Exception;


    /**
     * helper method for subclasses used for deserialize message payload user internal {@link SerializerFactory}
     *
     * @param message
     * @return
     */
    protected Object deserialize(AcknowledgeableMqttMessage message) {
        try {
            return SerializerFactoryUtil.deserialize(serializerFactory, message.getPayload());
        } catch (Exception e) {
            throw new IllegalStateException("failure to deserialize message.", e);
        }
    }

    public final void setClient(IMqttAsyncClient client) {
        this.client = client;
    }

    public final void setSerializerFactory(SerializerFactory<InputStream, OutputStream> serializerFactory) {
        this.serializerFactory = serializerFactory;
    }
}
