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
package org.scleropages.crud.types;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Represents an named primitive type.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class NamedPrimitive<T> {

    private final String name;

    private final T value;


    /**
     * @param name  name of primitive
     * @param value value of primitive
     */
    public NamedPrimitive(String name, T value) {
        Assert.hasText(name, "name must not empty.");
        Assert.notNull(value, "value must not be null.");
        Assert.isTrue(ClassUtils.isPrimitiveOrWrapper(value.getClass()), "given value not an primitive type.");
        this.name = name;
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }
}
