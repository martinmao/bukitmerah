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
package org.scleropages.crud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class InfrastructureLogger {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger("InfrastructureLogger");

    public static final void debug(Consumer<Logger> logger) {
        if (DEFAULT_LOGGER.isDebugEnabled())
            logger.accept(DEFAULT_LOGGER);
    }

    public static final void trace(Consumer<Logger> logger) {
        if (DEFAULT_LOGGER.isTraceEnabled())
            logger.accept(DEFAULT_LOGGER);
    }

    public static final void info(Consumer<Logger> logger) {
        if (DEFAULT_LOGGER.isInfoEnabled())
            logger.accept(DEFAULT_LOGGER);
    }

    public static final void error(Consumer<Logger> logger) {
        if (DEFAULT_LOGGER.isErrorEnabled())
            logger.accept(DEFAULT_LOGGER);
    }

    public static void main(String[] args) {
        InfrastructureLogger.info(logger -> logger.info("xxxx"));
    }
}
