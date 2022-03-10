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
package org.scleropages.crud.uid;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class GenericUidGenerator implements InitializingBean {


    private static final InheritableThreadLocal<Object> currentUidProviderKey =
            new InheritableThreadLocal<>();


    private ObjectProvider<UidGenerator> uidGeneratorsProvider;

    /**
     * mapped key object to uid generator
     */
    private Map<Object, UidGenerator> uidGenerators = Maps.newHashMap();

    public GenericUidGenerator(ObjectProvider<UidGenerator> uidGeneratorsProvider) {
        this.uidGeneratorsProvider = uidGeneratorsProvider;
    }


    public Serializable next() {
        return null;
    }

    public static Object getCurrentUidProviderKey() {
        return currentUidProviderKey.get();
    }

    public static void setCurrentUidProviderKey(Object key) {
        currentUidProviderKey.set(key);
    }

    public static void clear() {
        currentUidProviderKey.remove();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == uidGeneratorsProvider) {
            uidGenerators = Collections.emptyMap();
            return;
        }
        uidGeneratorsProvider.stream().forEach(provider -> {

        });
    }
}
