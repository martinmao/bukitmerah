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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.framework.api.transaction.TransactionOp;
import org.apache.curator.framework.recipes.barriers.DistributedBarrier;
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.Lease;
import org.scleropages.connector.zookeeper.JoinLeaveBarrier;
import org.scleropages.connector.zookeeper.Latch;
import org.scleropages.connector.zookeeper.LatchOwner;
import org.scleropages.connector.zookeeper.LeaderParticipant;
import org.scleropages.connector.zookeeper.RecipesClient;
import org.scleropages.connector.zookeeper.ReentrantLock;
import org.scleropages.connector.zookeeper.ReentrantReadWriteLock;
import org.scleropages.connector.zookeeper.Semaphore;
import org.scleropages.connector.zookeeper.StateListener;
import org.scleropages.core.concurrent.Blocking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <pre>
 * advanced features lists:
 * {@link #sync(String, BackgroundCallback, Object)}
 * {@link #transaction(TransactionalOperations)}
 * {@link #reentrantLock(String)}
 * {@link #reentrantReadWriteLock(String)}
 * {@link #semaphore(String, int)}
 * {@link #startLeaderSelector(String, boolean)}
 * {@link #barrier(String, int)}
 * {@link #latch(String)}
 * {@link #latchOwner(String)}
 * </pre>
 *
 * @author<a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */


public class CuratorRecipesClient extends CuratorClient implements RecipesClient {

    public CuratorRecipesClient(CuratorFramework curator, CuratorOptions curatorOption) {
        super(curator, curatorOption);
    }

    @Override
    protected void startInternal() {
        super.startInternal();
        if (autoLeaderSelector) {
            startLeaderSelector(autoLeaderSelectorPath, autoLeaderSelectorRequeue);
        }
    }

    public void sync(String path, BackgroundCallback callback, Object callBackObject) throws Exception {
        curator.sync().inBackground(callback, callBackObject).forPath(path);
    }

    /**
     * <p>
     * Transactional/atomic operations.
     * </p>
     * <pre>
     *   ...=new TransactionalOperations() {
     *
     *          void executeInTransaction(List<CuratorOp> operations, TransactionOp transactionOp) throws Exception {
     *              operations.add(transactionOp.create().forPath("/a/path", "some data".getBytes()));
     *              operations.add(transactionOp.setData().forPath("/another/path", "other data".getBytes()));
     *              operations.add(transactionOp.delete().forPath("/yet/another/path"));}
     *      };
     *  </pre>
     */
    public List<CuratorTransactionResult> transaction(TransactionalOperations transactionalOperations) throws Exception {
        List<CuratorOp> transactionOps = Lists.newArrayList();
        transactionalOperations.executeInTransaction(transactionOps, curator.transactionOp());
        return curator.transaction().forOperations(transactionOps);
    }

    public abstract class TransactionalOperations {
        abstract void executeInTransaction(List<CuratorOp> operations, TransactionOp transactionOp) throws Exception;
    }


    /*监控列表,nativeLock-->LockState*/
    private final ConcurrentMap<Object, LockState> activeLockStates = Maps.newConcurrentMap();


    @Value("#{ @environment['zookeeper.client.auto-leader-selector'] ?: false}")
    private boolean autoLeaderSelector;
    @Value("#{ @environment['zookeeper.client.auto-leader-selector-path'] ?: null}")
    private String autoLeaderSelectorPath;
    @Value("#{ @environment['zookeeper.client.auto-leader-selector-requeue'] ?: true}")
    private boolean autoLeaderSelectorRequeue;

    @Value("#{ @environment['zookeeper.client.min-try-acquire-time-ms'] ?: 200}")
    private int minTryAcquireTimeMs;

    public static class LockState {

        public enum LockType {
            REENTRANT_LOCK, READ_LOCK, WRITE_LOCK, SEMAPHORE, LEADER_SELECTOR, JOIN_LEAVE_BARRIER, LATCH, LATCH_OWNER
        }

        private final String path;
        private final Object nativeLock;
        private final Date acquiredTime;
        private final LockType lockType;
        private Throwable errorReport;
        private int permits = 0;//记录重入次数或信号量permits

        public LockState(String path, Object nativeLock, LockType lockType) {
            this.path = path;
            this.nativeLock = nativeLock;
            this.lockType = lockType;
            this.acquiredTime = new Date();
        }

        private void setErrorReport(Throwable errorReport) {
            this.errorReport = errorReport;
        }

        private void incrPermits(int permits) {
            this.permits += permits;
        }

        private void decrPermits(int permits) {
            this.permits -= permits;
        }

        public boolean isNoPermits() {
            return this.permits == 0;
        }

        public int getPermits() {
            return permits;
        }

        public String getPath() {
            return path;
        }

        public Object getNativeLock() {
            return nativeLock;
        }

        public Date getAcquiredTime() {
            return acquiredTime;
        }

        public Throwable getErrorReport() {
            return errorReport;
        }

        public LockType getLockType() {
            return lockType;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
        }
    }

    protected LockState createIfAbsent(String path, Object nativeLock, LockState.LockType lockType) {
        LockState lockState = new LockState(path, nativeLock, lockType);
        LockState associated = activeLockStates.putIfAbsent(nativeLock, lockState);
        return associated != null ? associated : lockState;
    }

    @ManagedOperation
    public Collection<LockState> getActiveLockStates() {
        return activeLockStates.values();
    }

    /**
     * 重入锁
     * <pre>
     * ReentrantLock reentrantLock = reentrantLock("/xyz");
     * if (reentrantLock.acquire(Blocking.PERMANENT_BLOCKING)){
     *      try {
     *          //lock acquired do somethings.
     *      } finally {
     *          reentrantLock.release();
     *      }
     * }
     * else {
     *      // lock not acquired do somethings.
     * }
     * </pre>
     *
     * @param path path to locking.
     * @return
     * @throws Exception
     */
    @Override
    public ReentrantLock reentrantLock(String path) {
        return as(path, new InterProcessMutex(curator, path), LockState.LockType.REENTRANT_LOCK);
    }


    protected ReentrantLock as(String path, InterProcessMutex mutex, LockState.LockType lockType) {
        return new ReentrantLock() {
            @Override
            public boolean acquire(Blocking blocking) {
                boolean acquired = false;
                try {
                    if (Blocking.isBlocking(blocking)) {
                        if (Blocking.isPermanentBlocking(blocking)) {
                            mutex.acquire();
                            acquired = true;
                        } else
                            acquired = mutex.acquire(blocking.toMillis(), TimeUnit.MILLISECONDS);
                    } else
                        acquired = mutex.acquire(minTryAcquireTimeMs, TimeUnit.MILLISECONDS);//不支持tryAcquire，设置一个较短的时间作为通讯延迟.
                    return acquired;
                } catch (Exception e) {
                    throw new IllegalStateException("can not acquire lock by given path: " + path, e);
                } finally {
                    if (acquired) {
                        LockState associated = createIfAbsent(path, mutex, lockType);
                        synchronized (associated) {
                            associated.incrPermits(1);//可重入，后续acquire只更新permits
                        }
                        logger.debug("acquired lock by path: [{}]. state:[{}]", path, associated);
                    }
                }
            }

            @Override
            public void release() {
                LockState lockState = activeLockStates.get(mutex);
                Assert.notNull(lockState, "no lockState found by given path: " + path);
                try {
                    mutex.release();
                    synchronized (lockState) {
                        lockState.decrPermits(1);
                        if (lockState.isNoPermits() && (!mutex.isAcquiredInThisProcess()))//重入释放直至计数为0并且当前vm中没有任何线程持有该锁才清除 state.
                            activeLockStates.remove(mutex);
                    }
                    logger.debug("released lock by path: [{}]. state:[{}]", path, lockState);
                } catch (Exception e) {
//                    if (!(e instanceof IllegalMonitorStateException))//非锁状态原因，io等原因失败尝试恢复??
                    lockState.setErrorReport(e);
                    throw new IllegalStateException("can not release lock by given path: " + path, e);
                }
            }

            @Override
            public boolean isLockHeldByCurrent() {
                return mutex.isOwnedByCurrentThread();
            }

            @Override
            public Serializable[] participants() {
                try {
                    return mutex.getParticipantNodes().toArray(new String[0]);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    /**
     * 重入读写锁，支持锁降级
     *
     * @param path
     * @return
     */
    @Override
    public ReentrantReadWriteLock reentrantReadWriteLock(String path) {
        InterProcessReadWriteLock nativeLock = new InterProcessReadWriteLock(curator, path);
        return new ReentrantReadWriteLock() {
            @Override
            public ReentrantLock readLock() {
                return as(path, nativeLock.readLock(), LockState.LockType.READ_LOCK);
            }

            @Override
            public ReentrantLock writeLock() {
                return as(path, nativeLock.writeLock(), LockState.LockType.WRITE_LOCK);
            }
        };
    }

    /**
     * no internal checks are done to prevent
     * Process A acting as if there are 10 leases and Process B acting as if there are 20. Therefore,
     * make sure that all instances in all processes use the same numberOfLeases value.
     *
     * @param path
     * @param permits
     * @return
     */
    @Override
    public Semaphore semaphore(String path, int permits) {
        InterProcessSemaphoreV2 semaphore = new InterProcessSemaphoreV2(curator, path, permits);
        return new Semaphore() {


            private final List<Lease> leases = Lists.newArrayList();

            @Override
            public void acquire(Blocking blocking, int permit) {
                Collection<Lease> acquired = null;
                try {
                    if (Blocking.isBlocking(blocking)) {
                        if (Blocking.isPermanentBlocking(blocking)) {
                            acquired = semaphore.acquire(permit);
                        } else {
                            acquired = semaphore.acquire(permit, blocking.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    } else {
                        acquired = semaphore.acquire(permit, minTryAcquireTimeMs, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("can not acquire number of [" + permit + "] semaphore by given path: " + path, e);
                } finally {
                    if (null != acquired && acquired.size() > 0) {
                        int acquiredSize = acquired.size();
                        synchronized (this) {
                            leases.addAll(acquired);
                            LockState associated = createIfAbsent(path, semaphore, LockState.LockType.SEMAPHORE);
                            associated.incrPermits(acquiredSize);
                            logger.debug("acquired number of [{}] permits from semaphore by path: [{}]", acquiredSize, path);
                        }
                    }
                }
            }

            @Override
            public void acquire(Blocking blocking) {
                acquire(blocking, 1);
            }

            @Override
            public void release(int permit) {
                Assert.isTrue(leases.size() >= permit, "releasing permit greater than acquired.(" + permit + ">" + leases.size() + ")");
                synchronized (this) {
                    LockState lockState = activeLockStates.get(semaphore);
                    Assert.notNull(lockState, "no lockState found by given path: " + path);
                    try {
                        for (int i = 0; i < permit; i++) {
                            semaphore.returnLease(leases.remove(i));
                            lockState.decrPermits(1);
                        }
                        if (0 == leases.size()) {
                            activeLockStates.remove(lockState);
                        }
                    } catch (Exception e) {
                        lockState.setErrorReport(e);
                        throw new IllegalStateException("can not release semaphore by given path: " + path, e);
                    }
                }
            }

            @Override
            public void release() {
                release(1);
            }


            @Override
            public int availablePermits() {
                logger.warn("not supported. returned value is allPermits-holdPermits(by current thread).");
                return permits - leases.size();//不支持，仅返回总数-当前线程获取的数量....
            }

            @Override
            public Serializable[] participants() {
                try {
                    return semaphore.getParticipantNodes().toArray(new String[0]);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }


    @Override
    @ManagedOperation
    public void startLeaderSelector(String path, boolean autoRequeue) {
        Assert.hasText(path, "path must not be empty text.");
        LeaderSelector leaderSelector = new LeaderSelector(curator, path, new LeaderSelectorListenerAdapter() {
            @Override
            public void takeLeadership(CuratorFramework client) {
                logger.debug("start lead from remote: {}", getCurrentConnected());
                onStateChanged(getNativeClient(), StateListener.START_LEAD);
                LockState sync = getLeaderSelectorState(path);
                synchronized (sync) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        if (logger.isDebugEnabled()) {
                            logger.warn("interrupted by other process for WAITING(may connected state changed.).", e);
                        }
                        Thread.currentThread().interrupt();
                    } finally {
                        onStateChanged(getNativeClient(), StateListener.STOP_LEAD);
                        logger.debug("stop lead from remote: {}", getCurrentConnected());
                    }
                }
            }
        });
        if (autoRequeue)
            leaderSelector.autoRequeue();
        leaderSelector.setId(generateLeaderId());
        leaderSelector.start();
        createIfAbsent(path, leaderSelector, LockState.LockType.LEADER_SELECTOR);
        onStateChanged(getNativeClient(), StateListener.JOIN_LEADER_GROUP);
    }

    @Value("#{ @environment['spring.application.name'] ?: null}")
    private String applicationName;

    @Value("#{ @environment['server.host'] ?: null}")
    private String serverHost;

    @Value("#{ @environment['server.port'] ?: null}")
    private String serverPort;

    @Value("#{ @environment['zookeeper.client.instance-id.auto_incr'] ?: false}")
    private boolean instanceIdAutoIncr = false;

    private static final String INSTANCE_ID_SEPARATOR = ":";

    private AtomicInteger generateInstanceIdCounter = new AtomicInteger();

    protected String generateLeaderId() {
        Assert.isTrue(StringUtils.hasText(applicationName), "spring.application.name not configure.");
        Assert.isTrue(StringUtils.hasText(serverHost), "server.host not configure.");
        Assert.isTrue(StringUtils.hasText(serverPort), "server.port not configure.");
        String instanceId = applicationName + INSTANCE_ID_SEPARATOR + serverHost + INSTANCE_ID_SEPARATOR + serverPort;
        return instanceIdAutoIncr ? instanceId + INSTANCE_ID_SEPARATOR + generateInstanceIdCounter.incrementAndGet() : instanceId;
    }

    private class LeaderParticipantImpl implements LeaderParticipant {

        private final boolean isLeader;
        private final String application;
        private final String serverHost;
        private final String serverPort;
        private final String incrNum;
        private final String id;


        private LeaderParticipantImpl(Participant participant) {
            this(participant.getId(), participant.isLeader());
        }

        private LeaderParticipantImpl(String id, boolean isLeader) {
            this.isLeader = isLeader;
            this.id = id;
            StringTokenizer tokenizer = new StringTokenizer(id, INSTANCE_ID_SEPARATOR);
            application = tokenizer.nextToken();
            serverHost = tokenizer.nextToken();
            serverPort = tokenizer.nextToken();
            incrNum = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        }

        @Override
        public String getApplication() {
            return application;
        }

        @Override
        public String getServerHost() {
            return serverHost;
        }

        @Override
        public String getServerPort() {
            return serverPort;
        }

        @Override
        public String getIncrNum() {
            return incrNum;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isLeader() {
            return isLeader;
        }
    }


    @Override
    @ManagedOperation
    public LeaderParticipant getCurrentParticipant(String path) {
        LockState lockState = getLeaderSelectorState(path);
        LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();

        return new LeaderParticipantImpl(leaderSelector.getId(), leaderSelector.hasLeadership());
    }

    @Override
    @ManagedOperation
    public void startLead(String path) {
        LockState lockState = getLeaderSelectorState(path);
        synchronized (lockState) {
            LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();
            if (leaderSelector.hasLeadership()) {
                logger.warn("path: {} already has leader ship.", path);
                return;
            }
            leaderSelector.requeue();
        }
    }

    @Override
    @ManagedOperation
    public void stopLead(String path) {
        LockState lockState = getLeaderSelectorState(path);
        synchronized (lockState) {
            LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();
            Assert.state(leaderSelector.hasLeadership(), "path: " + path + " dose not has leader ship. can not be stopped.");
            lockState.notifyAll();
            leaderSelector.interruptLeadership();
        }
    }

    @Override
    @ManagedOperation
    public void stopLeaderSelector(String path) {
        LockState lockState = getLeaderSelectorState(path);
        synchronized (lockState) {
            LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();
            try {
                leaderSelector.close();
                activeLockStates.remove(leaderSelector);
                onStateChanged(getNativeClient(), StateListener.LEAVE_LEADER_GROUP);
            } catch (Exception e) {
                lockState.setErrorReport(e);
            }
        }
    }

    @Override
    @ManagedOperation
    public Collection<LeaderParticipant> leaderParticipants(String path) throws Exception {
        LockState lockState = getLeaderSelectorState(path);
        LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();
        Collection<Participant> participants = leaderSelector.getParticipants();
        Collection<LeaderParticipant> leaderParticipants = Lists.newArrayList();
        participants.forEach(participant -> {
            leaderParticipants.add(new LeaderParticipantImpl(participant));
        });
        return leaderParticipants;
    }

    @Override
    @ManagedOperation
    public LeaderParticipant getLeader(String path) throws Exception {
        LockState lockState = getLeaderSelectorState(path);
        LeaderSelector leaderSelector = (LeaderSelector) lockState.getNativeLock();
        return new LeaderParticipantImpl(leaderSelector.getLeader());
    }


    protected LockState getLeaderSelectorState(String path) {
        for (LockState lockState : activeLockStates.values()) {
            if (lockState.getPath().equals(path)) {
                Assert.state(lockState.getLockType().equals(LockState.LockType.LEADER_SELECTOR), "path: " + path + " not is a LEADER_SELECTOR lock type.");
                return lockState;
            }
        }
        throw new IllegalStateException("no leader-selector state found by given path: " + path);
    }

    @Override
    public JoinLeaveBarrier barrier(String path, int parties) {
        DistributedDoubleBarrier barrier = new DistributedDoubleBarrier(curator, path, parties);
        return new JoinLeaveBarrier() {
            @Override
            public boolean join(Blocking blocking) throws InterruptedException {
                boolean entered = false;
                try {
                    if (Blocking.isBlocking(blocking)) {
                        if (Blocking.isPermanentBlocking(blocking)) {
                            barrier.enter();
                            entered = true;
                        } else {
                            entered = barrier.enter(blocking.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    } else {
                        entered = barrier.enter(minTryAcquireTimeMs, TimeUnit.MILLISECONDS);
                    }
                    return entered;
                } catch (Exception e) {
                    logger.error("failure to enter barrier for path: " + path, e);
                    Thread.interrupted();
                    throw new InterruptedException(e.getMessage());
                } finally {
                    if (entered) {
                        LockState associated = createIfAbsent(path, barrier, LockState.LockType.JOIN_LEAVE_BARRIER);
                        synchronized (associated) {
                            associated.incrPermits(1);//+1标识进入enter状态
                        }
                        logger.debug("join barrier by path: [{}]. state:[{}]", path, associated);
                    }
                }
            }

            @Override
            public boolean leave(Blocking blocking) throws InterruptedException {
                boolean leaved = false;
                try {
                    if (Blocking.isBlocking(blocking)) {
                        if (Blocking.isPermanentBlocking(blocking)) {
                            barrier.leave();
                            leaved = true;
                        } else {
                            leaved = barrier.leave(blocking.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    } else {
                        leaved = barrier.leave(minTryAcquireTimeMs, TimeUnit.MILLISECONDS);
                    }
                    return leaved;
                } catch (Exception e) {
                    logger.error("failure to leave barrier for path: " + path, e);
                    Thread.interrupted();
                    throw new InterruptedException(e.getMessage());
                } finally {
                    if (leaved) {
                        LockState associated = activeLockStates.get(barrier);
                        synchronized (associated) {
                            associated.decrPermits(1);//-1标识进入leave状态
                            logger.debug("leave barrier by path: [{}]. state:[{}]", path, associated);
                            activeLockStates.remove(barrier);
                        }
                    }
                }
            }
        };
    }

    @Override
    public Latch latch(String path) {
        DistributedBarrier barrier = new DistributedBarrier(curator, path);
        return blocking -> {
            LockState associated = createIfAbsent(path, barrier, LockState.LockType.LATCH);
            logger.debug("joining... latch by path: [{}]. state:[{}]", path, associated);
            boolean done = false;
            try {
                if (Blocking.isBlocking(blocking)) {
                    if (Blocking.isPermanentBlocking(blocking)) {
                        barrier.waitOnBarrier();
                        done = true;
                    } else {
                        done = barrier.waitOnBarrier(blocking.toMillis(), TimeUnit.MILLISECONDS);
                    }
                } else {
                    done = barrier.waitOnBarrier(minTryAcquireTimeMs, TimeUnit.MILLISECONDS);
                }
                return done;
            } catch (Exception e) {
                logger.error("failure to join latch for latch.", e);
                Thread.interrupted();
                throw new InterruptedException(e.getMessage());
            } finally {
                if (done) {
                    activeLockStates.remove(barrier);
                    logger.debug("leave latch by path: [{}]. state:[{}]", path, associated);
                }
            }
        };
    }

    @Override
    public LatchOwner latchOwner(String path) {

        DistributedBarrier barrier = new DistributedBarrier(curator, path);

        return new LatchOwner() {
            @Override
            public void open() {
                try {
                    barrier.setBarrier();
                } catch (Exception e) {
                    throw new IllegalStateException("failure to open barrier for path: " + path, e);
                }
            }

            @Override
            public void down() {
                try {
                    barrier.removeBarrier();
                } catch (Exception e) {
                    throw new IllegalStateException("failure to down barrier for path: " + path, e);
                }
            }
        };
    }
}
