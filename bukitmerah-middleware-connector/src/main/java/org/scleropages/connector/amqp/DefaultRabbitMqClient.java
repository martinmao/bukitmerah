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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.adapter.AbstractAdaptableMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 *
 */
@ManagedResource(objectName = "org.scleropages.connector:name=rabbitmq", description = "rabbitmq client instance.")
public class DefaultRabbitMqClient implements AmqpClient, InitializingBean, ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final RabbitAdmin admin;

    private final RabbitTemplate template;

    private AsyncRabbitTemplate asyncAmqpTemplate;

    private final MessageConverter messageConverter;

    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    private RabbitListenerContainerFactory rabbitListenerContainerFactory;

    private RabbitTemplate.ReturnCallback returnCallback;

    private RabbitTemplate.ConfirmCallback confirmCallback;


    public DefaultRabbitMqClient(RabbitAdmin amqpAdmin, RabbitTemplate amqpTemplate, MessageConverter messageConverter) {
        this.admin = amqpAdmin;
        this.template = amqpTemplate;
        this.messageConverter = messageConverter;
    }


    /**
     * 异步发布，发布结果通过publicationCallback回调获取
     *
     * @param exchange            exchange name
     * @param routingKey          routing key bindings from exchange to queue(or other exchanges)
     * @param message             the spring amqp-message
     * @param publicationCallback callback method for message confirm(or returned).if null use global publicationCallback set on initialize.
     */
    @Override
    public void send(final String exchange, final String routingKey,
                     final Message message, final FutureCallback<Publication> publicationCallback) {
        String returnedId = generateDefaultCorrelationIdIfNecessary(message.getMessageProperties());
        final CorrelationData correlationData = null != publicationCallback ?
                new InvocationCorrelationData(returnedId, message, null, publicationCallback) : new DefaultCorrelationData(returnedId, message, null);
        template.send(exchange, routingKey, message, correlationData);
    }

    /**
     * 同步或异步发布
     *
     * @param exchange   exchange name
     * @param routingKey routing key bindings from exchange to queue(or other exchanges)
     * @param message    the spring amqp-message
     * @param blocking   in blocking mode or not
     * @return true if successfully published to remote broker. in async mode always return false.
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Override
    public boolean send(final String exchange, final String routingKey,
                        final Message message, final Blocking blocking) throws ExecutionException, InterruptedException, TimeoutException {
        final CorrelationData correlationData = new DefaultCorrelationData(generateDefaultCorrelationIdIfNecessary(message.getMessageProperties()), message, null);
        template.send(exchange, routingKey, message, correlationData);
        if (Blocking.isBlocking(blocking)) {
            if (Blocking.isPermanentBlocking(blocking)) {
                return correlationData.getFuture().get().isAck() && correlationData.getReturnedMessage() == null;
            } else {
                return correlationData.getFuture().get(blocking.toMillis(), TimeUnit.MILLISECONDS).isAck() && correlationData.getReturnedMessage() == null;
            }
        } else
            return false;
    }


    /**
     * 异步发布，发布结果可在指定周期内通过publicationObservable获取，默认使用messageId作为 subscribe-Id,但如果未提供则由系统自动生成.
     *
     * @param exchange              exchange name
     * @param routingKey            routing key bindings from exchange to queue(or other exchanges)
     * @param message               the spring amqp-message
     * @param publicationObservable publication observable
     */
    @Override
    public void send(final String exchange, final String routingKey,
                     final Message message, GuavaFutures.IdObservable<String, Publication> publicationObservable) {

        String subscribeId = generateDefaultCorrelationIdIfNecessary(message.getMessageProperties());

        send(exchange, routingKey, message, new FutureCallback<Publication>() {

            @Override
            public void onSuccess(Publication result) {
                publicationObservable.done(subscribeId, result);
            }

            @Override
            public void onFailure(Throwable t, Publication result) {
                publicationObservable.fault(subscribeId, new PublicationException(t, result), false);
            }
        });
        publicationObservable.subscribe(subscribeId);
    }


    @Override
    public void send(final String exchange, final String routingKey,
                     final Object message, MessageProperties messageProperties, final FutureCallback<Publication> publicationCallback) {
        send(exchange, routingKey, toMessage(message, messageProperties), publicationCallback);
    }

    @Override
    public boolean send(final String exchange, final String routingKey,
                        final Object message, MessageProperties messageProperties, final Blocking blocking) throws ExecutionException, InterruptedException, TimeoutException {
        return send(exchange, routingKey, toMessage(message, messageProperties), blocking);
    }

    @Override
    public void send(final String exchange, final String routingKey,
                     final Object message, MessageProperties messageProperties, GuavaFutures.IdObservable<String, Publication> publicationObservable) {
        send(exchange, routingKey, toMessage(message, messageProperties), publicationObservable);
    }

    @Override
    public Message exchange(final String exchange, final String routingKey,
                            final Message message) {
        return template.sendAndReceive(exchange, routingKey, message, null);
    }

    @Override
    public void exchange(final String exchange, final String routingKey,
                         final Message message, FutureCallback<Message> replyCallback) {
        ListenableFuture<Message> messageListenableFuture = getAsyncAmqpTemplate().sendAndReceive(exchange, routingKey, message);
        messageListenableFuture.addCallback(new ListenableFutureCallback<Message>() {
            @Override
            public void onFailure(Throwable ex) {
                replyCallback.onFailure(ex);
            }

            @Override
            public void onSuccess(Message result) {
                replyCallback.onSuccess(result);
            }
        });
    }


    @Override
    public <T> T exchange(final String exchange, final String routingKey,
                          final Object message, final MessageProperties messageProperties) {
        Message reply = exchange(exchange, routingKey, toMessage(message, messageProperties));
        return (T) fromMessage(reply);
    }

    @Override
    public <T> void exchange(final String exchange, final String routingKey,
                             final Object message, final MessageProperties messageProperties, FutureCallback<T> replyCallback) {
        Assert.notNull(replyCallback, "replyCallback must not be null.");
        ListenableFuture<Message> messageListenableFuture = getAsyncAmqpTemplate().sendAndReceive(exchange, routingKey,
                toMessage(message, messageProperties));
        messageListenableFuture.addCallback(new ListenableFutureCallback<Message>() {
            @Override
            public void onFailure(Throwable ex) {
                replyCallback.onFailure(ex);
            }

            @Override
            public void onSuccess(Message result) {
                replyCallback.onSuccess((T) fromMessage(result));
            }
        });
    }

    // 启动停止异步rpc客户端
    @ManagedOperation
    public void startAsyncAmqpTemplate() {
        getAsyncAmqpTemplate().start();
    }
    @ManagedOperation
    public void stopAsyncAmqpTemplate() {
        getAsyncAmqpTemplate().start();
    }


    @Override
    public RabbitListenerEndpoint createRabbitListenerEndpoint(String id, String[] queueNames, boolean autoStartup, boolean exclusive, String concurrency, MessageListener messageListener) {
        SimpleRabbitListenerEndpoint simpleRabbitListenerEndpoint = new SimpleRabbitListenerEndpoint();
        simpleRabbitListenerEndpoint.setId(id);
        simpleRabbitListenerEndpoint.setQueueNames(queueNames);
        simpleRabbitListenerEndpoint.setAutoStartup(autoStartup);
        simpleRabbitListenerEndpoint.setExclusive(exclusive);
        simpleRabbitListenerEndpoint.setMessageConverter(messageConverter);
        simpleRabbitListenerEndpoint.setAdmin(admin);
        simpleRabbitListenerEndpoint.setConcurrency(concurrency);
        processMessageListener(messageListener);
        simpleRabbitListenerEndpoint.setMessageListener(messageListener);
        return simpleRabbitListenerEndpoint;
    }

    protected void processMessageListener(MessageListener messageListener) {
        if (messageListener instanceof AbstractAdaptableMessageListener) {
            AbstractAdaptableMessageListener adaptableMessageListenerMessageListener = (AbstractAdaptableMessageListener) messageListener;
            adaptableMessageListenerMessageListener.setMessageConverter(messageConverter);
        }
    }


    @Override
    public void registerMessageListenerContainer(RabbitListenerEndpoint rabbitListenerEndpoint, boolean startImmediately) {
        rabbitListenerEndpointRegistry.registerListenerContainer(rabbitListenerEndpoint, rabbitListenerContainerFactory, startImmediately);
    }


    @Override
    @ManagedOperation
    public MessageListenerContainer getMessageListenerContainer(String id) {
        return rabbitListenerEndpointRegistry.getListenerContainer(id);
    }

    @Override
    @ManagedOperation
    public Set<String> getMessageListenerContainerIds() {
        return rabbitListenerEndpointRegistry.getListenerContainerIds();
    }

    @Override
    @ManagedOperation
    public void stopMessageListenerContainer(String id) {
        MessageListenerContainer messageListenerContainer = getMessageListenerContainer(id);
        messageListenerContainer.stop();
        logger.info("successfully stopped message listener container for id: [{}]", id);
    }

    @Override
    @ManagedOperation
    public void startMessageListenerContainer(String id) {
        MessageListenerContainer messageListenerContainer = getMessageListenerContainer(id);
        messageListenerContainer.start();
        logger.info("successfully started message listener container for id: [{}]", id);
    }

    private AtomicInteger currentCorrelationId = new AtomicInteger();

    private static final String DEFAULT_CORRELATION_ID_PREFIX = "sys_";

    /**
     * implementation this method to generate a default correlationId if no message id provided from message properties.
     * must make sure unique. otherwise the returned message can not obtain from CorrelationData.
     * by default use thread-safety number auto incrementer and with prefix "sys_"
     *
     * @return
     */
    protected String generateDefaultCorrelationIdIfNecessary(MessageProperties messageProperties) {
        String messageId = null != messageProperties ? messageProperties.getMessageId() : null;
        return null != messageId ? messageId : DEFAULT_CORRELATION_ID_PREFIX + currentCorrelationId.incrementAndGet();
    }


    protected Message toMessage(final Object object, MessageProperties messageProperties) {
        if (object instanceof Message) {
            return (Message) object;
        }
        messageProperties = messageProperties != null ? messageProperties : new MessageProperties();
        if (object instanceof MessagePropertiesOverrider) {
            MessagePropertiesOverrider messagePropertiesOverrider = (MessagePropertiesOverrider) object;
            messagePropertiesOverrider.overrideMessageProperties(messageProperties);
        }
        return messageConverter.toMessage(object, messageProperties);
    }

    protected Object fromMessage(final Message message) {
        return messageConverter.fromMessage(message);
    }

    protected void setupCallbacks() {
        template.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            logger.warn("Returned: " + message + "\nreplyCode: " + replyCode
                    + "\nreplyText: " + replyText + "\nexchange/rk: " + exchange + "/" + routingKey);
            if (returnCallback != null)
                returnCallback.returnedMessage(message, replyCode, replyText, exchange, routingKey);
        });

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData instanceof InvocationCorrelationData) {
                invokeCorrelationData((InvocationCorrelationData) correlationData, ack, cause);
            } else {
                if (confirmCallback != null)
                    confirmCallback.confirm(correlationData, ack, cause);
            }
        });
    }


    protected void invokeCorrelationData(InvocationCorrelationData invocationCorrelationData, boolean ack, String cause) {
        if (ack) {
            if (invocationCorrelationData.getReturnedMessage() == null)
                invocationCorrelationData.getPublicationCallback().onSuccess(
                        new PublicationImpl(invocationCorrelationData));
            else {
                Throwable throwable = new IllegalStateException("returned by broker.");
                invocationCorrelationData.getPublicationCallback().onFailure(throwable, new PublicationImpl(invocationCorrelationData, throwable));
            }
        } else {
            Throwable throwable = new IllegalStateException(cause);
            invocationCorrelationData.getPublicationCallback().onFailure(throwable, new PublicationImpl(invocationCorrelationData, throwable));
        }
    }


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AsyncRabbitTemplate getAsyncAmqpTemplate() {
        if (null == asyncAmqpTemplate)
            throw new IllegalStateException("asyncAmqpTemplate not enabled.");
        return asyncAmqpTemplate;
    }

    private class PublicationImpl implements Publication {

        private final Message message;

        private final Object source;

        private final boolean success;

        private final Throwable cause;

        public PublicationImpl(InvocationCorrelationData invocationCorrelationData) {
            this.message = invocationCorrelationData.getMessage();
            this.source = invocationCorrelationData.getSource();
            this.success = true;
            this.cause = null;
        }

        public PublicationImpl(InvocationCorrelationData invocationCorrelationData, Throwable t) {
            this.message = invocationCorrelationData.getMessage();
            this.source = invocationCorrelationData.getSource();
            this.success = false;
            this.cause = t;
        }

        @Override
        public Message message() {
            return message;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }

        @Override
        public Object source() {
            return source;
        }

        @Override
        public Throwable cause() {
            return cause;
        }
    }

    private class InvocationCorrelationData extends DefaultCorrelationData {

        private final FutureCallback<Publication> publicationCallback;

        public InvocationCorrelationData(String id, Message message, Object source, FutureCallback<Publication> publicationCallback) {
            super(id, message, source);
            this.publicationCallback = publicationCallback;
        }

        protected FutureCallback<Publication> getPublicationCallback() {
            return publicationCallback;
        }
    }

    private class DefaultCorrelationData extends CorrelationData {
        private final Message message;
        private final Object source;

        protected DefaultCorrelationData(String id, Message message, Object source) {
            super(id);
            this.message = message;
            this.source = source;
        }

        protected Message getMessage() {
            return message;
        }

        protected Object getSource() {
            return source;
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Autowired
    public void setRabbitListenerEndpointRegistry(RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry) {
        this.rabbitListenerEndpointRegistry = rabbitListenerEndpointRegistry;
    }

    @Autowired
    public void setRabbitListenerContainerFactory(RabbitListenerContainerFactory rabbitListenerContainerFactory) {
        this.rabbitListenerContainerFactory = rabbitListenerContainerFactory;
    }

    @Override
    @ManagedOperation(description = "Create an exchange from the broker")
    public void createExchange(String name, String type, boolean durable, boolean autoDelete) {
        CustomExchange customExchange = new CustomExchange(name, type, durable, autoDelete);
        admin.declareExchange(customExchange);
        logger.info("successfully declare exchange:[{}]", customExchange);
    }

    @Override
    @ManagedOperation(description = "Delete an exchange from the broker")
    public void deleteExchange(String name) {
        admin.deleteExchange(name);
        logger.info("successfully delete exchange:[{}]", name);
    }

    @Override
    @ManagedOperation(description = "Create an queue from the broker")
    public void createQueue(String name, boolean durable, boolean exclusive, boolean autoDelete) {
        Queue queue = new Queue(name, durable, exclusive, autoDelete);
        admin.declareQueue(queue);
        logger.info("successfully declare queue:[{}]", queue);

    }

    @Override
    @ManagedOperation(description = "Delete an queue from the broker")
    public void deleteQueue(String name, boolean unused, boolean empty) {
        admin.deleteQueue(name, unused, empty);
        logger.info("successfully delete queue:[{}]", name);
    }

    @Override
    @ManagedOperation(description = "Create an binding from the broker")
    public void createBinding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey) {
        Binding binding = new Binding(destination, destinationType, exchange, routingKey, Collections.emptyMap());
        admin.declareBinding(binding);
        logger.info("successfully declare binding:[{}]", binding);
    }

    public void setReturnCallback(RabbitTemplate.ReturnCallback returnCallback) {
        this.returnCallback = returnCallback;
    }

    public void setConfirmCallback(RabbitTemplate.ConfirmCallback confirmCallback) {
        this.confirmCallback = confirmCallback;
    }

    public void setAsyncAmqpTemplate(AsyncRabbitTemplate asyncAmqpTemplate) {
        this.asyncAmqpTemplate = asyncAmqpTemplate;
    }
}
