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

import org.springframework.util.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务失败或超时后，任务延期执行周期以固定倍数增长，直至增长到maxDelay时，不在发生变化，一旦任务执行正常，则重置初始delay
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ExponentialBackoffTask extends SchedulingTask {


    private final long delay;

    private final long maxDelay;

    private final double growthRatio;

    private AtomicLong currentDelay;

    /**
     * @param taskName      任务名称
     * @param task          目标任务
     * @param taskTimeout   目标任务超时时间
     * @param timeUnit      时间单位
     * @param taskScheduler 调度器
     * @param taskExecutor  目标任务执行器
     * @param delay         初始delay
     * @param maxDelay      最大delay
     * @param growthRatio   delay增长比例(最小1，不增长)
     */
    public ExponentialBackoffTask(String taskName, Runnable task, long taskTimeout, long delay, long maxDelay, double growthRatio, TimeUnit timeUnit, ScheduledExecutorService taskScheduler, ExecutorService taskExecutor) {
        super(taskName, task, taskTimeout, timeUnit, taskScheduler, taskExecutor);
        this.delay = delay;
        this.maxDelay = maxDelay;
        Assert.isTrue(growthRatio > 1, "growthRatio must greater than 1");
        this.growthRatio = growthRatio;
        currentDelay = new AtomicLong(delay);
    }

    @Override
    protected boolean onSuccess() {
        currentDelay.set(delay);
        return super.onSuccess();
    }

    @Override
    protected boolean onTimeout(TimeoutException e, ExecutorService taskExecutor) {
        computeAndSetCurrentDelay();
        return super.onTimeout(e, taskExecutor);
    }

    @Override
    protected boolean onOtherError(Exception e, ExecutorService taskExecutor, ScheduledExecutorService taskScheduler) {
        computeAndSetCurrentDelay();
        return super.onOtherError(e, taskExecutor, taskScheduler);
    }

    protected void computeAndSetCurrentDelay() {
        long delay = currentDelay.get();
        long newDelay = (long) Math.min(maxDelay, delay * growthRatio);
        currentDelay.compareAndSet(delay, newDelay);
    }

    @Override
    protected long delayNext() {
        return currentDelay.get();
    }

    @Override
    protected void reset() {
        currentDelay.set(delay);
    }
}
