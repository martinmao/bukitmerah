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
package org.scleropages.serialize.msgpack;

import org.scleropages.serialize.SerialIdRegistry;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public final class MsgPackIdRegistry extends SerialIdRegistry<Integer> {


    @Override
    protected Class<? extends Annotation> annotation() {
        return MsgPackId.class;
    }

    @Override
    protected Integer asId(Object annotationValue) {
        int id = Integer.parseInt(annotationValue.toString());
        Assert.isTrue(id > 100, "@MsgPackId value must more than 100.");
        return id;
    }

    @Override
    public void register(Class<?> clazz, Integer id) {
        super.register(clazz, id);
        MsgPackSupport.register(clazz);
    }

    public void registerWithoutPack(Class<?> clazz, Integer id) {
        super.register(clazz, id);
    }
}
