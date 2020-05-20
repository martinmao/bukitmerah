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
package org.scleropages.serialize.msgpack.v8;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.scleropages.serialize.spi.ObjectSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */

public class MsgPackObjectSerializer8 extends MsgPackSupport8 implements ObjectSerializer<InputStream, OutputStream> {

    private final ObjectMapper objectMapper;


    public MsgPackObjectSerializer8(InputStream in, ObjectMapper objectMapper) {
        super(in);
        this.objectMapper = objectMapper;
    }

    public MsgPackObjectSerializer8(OutputStream out, ObjectMapper objectMapper) {
        super(out);
        this.objectMapper = objectMapper;
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return objectMapper.readValue(getInputStream(), Object.class);
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return objectMapper.readValue(getInputStream(), cls);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        objectMapper.writeValue(getOutputStream(), obj);
    }


}
