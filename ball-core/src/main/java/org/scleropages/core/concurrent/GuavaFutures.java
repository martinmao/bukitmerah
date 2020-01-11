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
package org.scleropages.core.concurrent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;


/**
 * 基于guava future 实现一些常用并发交互模型
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class GuavaFutures {


    /**
     * create new {@link IdObservable}
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> IdObservable<K, V> newNormalIdObservable() {
        return newIdObservable(2000, 30, TimeUnit.SECONDS);
    }


    /**
     * create new {@link IdObservable}
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> IdObservable<K, V> newSlowIdObservable() {
        return newIdObservable(5000, 1, TimeUnit.MINUTES);
    }


    /**
     * create new {@link IdObservable}
     *
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> IdObservable<K, V> newFastIdObservable() {
        return newIdObservable(500, 10, TimeUnit.SECONDS);
    }


    /**
     * create new {@link IdObservable}
     *
     * @param subscriptions
     * @param duration
     * @param timeUnit
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> IdObservable<K, V> newIdObservable(long subscriptions, long duration, TimeUnit timeUnit) {
        return new IdObservable<>(
                CacheBuilder.newBuilder().maximumSize(subscriptions).expireAfterWrite(duration, timeUnit));
    }

    /**
     * create new {@link IdObservable} 使用指定的缓存设置来保存subscriptios
     *
     * @param cacheBuilder
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    public static <K, V> IdObservable<K, V> newIdObservable(CacheBuilder cacheBuilder) {
        return new IdObservable<>(cacheBuilder);
    }

    /**
     * 并发环境事件订阅器，订阅端基于事先约定的事件ID执行事件订阅(subscribe)，通知线程在事件产生后通知订阅端（通过done 或
     * fault），订阅端可以通过 {@link FutureCallback}异步订阅事件，也可以采用阻塞的方式(waitDone)订阅事件.<br>
     * <p>
     * 用例举例:<br>
     * <p>
     * 在异步rpc类应用客户端程序中，往往需要将请求/响应消息进行关联。请求发送线程在请求发送后可以根据请求id(常称为correlationId)
     * 等待响应消息（subscribe）<br>
     * 响应接受线程收到服务端消息时执行done通知请求发送端线程，通过请求id将这一组请求响应消息进行关联<br>
     *
     * <p>
     * <b>NOTE:<br>
     * 默认的（waitDone,fault,done）都会自动清除subscription，即一次订阅一次消费，这就要求确保
     * subscribe--->waitDone---->done/fault
     * 的交互顺序，重载版本可以通过参数设置是否清除，如果不自动清除,则可作为一组事件流(不会缓存之前的事件),但必须确保手动removeSubscription，否则清除动作由缓存策略决定<br>
     * <p>
     * 当前实现使用 guava cache来 缓存subscriptions来尽量限定可用的内存（避免oom），需结合实际情况合理的设置
     * {@link CacheBuilder}
     */
    public static class IdObservable<K, V> {

        private final Cache<K, SettableFuture<V>> subscriptions;

        /**
         * 在 {@link CacheBuilder#recordStats()}打开的情况下，可以获取缓存状态
         *
         * <br>
         * Returns a current snapshot of this cache's cumulative statistics. All
         * stats are initialized to zero, and are monotonically increasing over
         * the lifetime of the cache.
         *
         * @return
         */
        public CacheStats stats() {
            return subscriptions.stats();
        }

        /**
         * Returns the approximate number of subscriptions.
         *
         * @return
         */
        public long subscriptions() {
            return subscriptions.size();
        }

        /**
         * Discards all subscriptions in the cache.
         */
        public void removeSubscriptions() {
            subscriptions.invalidateAll();
        }

        /**
         * Discards subscription by given key.
         *
         * @param id
         */
        public void removeSubscription(K id) {
            subscriptions.invalidate(id);
        }

        /**
         * guava并不会周期性检测清理缓存失效数据，仅是在少量写操作或者偶尔读操作进行，要实现周期性清理请以固定的时间间隔调用cleanUp
         */
        public void cleanUp() {
            subscriptions.cleanUp();
        }

        /**
         * 使用指定的缓存设置来保存subscriptions
         *
         * @param cacheBuilder
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public IdObservable(CacheBuilder cacheBuilder) {
            cacheBuilder.removalListener(new RemovalListener<K, SettableFuture<V>>() {
                @Override
                public void onRemoval(RemovalNotification<K, SettableFuture<V>> notification) {
                    if (notification.wasEvicted())
                        notification.getValue().setException(new IllegalStateException(
                                "Subscription timeout. That was evicted by guava cache of given id: "
                                        + notification.getKey() + ". You can change guava cache eviction settings: "
                                        + "size-based eviction(increase cache size)、"
                                        + "timed eviction(increase expire time)、"
                                        + "reference-based eviction(weak/soft values or weak key)."));
                }
            });
            this.subscriptions = cacheBuilder.build();
        }

        /**
         * 注册一个事件订阅
         *
         * @param id
         * @return
         */
        public void subscribe(K id) {
            subscribe(id, null);
        }

        /**
         * 注册一个事件订阅并设置 {@link FutureCallback}
         * 来实现异步通知（通知动作在done方法的执行线程执行，即这会影响事件发送端执行效率）
         *
         * @param id
         * @param futureCallback
         * @return
         */
        public void subscribe(K id, final FutureCallback<V> futureCallback) {
            subscribe(id, futureCallback, null);
        }

        /**
         * 注册一个事件订阅并设置 {@link FutureCallback} 来实现异步通知（通知动作在指定的
         * {@link Executor}中执行）
         *
         * @param id
         * @param futureCallback
         * @param executor
         * @return
         */
        public void subscribe(K id, final FutureCallback<V> futureCallback, final Executor executor) {
            try {
                subscriptions.get(id, new Callable<SettableFuture<V>>() {
                    @Override
                    public SettableFuture<V> call() throws Exception {
                        SettableFuture<V> settableFuture = SettableFuture.create();
                        if (null != futureCallback)
                            if (null != executor)
                                Futures.addCallback(settableFuture, futureCallback, executor);
                            else
                                Futures.addCallback(settableFuture, futureCallback, new Executor() {
                                    @Override
                                    public void execute(Runnable command) {
                                        command.run();
                                    }
                                });
                        return settableFuture;
                    }
                });
            } catch (ExecutionException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * 产生事件
         *
         * @param id
         * @param v
         */
        public void done(K id, V v) {
            done(id, v, true);
        }

        public void done(K id, V v, boolean removeSubscription) {
            getSubscriptionIfPresent(id).set(v);
            if (removeSubscription)
                removeSubscription(id);
        }

        /**
         * 产生事件失败，发送异常
         *
         * @param id
         * @param e
         */
        public void fault(K id, Throwable e) {
            fault(id, e, true);
        }

        /**
         * 产生事件失败，发送异常
         *
         * @param id
         * @param e
         */
        public void fault(K id, Throwable e, boolean removeSubscription) {
            getSubscriptionIfPresent(id).setException(e);
            if (removeSubscription)
                removeSubscription(id);
        }

        /**
         * 阻塞等待事件
         *
         * @param id
         * @param timeout
         * @param timeUnit
         * @return
         * @throws InterruptedException
         * @throws TimeoutException
         * @throws ExecutionException
         */
        public V waitDone(K id, long timeout, TimeUnit timeUnit)
                throws InterruptedException, TimeoutException, ExecutionException {
            return waitDone(id, timeout, timeUnit, true);
        }

        /**
         * 阻塞等待事件
         *
         * @param id
         * @param timeout
         * @param timeUnit
         * @return
         * @throws InterruptedException
         * @throws TimeoutException
         * @throws ExecutionException
         */
        public V waitDone(K id, long timeout, TimeUnit timeUnit, boolean removeSubscription)
                throws InterruptedException, TimeoutException, ExecutionException {
            boolean removeFlag = true;
            SettableFuture<V> settableFuture = getSubscriptionIfPresent(id);
            try {
                if (-1 == timeout)
                    return settableFuture.get();
                return settableFuture.get(timeout, timeUnit);
            } catch (TimeoutException | InterruptedException e) {
                // 超时以及线程中断请求，不应该删除订阅，可能下次继续订阅
                removeFlag = false;
                throw e;
            } finally {
                if (removeSubscription && removeFlag)
                    removeSubscription(id);
            }
        }


        /**
         * 等待中的订阅
         *
         * @param
         * @return
         */
        public Collection<K> waitings() {
            List<K> waitings = Lists.newArrayList();
            synchronized (subscriptions) {
                subscriptions.asMap().forEach((id, settableFuture) -> {
                    if (!(settableFuture.isDone() || settableFuture.isCancelled())) {
                        try {
                            waitings.add(id);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                });
                return waitings;
            }
        }

        /**
         * 成功的订阅结果
         *
         * @param remove
         * @return
         */
        public Map<K, V> dones(boolean remove) {
            Map<K, V> doneValues = Maps.newHashMap();
            synchronized (subscriptions) {
                subscriptions.asMap().forEach((id, settableFuture) -> {
                    if (settableFuture.isDone()) {
                        try {
                            doneValues.put(id, settableFuture.get());
                            if (remove)
                                removeSubscription(id);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                });
                return doneValues;
            }
        }

        /**
         * 失败的订阅结果
         *
         * @param remove
         * @return
         */
        public Map<K, Exception> faults(boolean remove) {
            Map<K, Exception> faultValues = Maps.newHashMap();
            synchronized (subscriptions) {
                subscriptions.asMap().forEach((id, settableFuture) -> {
                    if (settableFuture.isDone() || settableFuture.isCancelled()) {
                        try {
                            settableFuture.get();
                        } catch (Exception e) {
                            faultValues.put(id, e);
                            if (remove)
                                removeSubscription(id);
                        }
                    }
                });
                return faultValues;
            }
        }

        private SettableFuture<V> getSubscriptionIfPresent(K id) {
            SettableFuture<V> settableFuture = subscriptions.getIfPresent(id);
            Assert.notNull(settableFuture, "No subscription found by given id: " + id);
            return settableFuture;
        }

    }

    public static void main(String[] args) throws InterruptedException, TimeoutException, ExecutionException {

        final Thread main = Thread.currentThread();

        final IdObservable<String, String> idObservable = newIdObservable(
                CacheBuilder.newBuilder().maximumSize(2000).expireAfterWrite(10, TimeUnit.MINUTES));
        idObservable.subscribe("0");
        idObservable.subscribe("1", new FutureCallback<String>() {

            @Override
            public void onSuccess(String result) {
                System.out.println(Thread.currentThread().getName() + ": " + result);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(1);
        idObservable.subscribe("2", new FutureCallback<String>() {

            @Override
            public void onSuccess(String result) {
                System.out.println(Thread.currentThread().getName() + ": " + result);
                LockSupport.unpark(main);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, executor);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + ": 产生事件");
                try {
                    TimeUnit.SECONDS.sleep(1);
                    idObservable.done("0", "xxxxx");
                    TimeUnit.SECONDS.sleep(1);
                    idObservable.done("1", "xxxx");
                    TimeUnit.SECONDS.sleep(1);
                    idObservable.done("2", "xxx");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        System.out.println(Thread.currentThread().getName() + ": " + idObservable.waitDone("0", 3, TimeUnit.SECONDS));

        LockSupport.park();
        System.out.println(idObservable.subscriptions());
        executor.shutdownNow();
    }

    public interface FutureWatcher<V> {
        void watch(SettableFuture<V> future, Object watchContext);
    }

    public static <V> Future<V> createWatchedFuture(Executor executor, FutureWatcher<V> watcher, long tryInterval, int tryMax, Object watchContext) {
        SettableFuture future = SettableFuture.create();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int tryNow = 0;
                while (true) {
                    try {
                        if (Thread.interrupted())
                            throw new InterruptedException("interrupted by other process.");
                        watcher.watch(future, watchContext);
                        if (future.isDone())
                            break;
                        if (++tryNow > tryMax)
                            future.setException(new IllegalStateException("max try [" + tryMax + "] failure reached."));
                        Thread.sleep(tryInterval);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();// re-flag interrupt status.
                    }
                }
            }
        };
        executor.execute(runnable);
        return future;
    }
}
