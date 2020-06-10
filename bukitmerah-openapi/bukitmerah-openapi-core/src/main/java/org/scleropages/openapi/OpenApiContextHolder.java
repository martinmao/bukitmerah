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
package org.scleropages.openapi;

import org.springframework.util.Assert;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class OpenApiContextHolder {

    private static OpenApiContext OPEN_API_CONTEXT;

    public static OpenApiContext getOpenApiContext() {
        Assert.notNull(OPEN_API_CONTEXT, "no open api context found.");
        return OPEN_API_CONTEXT;
    }

    protected static void setOpenApiContext(OpenApiContext openApiContext) {
        Assert.notNull(openApiContext, "openApiContext must not be null.");
        Assert.isNull(OPEN_API_CONTEXT, "open api context already exists.");
        OPEN_API_CONTEXT = openApiContext;
    }
}
