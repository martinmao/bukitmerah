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


import org.scleropages.core.concurrent.Blocking;
import org.scleropages.core.concurrent.GuavaFutures;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface AmqpClient {

    /**
     * 异步发布，发布结果通过publicationCallback回调获取
     *
     * @param exchange            exchange name
     * @param routingKey          routing key bindings from exchange to queue(or other exchanges)
     * @param message             the spring amqp-message
     * @param publicationCallback callback method for message confirm(or returned).if null use global publicationCallback set on initialize.
     */
    void send(String exchange, String routingKey,
              Message message, FutureCallback<Publication> publicationCallback);

    boolean send(String exchange, String routingKey,
                 Message message, Blocking blocking) throws ExecutionException, InterruptedException, TimeoutException;


    /**
     * 异步发布，发布结果可在指定周期内通过publicationObservable获取，默认使用messageId作为 subscribe-Id,但如果未提供则由系统自动生成.
     *
     * @param exchange              exchange name
     * @param routingKey            routing key bindings from exchange to queue(or other exchanges)
     * @param message               the spring amqp-message
     * @param publicationObservable publication observable
     */
    void send(String exchange, String routingKey,
              Message message, GuavaFutures.IdObservable<String, Publication> publicationObservable);

    void send(String exchange, String routingKey,
              Object message, MessageProperties messageProperties, FutureCallback<Publication> publicationCallback);

    boolean send(String exchange, String routingKey,
                 Object message, MessageProperties messageProperties, Blocking blocking) throws ExecutionException, InterruptedException, TimeoutException;

    void send(String exchange, String routingKey,
              Object message, MessageProperties messageProperties, GuavaFutures.IdObservable<String, Publication> publicationObservable);

    Message exchange(String exchange, String routingKey,
                     Message message);

    void exchange(String exchange, String routingKey,
                  Message message, FutureCallback<Message> replyCallback);

    <T> T exchange(String exchange, String routingKey,
                   Object message, MessageProperties messageProperties);

    <T> void exchange(String exchange, String routingKey,
                      Object message, MessageProperties messageProperties, FutureCallback<T> replyCallback);


    RabbitListenerEndpoint createRabbitListenerEndpoint(String id, String[] queueNames, boolean autoStartup, boolean exclusive, String concurrency, MessageListener messageListener);

    void registerMessageListenerContainer(RabbitListenerEndpoint rabbitListenerEndpoint, boolean startImmediately);

    MessageListenerContainer getMessageListenerContainer(String id);

    Set<String> getMessageListenerContainerIds();

    void stopMessageListenerContainer(String id);

    void startMessageListenerContainer(String id);

    void createExchange(String name, String type, boolean durable, boolean autoDelete);

    void deleteExchange(String name);

    void createQueue(String name, boolean durable, boolean exclusive, boolean autoDelete);

    void deleteQueue(String name, boolean unused, boolean empty);

    void createBinding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey);
}
