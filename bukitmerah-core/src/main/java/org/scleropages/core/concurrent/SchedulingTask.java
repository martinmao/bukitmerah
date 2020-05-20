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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务包装器，使任意 {@link Runnable} task 支持自调度 ,可由 {@link ScheduledExecutorService} 或 {@link java.util.Timer} 开启第一次调度（也可直接调用 {@link #run()},马上在当前线程开启调度，但会阻塞直至目标任务第一次运行完毕或超时），后续调度逻辑由该任务自身控制.
 * 该自调度任务支持目标任务设置超时时间，但须 {@link ExecutorService} 来提供支持.即目标任务实际运行在 ExecutorService中(用于阻塞超时判定)
 * 该自调度也支持开关用于恢复暂停自调度任务
 * NOTE,自调度任务由于支持暂停，在run执行时加入锁，故而无法并发执行
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SchedulingTask extends TimerTask {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String taskName;

    private final Runnable task;

    private final long taskTimeout;

    private final ScheduledExecutorService taskScheduler;

    private final ExecutorService taskExecutor;

    private volatile boolean isPaused = false;

    private final ReentrantLock pausedLock = new ReentrantLock();

    private final Condition paused = pausedLock.newCondition();

    private AtomicBoolean started = new AtomicBoolean(true);


    /**
     * @param taskName      任务名称
     * @param task          目标任务
     * @param taskTimeout   目标任务超时时间
     * @param timeUnit      时间单位
     * @param taskScheduler 调度器
     * @param taskExecutor  目标任务执行器
     */
    public SchedulingTask(String taskName, Runnable task, long taskTimeout, TimeUnit timeUnit, ScheduledExecutorService taskScheduler, ExecutorService taskExecutor) {
        Assert.isTrue(null != taskName && null != task && null != timeUnit && null != taskScheduler && null != taskExecutor, "all arguments is required.");
        this.taskName = taskName;
        this.task = task;
        this.taskTimeout = timeUnit.toMillis(taskTimeout);
        this.taskScheduler = taskScheduler;
        this.taskExecutor = taskExecutor;
    }


    @Override
    public void run() {
        pausedLock.lock();
        try {
            while (isPaused) paused.await();
            runInternal();
        } catch (InterruptedException e) {
            logger.error("interrupt by other process.", e);
            Thread.currentThread().interrupt();
        } finally {
            pausedLock.unlock();
        }
    }


    protected void runInternal() {
        Future future = null;
        boolean continueFlag = true;
        try {
            future = taskExecutor.submit(task);
            future.get(taskTimeout, TimeUnit.MILLISECONDS);
            continueFlag = onSuccess();
        } catch (RejectedExecutionException e) {
            logger.warn("rejected task[" + taskName + "]. may task executor is busy now.", e);
            continueFlag = onRejected(e, taskExecutor);
        } catch (TimeoutException e) {
            logger.warn("timeout while task[" + taskName + "] executing.", e);
            continueFlag = onTimeout(e, taskExecutor);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            continueFlag = onOtherError(e, taskExecutor, taskScheduler);
        } finally {
            if (null != future)
                future.cancel(true);
            if (started.get() && continueFlag) {
                schedulerNext(taskScheduler, taskExecutor);
            } else {
                logger.info("task[" + taskName + "] stopped. ");
            }
        }
    }

    /**
     * 仅用于运行中检查，初始状态就是true
     *
     * @return
     */
    public boolean isStarted() {
        return started.get();
    }


    public synchronized void stop() {
        if (started.compareAndSet(true, false)) {
            reset();
            logger.debug("stopping...task [{}].", taskName);
        }
    }

    public synchronized void reStart() {
        if (started.compareAndSet(false, true)) {
            logger.debug("task [{}] started and will run immediately.", taskName);
            run();
        }
    }

    public synchronized void reStartNext() {
        if (started.compareAndSet(false, true)) {
            logger.debug("task [{}] started and will run in next period.", taskName);
            schedulerNext(taskScheduler, taskExecutor);
        }
    }

    public void resume() {
        pausedLock.lock();
        try {
            isPaused = false;
            paused.signalAll();
            logger.debug("task [{}] resumed.", taskName);
        } finally {
            pausedLock.unlock();
        }
    }

    public void pause() {
        pausedLock.lock();
        try {
            isPaused = true;
            logger.debug("task [{}] paused.", taskName);
        } finally {
            pausedLock.unlock();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * task被拒绝时执行
     *
     * @param e
     * @param taskExecutor
     * @return true if continue.
     */
    protected boolean onRejected(RejectedExecutionException e, ExecutorService taskExecutor) {
        return !taskExecutor.isShutdown();
    }

    /**
     * 任务执行成功时调用
     *
     * @return true if continue.
     */
    protected boolean onSuccess() {
        return true;
    }

    /**
     * 其他错误
     *
     * @param e
     * @param taskExecutor
     * @return true if continue.
     */
    protected boolean onOtherError(Exception e, ExecutorService taskExecutor, ScheduledExecutorService taskScheduler) {
        return true;
    }

    /**
     * task 执行超时时执行
     *
     * @param e
     * @param taskExecutor
     * @return true if continue.
     */
    protected boolean onTimeout(TimeoutException e, ExecutorService taskExecutor) {
        return true;
    }

    /**
     * 进行下次调度
     *
     * @param taskScheduler
     */
    protected void schedulerNext(ScheduledExecutorService taskScheduler, ExecutorService taskExecutor) {
        if (!taskScheduler.isShutdown() && !taskExecutor.isShutdown()) {
            long delay = delayNext();
            taskScheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
            logger.debug("[{}] will fired after {} milliseconds", taskName, delay);
        } else
            logger.warn("taskScheduler or taskExecutor is shutdown. ignore next scheduler");
    }

    /**
     * 下次调度等待周期
     *
     * @return
     */
    protected abstract long delayNext();

    /**
     * stop 时重置
     */
    protected abstract void reset();
}
