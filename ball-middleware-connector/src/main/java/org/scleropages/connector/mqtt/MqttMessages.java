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

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class MqttMessages {

    public static final int QOS_AT_MOST_ONCE = 0;
    public static final int QOS_AT_LEAST_ONCE = 1;
    public static final int QOS_EXACTLY_ONCE = 2;

    /**
     * 默认的消息qos级别 QOS_AT_LEAST_ONCE，可靠性以及性能均衡考虑，Mqtt provider 对发送的消息进行一次确认(PUBBACK).确保消息不会丢失.但会有重复消息投递
     *
     * @param payload
     * @return
     */
    public static MqttMessage defaultSendingMessage(byte[] payload) {
        return newSendingMessage(payload, QOS_AT_LEAST_ONCE, false);
    }

    /**
     * 最低的qos级别，QOS_AT_MOST_ONCE，最大化性能，Mqtt provider 不会对发送的消息进行确认.会导致消息丢失
     *
     * @param payload
     * @return
     */
    public static MqttMessage noAckSendingMessage(byte[] payload) {
        return newSendingMessage(payload, QOS_AT_MOST_ONCE, false);
    }

    /**
     * 最高qos级别，QOS_EXACTLY_ONCE，最大化可靠性，Mqtt provider会对发送消息进行两步确认(PUBREC->PUBREL->PUBCOMP)，确保消息不会重复投递
     *
     * @param payload
     * @return
     */
    public static MqttMessage reliableSendingMessage(byte[] payload) {
        return newSendingMessage(payload, QOS_EXACTLY_ONCE, false);
    }

    /**
     * @param payload
     * @param qos
     * @param retained mqtt provider会保留该topic的最后一条retained消息，当订阅端上线后会将消息派发，可考虑用于订阅端一上线就需要获取发布方的最新状态等.保留消息可以不断被覆盖，也可删除（空的消息体）
     * @return
     */
    public static MqttMessage newSendingMessage(byte[] payload, int qos, boolean retained) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(retained);
        mqttMessage.setPayload(payload);
        return mqttMessage;
    }
}
