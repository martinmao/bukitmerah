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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * referenced from guava {@link com.google.common.util.concurrent.FutureCallback}.
 * and change onFailure method declare add parameter result. used for some failure and returned some data.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface FutureCallback<V> extends com.google.common.util.concurrent.FutureCallback<V> {

    /**
     * Invoked when a {@code Future} computation fails or is canceled.
     *
     * <p>If the future's {@link Future#get() get} method throws an {@link
     * ExecutionException}, then the cause is passed to this method. Any other
     * thrown object is passed unaltered.
     */
    void onFailure(Throwable t, V result);

    @Override
    default void onFailure(Throwable t) {
        onFailure(t, null);
    }
}