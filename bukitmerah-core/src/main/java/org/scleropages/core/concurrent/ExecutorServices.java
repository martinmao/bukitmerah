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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class ExecutorServices {


    public static void gracefulShutdown(ExecutorService executorService, Logger logger, String tag, long awaitForceTermination) {
        if (null == executorService)
            return;
        logger.debug("Shutting...{}", tag);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(awaitForceTermination, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted flag
        }
        logger.info("successfully shutdown {}.", tag);
    }

    public static void createFixedExecutor(){

    }
}
