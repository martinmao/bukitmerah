/**
 * 
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.serialize.msgpack.v8;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.scleropages.serialize.spi.StreamSerializerSupport;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackSupport8 extends StreamSerializerSupport{

	protected final MessageUnpacker messageUnpacker;
	protected final MessagePacker messagePacker;
	/**
	 * perform deserialize
	 * 
	 * @param in
	 */
	public MsgPackSupport8(InputStream in) {
		super(in);
		this.messageUnpacker = MessagePack.newDefaultUnpacker(in);
		this.messagePacker = null;
	}

	/**
	 * perform serialize
	 * 
	 * @param in
	 */
	public MsgPackSupport8(OutputStream out) {
		super(out);
		this.messagePacker = MessagePack.newDefaultPacker(out);
		this.messageUnpacker = null;
	}
	
	protected void assertPackMode() {
		Assert.notNull(messagePacker, "not in packer mode.");
	}

	protected void assertUnPackMode() {
		Assert.notNull(messageUnpacker, "not in unpacker mode.");
	}
	
	@Override
	public void flushWrite() throws IOException {
		messagePacker.flush();
		super.flushWrite();
	}
}
