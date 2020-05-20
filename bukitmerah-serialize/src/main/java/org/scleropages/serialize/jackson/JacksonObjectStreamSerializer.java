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
package org.scleropages.serialize.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scleropages.serialize.spi.ObjectSerializer;
import org.scleropages.serialize.spi.StreamSerializerSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * 默认使用jackson  enableDefaultTyping 在序列化时加入属性 描述其类型（java完整类路径），反序列化时根据该属性实例化对象
 * 该序列化器不应暴露给外部接口，可能存在安全风险，此外由于加入属性描述类型，使原始json增大，影响请参考
 * {@link ObjectMapper#enableDefaultTyping(ObjectMapper.DefaultTyping, JsonTypeInfo.As)}</p>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JacksonObjectStreamSerializer extends StreamSerializerSupport
        implements ObjectSerializer<InputStream, OutputStream> {

    private final ObjectMapper objectMapper;


    public JacksonObjectStreamSerializer(InputStream in, ObjectMapper objectMapper) {
        super(in);
        this.objectMapper = objectMapper;
    }

    public JacksonObjectStreamSerializer(OutputStream out, ObjectMapper objectMapper) {
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
