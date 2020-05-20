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
package org.scleropages.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SerializerFactoryUtil {

    private static final int DEFAULT_WRITE_BUFF_SIZE = 64;

    public static byte[] serialize(SerializerFactory serializerFactory, Object object) throws IOException {
        if (serializerFactory.supportSerialize(OutputStream.class)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(DEFAULT_WRITE_BUFF_SIZE);
            serialize(serializerFactory, object, out);
            return out.toByteArray();
        } else {
            throw new IllegalStateException("not implementation or supported.");
        }
    }

    public static <T> T deserialize(SerializerFactory serializerFactory, byte[] payload) throws IOException, ClassNotFoundException {
        if (serializerFactory.supportDeserialize(InputStream.class)) {
            ByteArrayInputStream in = new ByteArrayInputStream(payload);
            return deserialize(serializerFactory, in);
        } else {
            throw new IllegalStateException("not implementation or supported.");
        }
    }

    public static <T> T deserialize(SerializerFactory serializerFactory, InputStream in) throws IOException, ClassNotFoundException {
        DataObjectReader dataObjectReader = serializerFactory.deserialize(in);
        return (T) dataObjectReader.readObject();
    }

    public static void serialize(SerializerFactory serializerFactory, Object object, OutputStream out) throws IOException {
        DataObjectWriter dataObjectWriter = serializerFactory.serialize(out);
        dataObjectWriter.writeObject(object);
        dataObjectWriter.flushWrite();
    }
}
