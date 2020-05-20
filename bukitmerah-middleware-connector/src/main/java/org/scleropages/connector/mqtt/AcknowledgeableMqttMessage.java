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

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class AcknowledgeableMqttMessage extends MqttMessage {


    private final MqttMessage nativeMessage;

    private final IMqttAsyncClient client;

    public AcknowledgeableMqttMessage(final MqttMessage nativeMessage, final IMqttAsyncClient client) {
        this.nativeMessage = nativeMessage;
        this.client = client;
    }


    public void acknowledge() {
        try {
            client.messageArrivedComplete(nativeMessage.getId(), nativeMessage.getQos());
        } catch (MqttException e) {
            throw new IllegalStateException("can't acknowledge received message: " + nativeMessage.getId(), e);
        }
    }

    /**
     * Returns the payload as a byte array.
     *
     * @return the payload as a byte array.
     */
    public byte[] getPayload() {
        return nativeMessage.getPayload();
    }

    /**
     * Clears the payload, resetting it to be empty.
     *
     * @throws IllegalStateException if this message cannot be edited
     */
    public void clearPayload() {
        nativeMessage.clearPayload();
    }

    /**
     * Sets the payload of this message to be the specified byte array.
     *
     * @param payload the payload for this message.
     * @throws IllegalStateException if this message cannot be edited
     * @throws NullPointerException  if no payload is provided
     */
    public void setPayload(byte[] payload) {
        if (null != nativeMessage)
            nativeMessage.setPayload(payload);
    }

    /**
     * Returns whether or not this message should be/was retained by the server.
     * For messages received from the server, this method returns whether or not
     * the message was from a current publisher, or was "retained" by the server as
     * the last message published on the topic.
     *
     * @return <code>true</code> if the message should be, or was, retained by
     * the server.
     * @see #setRetained(boolean)
     */
    public boolean isRetained() {
        return nativeMessage.isRetained();
    }

    /**
     * Whether or not the publish message should be retained by the messaging engine.
     * Sending a message with retained set to <code>true</code> and with an empty
     * byte array as the payload e.g. <code>new byte[0]</code> will clear the
     * retained message from the server.  The default value is <code>false</code>
     *
     * @param retained whether or not the messaging engine should retain the message.
     * @throws IllegalStateException if this message cannot be edited
     */
    public void setRetained(boolean retained) {
        nativeMessage.setRetained(retained);
    }

    /**
     * Returns the quality of service for this message.
     *
     * @return the quality of service to use, either 0, 1, or 2.
     * @see #setQos(int)
     */
    public int getQos() {
        return nativeMessage.getQos();
    }

    /**
     * Sets the quality of service for this message.
     * <ul>
     * <li>Quality of Service 0 - indicates that a message should
     * be delivered at most once (zero or one times).  The message will not be persisted to disk,
     * and will not be acknowledged across the network.  This QoS is the fastest,
     * but should only be used for messages which are not valuable - note that
     * if the server cannot process the message (for example, there
     * is an authorization problem), then an
     * {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}.
     * Also known as "fire and forget".</li>
     *
     * <li>Quality of Service 1 - indicates that a message should
     * be delivered at least once (one or more times).  The message can only be delivered safely if
     * it can be persisted, so the application must supply a means of
     * persistence using <code>MqttConnectOptions</code>.
     * If a persistence mechanism is not specified, the message will not be
     * delivered in the event of a client failure.
     * The message will be acknowledged across the network.
     * This is the default QoS.</li>
     *
     * <li>Quality of Service 2 - indicates that a message should
     * be delivered once.  The message will be persisted to disk, and will
     * be subject to a two-phase acknowledgement across the network.
     * The message can only be delivered safely if
     * it can be persisted, so the application must supply a means of
     * persistence using <code>MqttConnectOptions</code>.
     * If a persistence mechanism is not specified, the message will not be
     * delivered in the event of a client failure.</li>
     *
     * </ul>
     * If persistence is not configured, QoS 1 and 2 messages will still be delivered
     * in the event of a network or server problem as the client will hold state in memory.
     * If the MQTT client is shutdown or fails and persistence is not configured then
     * delivery of QoS 1 and 2 messages can not be maintained as client-side state will
     * be lost.
     *
     * @param qos the "quality of service" to use.  Set to 0, 1, 2.
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @throws IllegalStateException    if this message cannot be edited
     */
    public void setQos(int qos) {
        nativeMessage.setQos(qos);
    }

    /**
     * Returns a string representation of this message's payload.
     * Makes an attempt to return the payload as a string. As the
     * MQTT client has no control over the content of the payload
     * it may fail.
     *
     * @return a string representation of this message.
     */
    public String toString() {
        return nativeMessage.toString();
    }

    /**
     * Returns whether or not this message might be a duplicate of one which has
     * already been received.  This will only be set on messages received from
     * the server.
     *
     * @return <code>true</code> if the message might be a duplicate.
     */
    public boolean isDuplicate() {
        return nativeMessage.isDuplicate();
    }

    /**
     * This is only to be used internally to provide the MQTT id of a message
     * received from the server.  Has no effect when publishing messages.
     *
     * @param messageId The Message ID
     */
    public void setId(int messageId) {
        nativeMessage.setId(messageId);
    }

    /**
     * Returns the MQTT id of the message.  This is only applicable to messages
     * received from the server.
     *
     * @return the MQTT id of the message
     */
    public int getId() {
        return nativeMessage.getId();
    }
}
