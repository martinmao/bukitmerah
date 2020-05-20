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

import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.Template;
import org.msgpack.unpacker.Unpacker;
import org.scleropages.serialize.spi.StreamSerializerSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackSupport extends StreamSerializerSupport {

    protected static final MessagePack messagePack = new MessagePack();

    static {
        /* ~~messagePack registry settings */
    }

    protected final Unpacker unpacker;
    protected final Packer packer;

    /**
     * perform deserialize
     *
     * @param in
     */
    public MsgPackSupport(InputStream in) {
        super(in);
        this.unpacker = messagePack.createUnpacker(in);
        this.packer = null;
    }

    /**
     * perform serialize
     *
     * @param in
     */
    public MsgPackSupport(OutputStream out) {
        super(out);
        this.packer = messagePack.createPacker(out);
        this.unpacker = null;
    }

    public static <T> void register(Class<T> clazz, Template<T> template) {
        synchronized (messagePack) {
            try {
                if (!ClassUtils.isPrimitiveOrWrapper(clazz))
                    messagePack.register(clazz, template);
            } catch (MessageTypeException e) {
                throw new IllegalStateException(
                        "maybe not found register class. please use @MsgPackId instead @Message to annotation class: "
                                + clazz.getName(),
                        e);
            }
        }

    }

    public static <T> void register(Class<T> clazz) {
        synchronized (messagePack) {
            try {
                if (!ClassUtils.isPrimitiveOrWrapper(clazz))
                    messagePack.register(clazz);
            } catch (MessageTypeException e) {
                throw new IllegalStateException(
                        "maybe not found register class. please use @MsgPackId instead @Message to annotation class: "
                                + clazz.getName(),
                        e);
            }
        }
    }

    protected void assertPackMode() {
        Assert.notNull(packer, "not in packer mode.");
    }

    protected void assertUnPackMode() {
        Assert.notNull(unpacker, "not in unpacker mode.");
    }

    @Override
    public void flushWrite() throws IOException {
        packer.flush();
        super.flushWrite();
    }
}
