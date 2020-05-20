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

import org.scleropages.serialize.spi.DataSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackDataSerializer8 extends MsgPackSupport8 implements DataSerializer<InputStream, OutputStream> {

	public MsgPackDataSerializer8(InputStream in) {
		super(in);
	}

	public MsgPackDataSerializer8(OutputStream out) {
		super(out);
	}

	@Override
	public boolean readBool() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackByte();
	}

	@Override
	public short readShort() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackShort();
	}

	@Override
	public int readInt() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackInt();
	}

	@Override
	public long readLong() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackLong();
	}

	@Override
	public float readFloat() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackFloat();
	}

	@Override
	public double readDouble() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackDouble();
	}

	@Override
	public String readUTF() throws IOException {
		assertUnPackMode();
		return messageUnpacker.unpackString();
	}

	@Override
	public char readChar() throws IOException {
		assertUnPackMode();
		return (char) messageUnpacker.unpackFloat();
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		assertUnPackMode();
		messageUnpacker.readPayload(bytes);
	}

	@Override
	public InputStream getReadSource() {
		return super.getReadSource();
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		assertPackMode();
		messagePacker.packBoolean(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		assertPackMode();
		messagePacker.packByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		assertPackMode();
		messagePacker.packShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		assertPackMode();
		messagePacker.packInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		assertPackMode();
		messagePacker.packLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		assertPackMode();
		messagePacker.packFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		assertPackMode();
		messagePacker.packDouble(v);
	}

	@Override
	public void writeUTF(String v) throws IOException {
		assertPackMode();
		messagePacker.packString(v);
	}

	@Override
	public void writeChar(char v) throws IOException {
		assertPackMode();
		messagePacker.packShort((short) v);
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		assertPackMode();
		messagePacker.addPayload(v);
	}

	@Override
	public OutputStream getWriteSource() {
		return super.getWriteSource();
	}

	@Override
	public void flushWrite() throws IOException {
		messagePacker.flush();
	}

}
