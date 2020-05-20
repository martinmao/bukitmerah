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

import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.scleropages.core.concurrent.Blocking;
import org.scleropages.core.concurrent.GuavaFutures;
import org.scleropages.serialize.SerializerFactory;
import org.scleropages.serialize.SerializerFactoryUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DefaultMqttClient extends PahoClientSupport implements MqttClient {

    private SerializerFactory<InputStream, OutputStream> serializerFactory;

    public DefaultMqttClient(PahoClientFactory pahoClientFactory) {
        super(pahoClientFactory);
    }

    @Override
    protected void startInternal() throws MqttException {
        initPahoClientIfNecessary(null);
    }

    @Override
    protected void stopInternal() throws MqttException {
        stoppedSubscriptions();
        resetPahoClientIfNecessary();
    }


    public class PublishResult implements Publication {

        private final MqttMessage mqttMessage;
        private final boolean success;
        private final Throwable errorCause;
        private final Object source;

        public PublishResult(MqttMessage mqttMessage, Object source) {
            this.mqttMessage = mqttMessage;
            this.success = true;
            this.errorCause = null;
            this.source = source;
        }

        public PublishResult(MqttMessage mqttMessage, Object source, Throwable errorCause) {
            this.mqttMessage = mqttMessage;
            this.success = false;
            this.errorCause = errorCause;
            this.source = source;
        }

        @Override
        public MqttMessage getMqttMessage() {
            return mqttMessage;
        }

        @Override
        public Throwable getErrorCause() {
            return errorCause;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public Object getSource() {
            return source;
        }
    }

    @Override
    public void publish(String topic, MqttMessage message, GuavaFutures.IdObservable<Integer, Publication> publicationContainer) throws Exceptions.MqttException {
        publish(topic, message, Blocking.NONE_BLOCKING, new FutureCallback<IMqttDeliveryToken>() {
            @Override
            public void onSuccess(IMqttDeliveryToken result) {
                publicationContainer.done(result.getMessageId(), new PublishResult(message, null), false);
            }

            @Override
            public void onFailure(Throwable t) {
                PahoFutureCallBackException e = (PahoFutureCallBackException) t;
                publicationContainer.done(e.getMqttToken().getMessageId(), new PublishResult(message, null, e));
                logger.warn("failure to send message with id: " + e.getMqttToken().getMessageId(), e);
            }
        });
        publicationContainer.subscribe(message.getId());
    }

    @Override
    public void publish(String topic, Object message, int qos, boolean retained, GuavaFutures.IdObservable<Integer, Publication> publicationContainer) throws Exceptions.MqttException {
        final MqttMessage realMessage = MqttMessages.newSendingMessage(serializeMessage(message), qos, retained);
        publish(topic, realMessage, Blocking.NONE_BLOCKING, new FutureCallback<IMqttDeliveryToken>() {
            @Override
            public void onSuccess(IMqttDeliveryToken result) {
                publicationContainer.done(result.getMessageId(), new PublishResult(realMessage, message), false);
            }

            @Override
            public void onFailure(Throwable t) {
                PahoFutureCallBackException e = (PahoFutureCallBackException) t;
                publicationContainer.done(e.getMqttToken().getMessageId(), new PublishResult(realMessage, message, e));
                logger.warn("failure to send message with id: " + e.getMqttToken().getMessageId(), e);
            }
        });
        publicationContainer.subscribe(realMessage.getId());
    }

    @Override
    public void publish(String topic, Object message, int qos, boolean retained, Blocking blocking) throws Exceptions.MqttException {
        publish(topic, MqttMessages.newSendingMessage(serializeMessage(message), qos, retained), blocking, null);
    }

    @Override
    public void publish(String topic, MqttMessage mqttMessage, Blocking blocking) throws Exceptions.MqttException {
        publish(topic, mqttMessage, blocking, null);
    }

    @Override
    public void publish(String topic, Object message, int qos, boolean retained, Blocking blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException {
        publish(topic, MqttMessages.newSendingMessage(serializeMessage(message), qos, retained), blocking, sendingCallback);
    }

    @Override
    public void publish(boolean checkConnected, String topic, Object message, int qos, boolean retained, Blocking blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException {
        publish(checkConnected, topic, MqttMessages.newSendingMessage(serializeMessage(message), qos, retained), blocking, sendingCallback);
    }

    @Override
    public void subscribe(String topic, int qos, IMqttMessageListener messageListener) {
        Subscriber subscriber = new Subscriber(topic, qos, messageListener);
        addSubscriber(subscriber);
        try {
            startSubscriber(topic);
        } catch (MqttException e) {
            throw Exceptions.asUncheckMqttException(e);
        }
    }

    @Override
    public void unSubscribe(String topic) {
        try {
            stopSubscriber(topic);
            removeSubscriber(topic);
        } catch (MqttException e) {
            throw Exceptions.asUncheckMqttException(e);
        }
    }


    protected byte[] serializeMessage(Object message) {
        try {
            return SerializerFactoryUtil.serialize(serializerFactory, message);
        } catch (IOException e) {
            throw new IllegalStateException("failure to serialize message.", e);
        }
    }

    protected <T> T deserializeMessage(MqttMessage message) {
        try {
            return SerializerFactoryUtil.deserialize(serializerFactory, message.getPayload());
        } catch (Exception e) {
            throw new IllegalStateException("failure to deserialize message.", e);
        }
    }

    @Override
    protected void postIMqttAcknowledgeableMessageClientListener(IMqttAcknowledgeableMessageClientListener iMqttAcknowledgeableMessageClientListener) {
        super.postIMqttAcknowledgeableMessageClientListener(iMqttAcknowledgeableMessageClientListener);
        iMqttAcknowledgeableMessageClientListener.setSerializerFactory(serializerFactory);
    }

    // just for testing..

    @ManagedOperation(description = "publish a utf8 encoded text.just for testing.")
    public void publishUtf8TextMessage(String topic, String text, int qos, boolean retained) {
        try {
            MqttMessage message = MqttMessages.newSendingMessage(text.getBytes("utf-8"), qos, retained);
            publish(topic, message, Blocking.NONE_BLOCKING, null);
        } catch (Exception e) {
            logger.debug("failure to send text message", e);
            throw new IllegalStateException(e);
        }
    }


    @ManagedOperation(description = "add subscriber receive message from specify topic and write to std-out as utf-8 text.just for testing")
    public void newUtf8StdOutSubscriber(String topic, int qos) {
        Subscriber subscriber = new Subscriber(topic, qos, utf8StdOutListener);
        addSubscriber(subscriber);
    }

    private final IMqttAcknowledgeableMessageClientListener utf8StdOutListener = new IMqttAcknowledgeableMessageClientListener() {
        @Override
        protected void messageArrivedInternal(String topic, AcknowledgeableMqttMessage message) throws Exception {
            logger.info("received message from {}. [{}] text: {}", topic, message.getId(), new String(message.getPayload(), "utf-8"));
            message.acknowledge();
        }
    };

    @Autowired
    public void setSerializerFactory(SerializerFactory<InputStream, OutputStream> serializerFactory) {
        this.serializerFactory = serializerFactory;
    }
}
