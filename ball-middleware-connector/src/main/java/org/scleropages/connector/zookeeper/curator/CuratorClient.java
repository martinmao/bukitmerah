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
package org.scleropages.connector.zookeeper.curator;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.CreateBuilderMain;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.EphemeralType;
import org.scleropages.connector.zookeeper.StateListener;
import org.scleropages.connector.zookeeper.ZNode;
import org.scleropages.connector.zookeeper.ZNodeListener;
import org.scleropages.connector.zookeeper.ZnodeImpl;
import org.scleropages.connector.zookeeper.ZookeeperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.*;
import static org.scleropages.connector.zookeeper.ZNodeListener.*;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
@ManagedResource(objectName = "org.scleropages.connector:name=zookeeper", description = "zookeeper client instance.")
public class CuratorClient implements ZookeeperClient, InitializingBean, ApplicationContextAware, DisposableBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("#{ @environment['zookeeper.client.auto-startup'] ?: false}")
    private boolean autoStartup;

    @Value("#{ @environment['zookeeper.client.auto-register-state-listener'] ?: false}")
    private boolean autoRegisterStateListener;

    @Value("#{ @environment['zookeeper.client.auto-register-znode-listener'] ?: false}")
    private boolean autoRegisterZNodeListener;

    private final Set<StateListener> stateListeners = new CopyOnWriteArraySet<>();

    /**
     * curator尚未start，缓存addZnodeListener创建的TreeCache，curator.start()后逐一启动TreeCache.start()
     */
    private final Set<TreeCache> delayStartTreeCaches = Collections.synchronizedSet(new HashSet<>());

    /**
     * watch路径与TreeCache绑定关系.在addZnodeListener时，如果已经watch的TreeCache存在，则直接加入，不重复创建
     */
    private ConcurrentMap<String, TreeCache> treeCaches = new ConcurrentHashMap<>();

    /**
     * 建立path->ZNodeListener->TreeCacheListener关系，用于 removeZnodeListener时，根据path->ZNodeListener->TreeCacheListener定位，从而删除TreeCache与TreeCacheListener绑定关系
     * 并在TreeCache没有任何绑定的TreeCacheListener时能够销毁.
     */
    private ConcurrentMap<String, ConcurrentMap<ZNodeListener, TreeCacheListener>> znodeListeners = new ConcurrentHashMap<>();


    protected final CuratorFramework curator;

    private final CuratorOptions curatorOptions;


    public CuratorClient(CuratorFramework curator, CuratorOptions curatorOption) {
        this.curator = curator;
        this.curatorOptions = curatorOption;
    }


    /**
     * @param path       支持递归创建
     * @param createMode 节点类型
     * @param data       节点数据
     * @param ttl        如果节点类型支持ttl，设置ttl时间
     * @param override   已存在覆盖数据
     * @param version    acid写入 -1，不进行version检查
     * @throws Exception
     */
    public void create(String path, CreateMode createMode, byte[] data, long ttl, boolean override, int version) throws Exception {
        CreateBuilderMain builder = curator.create();
        if (override)
            ((CreateBuilder) builder).orSetData(version);
        if (createMode.isTTL())
            ((CreateBuilder) builder).withTtl(ttl);
        if (createMode.isContainer() || createMode.isEphemeral() || createMode.isTTL()) {
            builder.creatingParentContainersIfNeeded().withMode(createMode).forPath(path, null != data ? data : new byte[0]);
        } else
            builder.creatingParentsIfNeeded().withMode(createMode).forPath(path, null != data ? data : new byte[0]);
    }

    @Override
    @ManagedOperation
    public void create(String path, CreateMode createMode, byte[] data) throws Exception {
        create(path, createMode, data, EphemeralType.MAX_TTL, false, -1);
    }

    @Override
    @ManagedOperation
    public void delete(String path, int version) throws Exception {
        curator.delete().guaranteed().deletingChildrenIfNeeded().withVersion(version).forPath(path);
    }

    @Override
    @ManagedOperation
    public Stat get(String path) throws Exception {
        return curator.checkExists().forPath(path);
    }

    @Override
    @ManagedOperation
    public void set(String path, int version, byte[] data) throws Exception {
        curator.setData().withVersion(version).forPath(path, data);
    }

    @Override
    @ManagedOperation
    public byte[] getData(String path) throws Exception {
        return curator.getData().forPath(path);
    }

    @Override
    @ManagedOperation
    public boolean exists(String path) throws Exception {
        return get(path) != null;
    }

    @Override
    @ManagedOperation
    public List<String> getChildren(String path) throws Exception {
        return curator.getChildren().forPath(path);
    }

    @Override
    public void addZNodeListener(String path, ZNodeListener listener) throws Exception {

        final String watchedPath = path != null ? path : listener.watchedPath();
        Assert.hasText(watchedPath, "path must not not empty (or provided by ZNodeListener).");


        TreeCache cache = treeCaches.computeIfAbsent(watchedPath, s -> TreeCache.newBuilder(curator, watchedPath).
                setCacheData(true).setMaxDepth(listener.depth()).setCreateParentNodes(false).
                setDataIsCompressed(false).build());

        TreeCacheListener targetListener = (client, event) -> {

            TreeCacheEvent.Type eventType = event.getType();
            int znodeEvent = eventType == NODE_ADDED ? ZNODE_EVENT_ADDED : eventType == NODE_REMOVED ?
                    ZNODE_EVENT_REMOVED : eventType == NODE_UPDATED ? ZNODE_EVENT_UPDATED : -1;
            ChildData data = event.getData();
            if (znodeEvent != -1)//ignore CONNECTION_SUSPENDED,CONNECTION_RECONNECTED,CONNECTION_LOST,INITIALIZED..
                listener.changed(this, znodeEvent, new ZnodeImpl(data.getPath(), data.getStat(), data.getData()));
            else
                logger.info("watched path [{}] received event: {}. for remote:{}.", watchedPath, event, curator.getZookeeperClient().getCurrentConnectionString());
        };

        synchronized (this) {

            ConcurrentMap<ZNodeListener, TreeCacheListener> newAssociated = new ConcurrentHashMap<>();
            ConcurrentMap<ZNodeListener, TreeCacheListener> associated = znodeListeners.putIfAbsent(watchedPath, newAssociated);
            if (null == associated)
                associated = newAssociated;
            if (null != associated.putIfAbsent(listener, targetListener)) {
                throw new IllegalStateException(listener.getClass().getName() + " was already registered for path: " + watchedPath);
            }

            cache.getListenable().addListener(targetListener);

            if (isStarted())
                cache.start();
            else {
                logger.warn("curator not started. added ZNodeListener: {} watched [{}] will not effective now.", listener.getClass().getName(), watchedPath);
                delayStartTreeCaches.add(cache);
            }
        }

        logger.info("add zookeeper znode listener. {}:{}", watchedPath, listener.getClass().getName());
    }

    @Override
    public void removeZNodeListener(String path, ZNodeListener listener) {
        final String watchedPath = path != null ? path : listener.watchedPath();
        Assert.hasText(watchedPath, "path must not not empty (or provided by ZNodeListener).");
        synchronized (this) {
            TreeCache cache = treeCaches.get(watchedPath);

            Assert.state(null != cache, "no watched path registered: " + watchedPath);

            ConcurrentMap<ZNodeListener, TreeCacheListener> znodes = znodeListeners.get(watchedPath);
            Assert.state(null != znodes, "no watched path registered: " + watchedPath);

            TreeCacheListener targetListener = znodes.remove(listener);

            Assert.state(null != targetListener, "no such listener registered: " + listener.getClass().getName() + " watched path: " + watchedPath);

            cache.getListenable().removeListener(targetListener);
            logger.info("remove zookeeper znode listener. {}:{}", watchedPath, listener.getClass().getName());

            //如果指定watch path的znodes为空，则删除该路径下的ZnodeListeners以及tree cache.
            if (znodes.size() == 0) {
                logger.info("there no listeners watched path: {}. auto remove associated znodeListeners and treeCaches(and close)", watchedPath);
                znodeListeners.remove(watchedPath);
                treeCaches.remove(watchedPath);
                cache.close();
            }
        }
    }


    @Override
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
        logger.info("added state listener to zookeeper client: {}", listener.getClass().getName());
    }

    @Override
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
        logger.info("removed state listener to zookeeper client: {}", listener.getClass().getName());
    }


    @Override
    public <T> T getNativeClient() {
        return (T) this;
    }

    @ManagedOperation
    public String getCurrentConnected() {
        return curator.getZookeeperClient().getCurrentConnectionString();
    }

    @Override
    @ManagedOperation
    public boolean isConnected() {
        return curator.getZookeeperClient().isConnected();
    }


    @ManagedOperation
    public void close() {
        if (started.compareAndSet(true, false)) {
            treeCaches.forEach((s, treeCache) -> {
                try {
                    CloseableUtils.closeQuietly(treeCache);
                } catch (Exception e) {
                    logger.warn("failure to close TreeCache watched from: " + s, e);
                }
            });
            CloseableUtils.closeQuietly(curator);
        } else
            logger.warn("curator client already closed.");
    }


    private AtomicBoolean started = new AtomicBoolean(false);

    @ManagedOperation
    public final void start() {
        if (started.compareAndSet(false, true)) {
            logger.debug("attempting start zookeeper client...");
            Assert.state(curator.getState() == CuratorFrameworkState.LATENT, "curator state not is latent.");
            logger.debug("attempting registering StateListeners...");
            addStateListener(systemStateListener);
            registerStateListeners();
            curator.getConnectionStateListenable().addListener((client, state) -> {
                if (state == ConnectionState.LOST || state == ConnectionState.SUSPENDED) {
                    onStateChanged(this, StateListener.DISCONNECTED);
                } else if (state == ConnectionState.CONNECTED) {
                    onStateChanged(this, StateListener.CONNECTED);
                } else if (state == ConnectionState.RECONNECTED) {
                    onStateChanged(this, StateListener.RECONNECTED);
                }
            });

            curator.start();
            try {
                Assert.state(curator.blockUntilConnected(curatorOptions.getBlockUntilConnectedWaitMs(), TimeUnit.MILLISECONDS),
                        "timeout wait zookeeper client connected. " +
                                "you can set greater value for current [zookeeper.client.block-until-connected-wait-ms=" + curatorOptions.getBlockUntilConnectedWaitMs() + "].");
                logger.info("successfully connected zookeeper server: {}", curator.getZookeeperClient().getCurrentConnectionString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("connecting interrupted by other process.", e);
            }
            logger.debug("attempting registering ZNodeListeners...");
            registerZNodeListener();
            logger.debug("attempting start delay-start tree caches");
            delayStartTreeCaches.forEach(treeCache -> {
                try {
                    treeCache.start();
                    logger.debug("tree cache: {} started", treeCache);
                } catch (Exception e) {
                    throw new IllegalStateException("failure to start tree cache: " + treeCache, e);
                }
            });
            startInternal();
        } else
            logger.warn("curator client already started.");
    }


    protected void startInternal() {

    }

    private StateListener systemStateListener = new StateListener() {
    };

    @ManagedOperation
    public boolean isStarted() {
        return started.get();
    }


    @ManagedOperation
    public void startEmbeddedServer() throws Exception {
        applicationContext.getBean(TestingServer.class).start();
    }

    @ManagedOperation
    public void stopEmbeddedServer() throws Exception {
        applicationContext.getBean(TestingServer.class).close();
    }


    @Override
    @ManagedOperation
    public String[] getWatchedPaths() {
        return treeCaches.keySet().toArray(new String[0]);
    }

    @ManagedOperation
    @Override
    public ZNodeListener[] getZNodeListeners(String path) {
        return znodeListeners.get(path).keySet().toArray(new ZNodeListener[0]);
    }


    protected void registerStateListeners() {
        if (autoRegisterStateListener)
            applicationContext.getBeansOfType(StateListener.class).forEach((beanId, stateListener) -> {
                addStateListener(stateListener);
                logger.info("auto-registered zookeeper state listener: {}", stateListener.getClass().getName());
            });

    }

    protected void registerZNodeListener() {
        if (autoRegisterZNodeListener)
            applicationContext.getBeansOfType(ZNodeListener.class).forEach((beanId, zNodeListener) -> {
                String watchedPath = zNodeListener.watchedPath();
                Assert.hasText(watchedPath, "watchedPath not provided by: " + zNodeListener.getClass().getName());
                try {
                    addZNodeListener(zNodeListener.watchedPath(), zNodeListener);
                } catch (Exception e) {
                    logger.warn("failure to add znode listener: " + zNodeListener.getClass().getName() + ". watched path: " + watchedPath, e);
                }
                logger.info("auto-registered zookeeper znode listener. {}:{}", watchedPath, zNodeListener.getClass().getName());
            });
    }

    @Override
    public Set<StateListener> getStateListeners() {
        return stateListeners;
    }

    protected void onStateChanged(ZookeeperClient client, int state) {
        getStateListeners().forEach(stateListener -> {
            try {
                stateListener.stateChanged(client, state);
            } catch (Exception e) {
                logger.warn("Detected a failure execution on StateListener[" + stateListener.getClass().getName()
                        + "]. Do not throws out any exception in implementation of StateListener", e);
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (autoStartup)
            start();
    }


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public void setAutoRegisterStateListener(boolean autoRegisterStateListener) {
        this.autoRegisterStateListener = autoRegisterStateListener;
    }

    public void setAutoRegisterZNodeListener(boolean autoRegisterZNodeListener) {
        this.autoRegisterZNodeListener = autoRegisterZNodeListener;
    }

    @Override
    public void destroy() throws Exception {
        close();
    }


    /**
     * just for testing.
     */

    private final StateListener stateLoggingListener = new StateListener() {
        @Override
        public void disconnected(ZookeeperClient client) {
            logger.info("stateLoggingListener: disconnect from: {}", curator.getZookeeperClient().getCurrentConnectionString());
        }

        @Override
        public void connected(ZookeeperClient client) {
            logger.info("stateLoggingListener: connected from: {}", curator.getZookeeperClient().getCurrentConnectionString());

        }

        @Override
        public void reconnected(ZookeeperClient client) {
            logger.info("stateLoggingListener: reconnected from: {}", curator.getZookeeperClient().getCurrentConnectionString());
        }

        @Override
        public void startLead(ZookeeperClient client) {
            logger.info("stateLoggingListener: start lead from: {}", curator.getZookeeperClient().getCurrentConnectionString());
        }

        @Override
        public void stopLead(ZookeeperClient client) {
            logger.info("stateLoggingListener: stop lead from: {}", curator.getZookeeperClient().getCurrentConnectionString());
        }
    };
    private final ZNodeListener zNodeLoggingListener = new ZNodeListener() {
        @Override
        public String watchedPath() {
            return null;
        }

        @Override
        public int depth() {
            return 1000;
        }

        @Override
        public void changed(ZookeeperClient client, int event, ZNode node) {
            logger.info("{}: {}", eventDesc(event), ReflectionToStringBuilder.toString(node, ToStringStyle.JSON_STYLE));
        }
    };

    @ManagedOperation
    public void addZNodeLoggingListener(String path) throws Exception {
        addZNodeListener(path, zNodeLoggingListener);
    }

    @ManagedOperation
    public void removeZNodeLoggingListener(String path) {
        removeZNodeListener(path, zNodeLoggingListener);
    }

    @ManagedOperation
    public void addStateLoggingListener() {
        addStateListener(stateLoggingListener);
    }

    @ManagedOperation
    public void removeStateLoggingListener() {
        removeStateListener(stateLoggingListener);
    }

}
