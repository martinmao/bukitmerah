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

import java.util.concurrent.TimeUnit;

/**
 * 用于描述阻塞行为
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class Blocking {

    /**
     * 非阻塞行为
     */
    public static final Blocking NONE_BLOCKING = new Blocking(0, null);

    /**
     * 永久阻塞
     */
    public static final Blocking PERMANENT_BLOCKING = new Blocking(-1, null);

    private final long timeout;

    private final TimeUnit timeUnit;

    private Blocking(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = null != timeUnit ? timeUnit : TimeUnit.MILLISECONDS;
    }

    public static Blocking newInstance(long timeout, TimeUnit timeUnit) {
        if (0 == timeout || -1 > timeout)
            throw new IllegalArgumentException("timeout must -1 or greater than 0");
        if (-1 == timeout)
            return PERMANENT_BLOCKING;
        return new Blocking(timeout, timeUnit);
    }


    /**
     * 是否是非阻塞行为
     *
     * @param blocking
     * @return
     */
    public static boolean isNoneBlocking(Blocking blocking) {
        return null == blocking || NONE_BLOCKING.equals(blocking);
    }

    /**
     * 是否阻塞行为
     *
     * @param blocking
     * @return
     */
    public static boolean isBlocking(Blocking blocking) {
        return !isNoneBlocking(blocking);
    }

    /**
     * 是否是永久阻塞行为
     *
     * @param blocking
     * @return
     */
    public static boolean isPermanentBlocking(Blocking blocking) {
        return PERMANENT_BLOCKING.equals(blocking);
    }


    public long toMillis() {
        return timeUnit.toMillis(timeout);
    }

    public long toSeconds() {
        return timeUnit.toSeconds(timeout);
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
