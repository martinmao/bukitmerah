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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubAck;

import org.scleropages.core.concurrent.Blocking;
import org.scleropages.core.concurrent.ExecutorServices;
import org.scleropages.core.concurrent.ExponentialBackoffTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@ManagedResource(objectName = "org.scleropages.connector:name=mqtt", description = "mqtt client instance.")
public abstract class PahoClientSupport implements InitializingBean, DisposableBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final PahoClientFactory pahoClientFactory;

    private final MqttConnectOptions mqttConnectOptions;

    private String url;

    @Value("#{ @environment['mqtt.client.client-id'] ?: null}")
    private String clientId;

    private boolean autoClientId = true;

    private final AtomicBoolean started = new AtomicBoolean(false);


    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private volatile MqttAsyncClient client;
    private volatile boolean connected = false;
    private volatile IMqttToken connectingToken;

    private volatile boolean autoReconnect;

    @Value("#{ @environment['mqtt.client.reconnect-delay'] ?: null}")
    private long reconnectDelay;
    @Value("#{ @environment['mqtt.client.auto-ack'] ?: false}")
    private boolean autoAck;
    @Value("#{ @environment['mqtt.client.check-connected-on-publishing'] ?: true}")
    private boolean checkConnectedOnPublishing = true;


    public PahoClientSupport(PahoClientFactory pahoClientFactory) {
        this.pahoClientFactory = pahoClientFactory;
        this.mqttConnectOptions = pahoClientFactory.getConnectionOptions();
        this.autoReconnect = mqttConnectOptions.isAutomaticReconnect();
        if (reconnectDelay == 0)
            reconnectDelay = mqttConnectOptions.getConnectionTimeout() * 1000;
    }

    @ManagedOperation
    public boolean isConnected() {
        readLock.lock();
        try {
            return connected ? connected : null != client && client.isConnected();
        } finally {
            readLock.unlock();
        }
    }

    protected void initPahoClientIfNecessary(FutureCallback<IMqttToken> connectingCallback, boolean fastFault) throws MqttException {
        if (!isConnected()) {
            if (fastFault && !writeLock.tryLock()) {
                return;
            } else
                writeLock.lock();
            try {
                if (!isConnected()) {
                    logger.debug("initializing paho-mqtt client....");
                    resetPahoClientIfNecessary();
                    client = (MqttAsyncClient) pahoClientFactory.newAsyncClient(getUrl(), clientId);
                    client.setCallback(globalListener);
                    client.setManualAcks(!autoAck);
                    connectingToken = client.connect(getMqttConnectOptions(), connectingCallback, connectingListener);
                    connectingToken.waitForCompletion(getMqttConnectOptions().getConnectionTimeout() * 1000);
                    /*当前仅支持同步初始化，需要确保刷新连接状态在当前线程内完成，而如果在其他通知类线程刷新waitForCompletion方法可能会导致死锁???待验证*/
                    connected = connectingToken.isComplete();
                    logger.debug("initialized mqtt client....");
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    protected void initPahoClientIfNecessary(FutureCallback<IMqttToken> connectingCallback) throws MqttException {
        initPahoClientIfNecessary(connectingCallback, false);
    }


    public void publish(boolean checkConnected, String topic, MqttMessage mqttMessage, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException {
        try {
            if (checkConnected) {
                initPahoClientIfNecessary(null, true);
            }
            IMqttDeliveryToken deliveryingFuture = client.publish(topic, mqttMessage, sendingCallback, sendingListener);
            mqttMessage.setId(deliveryingFuture.getMessageId());
            if (Blocking.isBlocking(blocking)) {
                if (Blocking.isPermanentBlocking(blocking))
                    deliveryingFuture.waitForCompletion();
                else
                    deliveryingFuture.waitForCompletion(blocking.toMillis());
            }
        } catch (MqttException e) {
            Exceptions.asUncheckMqttException(e);
        }

    }

    public void publish(String topic, MqttMessage mqttMessage, Blocking
            blocking, FutureCallback<IMqttDeliveryToken> sendingCallback) throws Exceptions.MqttException {
        publish(checkConnectedOnPublishing, topic, mqttMessage, blocking, sendingCallback);
    }

    @ManagedOperation
    public final void start() throws MqttException {
        if (started.compareAndSet(false, true)) {
            try {
                startInternal();
            } catch (Exception e) {
                logger.error("failure to start mqtt client.", e);
                throw e;
            }
        } else {
            logger.warn("mqtt client already started.");
        }
    }

    @ManagedOperation
    public final void stop() throws MqttException {
        if (started.compareAndSet(true, false)) {
            writeLock.lock();
            try {
                stopInternal();
                logger.info("mqtt client shutdown.");
            } catch (Exception e) {
                logger.error("failure to stop mqtt client.", e);
                throw e;
            } finally {
                writeLock.unlock();
            }
        } else {
            logger.warn("mqtt client already stopped or not started.");
        }
    }


    abstract protected void startInternal() throws MqttException;

    abstract protected void stopInternal() throws MqttException;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    protected MqttConnectOptions getMqttConnectOptions() {
        return mqttConnectOptions;
    }


    private final MqttCallback globalListener = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            logger.warn("connection lost. ", cause);
            lostConnection(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            if (logger.isTraceEnabled())
                logger.trace("inbound message from {} with id: {}", topic, message.getId());
            anyMessageArrived(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            if (logger.isTraceEnabled())
                logger.trace("outbound message from {} with id: {}", token.getTopics(), token.getMessageId());
            anyDeliveryComplete(token);
        }
    };

    //~~ for re-connecting
    //~~
    //~~
    //~~
    protected void lostConnection(Throwable cause) {
        writeLock.lock();
        try {
            connected = false;
            subscriptions.forEach((topic, subscriber) -> {
                synchronized (subscriber) {
                    subscriber.error(cause);
                    logger.debug("reset subscriber status: {}", subscriber);
                }
            });
            if (autoReconnect) {
                try {
                    scheduleReconnect();
                } catch (Exception e) {
                    logger.error("failure to schedule-reconnect tasks.", e);
                }
            } else
                logger.warn("reconnect not enabled...");
        } finally {
            writeLock.unlock();
        }
    }


    protected void scheduleReconnect() {

        initSchedulersIfNecessary();
        if (reconnectTask.isPaused()) {
            reconnectTask.resume();
        } else if (!reconnectTask.isStarted()) {
            reconnectTask.reStart();
        } else {
            scheduler.schedule(reconnectTask, 0, TimeUnit.MILLISECONDS);
        }
    }

    private ExponentialBackoffTask reconnectTask;

    private double reconnectFaultDelayRatio = 1.5;

    private AtomicBoolean schedulerInitialized = new AtomicBoolean(false);

    private volatile ScheduledExecutorService scheduler;
    private volatile ThreadPoolExecutor connectingExecutor;


    protected void initSchedulersIfNecessary() {
        if (schedulerInitialized.compareAndSet(false, true)) {
            MqttConnectOptions options = getMqttConnectOptions();
            scheduler = Executors.newScheduledThreadPool(2,
                    new ThreadFactoryBuilder()
                            .setNameFormat("mqttClient-scheduler-%d")
                            .setDaemon(true)
                            .build());
            connectingExecutor = new ThreadPoolExecutor(
                    1, 2, 0, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("mqttClient-scheduler-executor-%d")
                            .setDaemon(true)
                            .build());
            reconnectTask = new ExponentialBackoffTask("mqtt-reconnect-task", reconnectRunnable, (long) (options.getConnectionTimeout() * 1000), reconnectDelay,
                    options.getMaxReconnectDelay(), reconnectFaultDelayRatio,
                    TimeUnit.MILLISECONDS, scheduler, connectingExecutor);
            logger.info("initialized mqtt-schedulers.");
        }
    }

    private final Runnable reconnectRunnable = () -> {
        try {
            initPahoClientIfNecessary(null);
        } catch (Exception e) {
            logger.warn("failure to reconnect mqtt server.", e);
            throw new IllegalStateException(e);//抛出运行时异常便于 reconnectTask捕获，否则reconnectTask认为执行成功重置重连时间.
        }
    };

    protected void destroySchedulers() {
        if (schedulerInitialized.compareAndSet(true, false)) {
            reconnectTask.stop();
            ExecutorServices.gracefulShutdown(connectingExecutor, logger, "mqttClient-scheduler-executor", 10);
            ExecutorServices.gracefulShutdown(scheduler, logger, "mqttClient-scheduler", 10);
            reconnectTask = null;
            connectingExecutor = null;
            scheduler = null;
        }
    }


    private boolean startSubscriptionsOnConnected = true;

    private final IMqttActionListener connectingListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            if (!client.isConnected())
                return;
//            if (null != reconnectTask)
//                reconnectTask.stop();
            destroySchedulers();
            try {
                if (startSubscriptionsOnConnected)
                    initSubscriptions();
            } catch (MqttException e) {
                logger.error("failure to init subscriptions", e);
            }
            try {
                connectSuccessfully(asyncActionToken);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }
            logger.info("successfully connected remote mqtt server: {}", client.getCurrentServerURI());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            logger.error("failure to connect remote mqtt server: " + client.getCurrentServerURI(), exception);
            try {
                connectFailure(asyncActionToken, exception);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }
        }
    };


    /**
     * 必须确保该方法在已经持有写锁的线程中执行
     *
     * @throws MqttException
     */
    protected void resetPahoClientIfNecessary() throws MqttException {
        if (!writeLock.isHeldByCurrentThread())
            throw new IllegalStateException("before calling resetPahoClientIfNecessary current thread must held a write lock.");
        if (null != client) {
            client.setCallback(null);
            if (client.isConnected()) {
                try {
                    client.disconnect().waitForCompletion(getMqttConnectOptions().getConnectionTimeout());
                    logger.debug("disconnect paho-client from: {}", client.getCurrentServerURI());
                } catch (Exception e) {
                    logger.warn("failure to disconnect from: {}" + client.getCurrentServerURI(), e);
                    client.disconnectForcibly();
                }
            }
            try {
                client.close(true);
            } catch (Exception e) {
                logger.warn("failure to close existing paho client.", e);
            }
            client = null;
            connectingToken = null;
            connected = false;
            logger.debug("destroyed existing paho client.");
        }
    }


    private final IMqttActionListener sendingListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            if (logger.isDebugEnabled()) {
                logger.debug("successfully sent message to {} with id: {}", asyncActionToken.getTopics(), asyncActionToken.getMessageId());
            }
            try {
                sent((IMqttDeliveryToken) asyncActionToken);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }

        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            if (logger.isDebugEnabled()) {
                logger.debug("failure sent message to {} with id: {}", asyncActionToken.getTopics(), asyncActionToken.getMessageId());
            }
            try {
                sendFailure((IMqttDeliveryToken) asyncActionToken, exception);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }
        }
    };


    /**
     * 任意入栈歇息都会被调用
     *
     * @param topic
     * @param message
     * @throws Exception
     */
    protected void anyMessageArrived(String topic, MqttMessage message) throws Exception {

    }

    /**
     * 任意出栈消息都会被调用
     *
     * @throws Exception
     */
    protected void anyDeliveryComplete(IMqttDeliveryToken token) {

    }


    /**
     * 连接成功时调用
     *
     * @param asyncActionToken
     */
    protected void connectSuccessfully(IMqttToken asyncActionToken) {
        if (null == asyncActionToken.getUserContext())
            return;
        FutureCallback<IMqttToken> connectingCallback = (FutureCallback<IMqttToken>) asyncActionToken.getUserContext();
        connectingCallback.onSuccess(asyncActionToken);
    }

    /**
     * 连接失败时调用
     *
     * @param asyncActionToken
     * @param exception
     */
    protected void connectFailure(IMqttToken asyncActionToken, Throwable exception) {
        if (null == asyncActionToken.getUserContext())
            return;
        FutureCallback<IMqttToken> connectingCallback = (FutureCallback<IMqttToken>) asyncActionToken.getUserContext();
        connectingCallback.onFailure(new PahoFutureCallBackException(asyncActionToken, exception));
    }

    /**
     * 发送消息成功时调用
     *
     * @param iMqttDeliveryToken
     */
    protected void sent(IMqttDeliveryToken iMqttDeliveryToken) {
        if (null == iMqttDeliveryToken.getUserContext())
            return;
        FutureCallback<IMqttDeliveryToken> sendingCallback = (FutureCallback<IMqttDeliveryToken>) iMqttDeliveryToken.getUserContext();
        sendingCallback.onSuccess(iMqttDeliveryToken);
    }

    /**
     * 发送消息失败时调用
     *
     * @param iMqttDeliveryToken
     * @param exception
     */
    protected void sendFailure(IMqttDeliveryToken iMqttDeliveryToken, Throwable exception) {
        if (null == iMqttDeliveryToken.getUserContext())
            return;
        FutureCallback<IMqttDeliveryToken> sendingCallback = (FutureCallback<IMqttDeliveryToken>) iMqttDeliveryToken.getUserContext();
        sendingCallback.onFailure(new PahoFutureCallBackException(iMqttDeliveryToken, exception));
    }


    //~~ for subscriptions
    //~~
    //~~
    //~~

    private ConcurrentMap<String, Subscriber> subscriptions = Maps.newConcurrentMap();

    private ReentrantLock subscriptionsLock = new ReentrantLock();

    private IMqttMessageListener defaultMessageListener;

    public final class Subscriber {

        private final String topic;

        private final int qos;

        private final IMqttMessageListener messageListener;

        private volatile boolean running;

        private volatile Date runningStarted;

        private volatile Date runningStopped;

        private volatile Throwable errorCause;

        private void error(Throwable cause) {
            running = false;
            runningStarted = null;
            runningStopped = new Date();
            errorCause = cause;
        }

        private void start() {
            running = true;
            runningStarted = new Date();
        }

        private void stop() {
            running = false;
            runningStopped = new Date();
        }

        public Subscriber(String topic, int qos, IMqttMessageListener messageListener) {
            this.topic = topic;
            this.qos = qos;
            this.messageListener = messageListener;
        }

        public String getTopic() {
            return topic;
        }

        public int getQos() {
            return qos;
        }

        public IMqttMessageListener getMessageListener() {
            return messageListener;
        }

        public boolean isRunning() {
            return running;
        }

        public Date getRunningStarted() {
            return runningStarted;
        }

        public Date getRunningStopped() {
            return runningStopped;
        }

        public Throwable getErrorCause() {
            return errorCause;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
        }
    }


    public void addSubscriber(Subscriber subscriber) {
        Assert.notNull(subscriber, "subscriber must not be null.");
        Assert.isNull(subscriptions.putIfAbsent(subscriber.getTopic(), subscriber), "subscriber already registered fot topic: " + subscriber.getTopic());
        logger.info("new subscriber {}:qos{} added.", subscriber.getTopic(), subscriber.getQos());
    }

    @ManagedOperation
    public void removeSubscriber(String topic) {
        Assert.hasText(topic, "topic must not empty.");
        Subscriber subscriber = getSubscriber(topic);
        synchronized (subscriber) {
            Assert.isTrue(!subscriber.isRunning(), "subscriber running. stop subscriber first: " + topic);
        }
        subscriptions.remove(topic);
        logger.info("subscriber {}:qos{} removed.", subscriber.getTopic(), subscriber.getQos());
    }


    @ManagedOperation
    public int getNumberOfSubscriptions() {
        return subscriptions.size();
    }


    @ManagedOperation
    public String[] getTopics() {
        return subscriptions.keySet().toArray(new String[subscriptions.size()]);
    }

    @ManagedOperation
    public Subscriber getSubscriber(String topic) {
        Subscriber subscriber = subscriptions.get(topic);
        Assert.notNull(subscriber, "no topic subscriber found: " + topic);
        return subscriber;
    }


    protected void initSubscriptions() throws MqttException {
        initPahoClientIfNecessary(null);
        subscriptionsLock.lock();
        try {
            for (String topic : getTopics())
                startSubscriber(topic);
        } finally {
            subscriptionsLock.unlock();
        }
    }

    protected void stoppedSubscriptions() throws MqttException {
        logger.debug("stopping all subscribers...");
        if (!isConnected())
            throw new IllegalStateException("client not connected.");
        subscriptionsLock.lock();
        try {
            for (String topic : getTopics())
                stopSubscriber(topic);
        } finally {
            subscriptionsLock.unlock();
        }
    }

    @ManagedOperation
    public void startSubscriber(String topic) throws MqttException {
        initPahoClientIfNecessary(null);
        Subscriber subscriber = getSubscriber(topic);
        synchronized (subscriber) {
            if (subscriber.running) {
                logger.warn("subscriber already running: " + topic);
                return;
            }
            IMqttMessageListener applyListener = subscriber.getMessageListener() != null ? subscriber.getMessageListener() : defaultMessageListener;
            checkMessageListener(applyListener);
            if (applyListener instanceof IMqttAcknowledgeableMessageClientListener) {
                //set new client every time on startSubscriber. client may refreshed when reconnected.
                ((IMqttAcknowledgeableMessageClientListener) applyListener).setClient(client);
                postIMqttAcknowledgeableMessageClientListener((IMqttAcknowledgeableMessageClientListener) applyListener);
            }
            IMqttToken subscribingFuture = client.subscribe(subscriber.getTopic(), subscriber.getQos(), subscriber, subscribingListener, applyListener);
            subscribingFuture.waitForCompletion();
            subscriber.start();
        }
    }

    protected void postIMqttAcknowledgeableMessageClientListener(IMqttAcknowledgeableMessageClientListener iMqttAcknowledgeableMessageClientListener) {

    }

    @ManagedOperation
    public void stopSubscriber(String topic) throws MqttException {
        initPahoClientIfNecessary(null);
        Subscriber subscriber = getSubscriber(topic);
        synchronized (subscriber) {
            if (!subscriber.running) {
                logger.warn("subscriber not running or already stopped: " + topic);
                return;
            }
            client.unsubscribe(topic, subscriber, subscribingListener).waitForCompletion();
            subscriber.stop();
        }
    }


    private final IMqttActionListener subscribingListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            boolean unsbuscribe = asyncActionToken.getResponse() instanceof MqttUnsubAck;

            Subscriber subscriber = (Subscriber) asyncActionToken.getUserContext();
            if (!unsbuscribe)
                logger.info("successfully start subscribe: {} ", subscriber);
            else
                logger.info("successfully stop subscribe: {}", subscriber);
            try {
                subscribed(asyncActionToken, subscriber);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            boolean unsbuscribe = asyncActionToken.getResponse() instanceof MqttUnsubAck;

            Subscriber subscriber = (Subscriber) asyncActionToken.getUserContext();
            subscriber.error(exception);
            if (!unsbuscribe)
                logger.warn("failure to start subscribe: " + subscriber, exception);
            else
                logger.warn("failure to stop subscribe: " + subscriber, exception);
            try {
                subscribFailure(asyncActionToken, exception, subscriber);
            } catch (Exception e) {
                logger.error("an error occurring on callback-method.", e);
            }
        }
    };


    /**
     * 加入订阅时调用
     *
     * @param asyncActionToken
     */
    protected void subscribed(IMqttToken asyncActionToken, Subscriber subscriber) {
    }

    /**
     * 取消订阅时调用
     *
     * @param asyncActionToken
     * @param exception
     */
    protected void subscribFailure(IMqttToken asyncActionToken, Throwable exception, Subscriber subscriber) {
    }

    public void setDefaultMessageListener(IMqttMessageListener defaultMessageListener) {
        checkMessageListener(defaultMessageListener);
        this.defaultMessageListener = defaultMessageListener;
    }

    protected void checkMessageListener(IMqttMessageListener iMqttMessageListener) {
        Assert.notNull(iMqttMessageListener, "iMqttMessageListener must not be null.");
        if (iMqttMessageListener instanceof IMqttAcknowledgeableMessageClientListener && autoAck)
            logger.warn("registered messageListener[{}] is AcknowledgeableMqttMessage but mqtt.client.auto-ack is set to false. it may cause acknowledge() work error on ImqttMessage.", iMqttMessageListener.getClass().getName());
        if (!(iMqttMessageListener instanceof IMqttAcknowledgeableMessageClientListener) && (!autoAck))
            throw new IllegalStateException("registered message [" + iMqttMessageListener.getClass().getName() + "] not is AcknowledgeableMqttMessage. when mqtt.client.auto-ack is set false.");
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public void setAutoClientId(boolean autoClientId) {
        this.autoClientId = autoClientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setStartSubscriptionsOnConnected(boolean startSubscriptionsOnConnected) {
        this.startSubscriptionsOnConnected = startSubscriptionsOnConnected;
    }

    public void setReconnectFaultDelayRatio(double reconnectFaultDelayRatio) {
        this.reconnectFaultDelayRatio = reconnectFaultDelayRatio;
    }

    public void setReconnectDelay(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    protected boolean isAutoClientId() {
        return autoClientId;
    }

    protected boolean isAutoReconnect() {
        return autoReconnect;
    }

    public boolean isStarted() {
        return started.get();
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }


    public void setAutoAck(boolean autoAck) {
        this.autoAck = autoAck;
    }


    public void setCheckConnectedOnPublishing(boolean checkConnectedOnPublishing) {
        this.checkConnectedOnPublishing = checkConnectedOnPublishing;
    }

    @Value("#{ @environment['mqtt.client.auto-startup'] ?: false}")

    private boolean autoStartup;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(null != pahoClientFactory, "pahoClientFactory must not be null.");
        Assert.state(null != this.getUrl() || null != getMqttConnectOptions().getServerURIs(),
                "url or MqttConnectOptions.serverURIs must least specify one.");
        if (clientId == null && !autoClientId)
            throw new IllegalStateException("clientId must bot be null. or set generateClientId as true.");
        clientId = null != clientId ? clientId : MqttAsyncClient.generateClientId();
        mqttConnectOptions.setAutomaticReconnect(false);//禁用paho默认重连机制
        if (autoStartup)
            start();
    }


    @Override
    public void destroy() throws Exception {
        try {
            stop();
        } finally {
            destroySchedulers();
        }
    }
}
