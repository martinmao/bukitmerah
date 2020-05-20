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


/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface MqttClient {


    /**
     * 异步发送，publicationContainer，客户端可以定期通过publicationContainer获取发布结果
     *
     * @param topic
     * @param message
     * @param publicationContainer
     * @throws MqttException
     */
    void publish(String topic, MqttMessage message, GuavaFutures.IdObservable<Integer, Publication> publicationContainer) throws Exceptions.MqttException;

    /**
     * 异步发送，publicationContainer，客户端可以定期通过publicationContainer获取发布结果
     *
     * @param topic
     * @param message
     * @param qos
     * @param retained
     * @param publicationContainer
     * @throws Exceptions.MqttException
     */
    void publish(String topic, Object message, int qos, boolean retained, GuavaFutures.IdObservable<Integer, Publication> publicationContainer) throws Exceptions.MqttException;

    /**
     * 同步或异步发送，但如果使用异步无法获取交付结果
     *
     * @param topic
     * @param mqttMessage
     * @param blocking
     * @throws Exceptions.MqttException
     */
    void publish(String topic, MqttMessage mqttMessage, Blocking blocking) throws Exceptions.MqttException;


    /**
     * 同步或异步发送，但如果使用异步无法获取交付结果
     *
     * @param topic
     * @param message
     * @param qos
     * @param retained
     * @param blocking
     * @throws Exceptions.MqttException
     */
    void publish(String topic, Object message, int qos, boolean retained, Blocking blocking) throws Exceptions.MqttException;

    /**
     * 同步或异步发送，可以通过FutureCallback获取交付结果
     *
     * @param topic
     * @param mqttMessage
     * @param blocking
     * @param sendingCallback
     * @throws Exceptions.MqttException
     */
    void publish(String topic, MqttMessage mqttMessage, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException;


    /**
     * 同步或异步发送，可以通过FutureCallback获取交付结果
     *
     * @param topic
     * @param mqttMessage
     * @param qos
     * @param retained
     * @param blocking
     * @param sendingCallback
     * @throws Exceptions.MqttException
     */
    void publish(String topic, Object mqttMessage, int qos, boolean retained, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException;

    /**
     * 同步或异步发送，可以通过FutureCallback获取交付结果
     *
     * @param checkConnected
     * @param topic
     * @param mqttMessage
     * @param blocking
     * @param sendingCallback
     * @throws Exceptions.MqttException
     */
    void publish(boolean checkConnected, String topic, MqttMessage mqttMessage, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException;


    /**
     * 同步或异步发送，可以通过FutureCallback获取交付结果
     *
     * @param checkConnected
     * @param topic
     * @param mqttMessage
     * @param qos
     * @param retained
     * @param blocking
     * @param sendingCallback
     * @throws Exceptions.MqttException
     */
    void publish(boolean checkConnected, String topic, Object mqttMessage, int qos, boolean retained, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException;

    /**
     * 参与订阅
     *
     * @param topic
     * @param qos
     * @param messageListener
     */
    void subscribe(String topic, int qos, IMqttMessageListener messageListener);

    /**
     * 取消订阅
     *
     * @param topic
     */
    void unSubscribe(String topic);
}
