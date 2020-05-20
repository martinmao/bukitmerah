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
package org.scleropages.serialize.msgpack;

import org.scleropages.serialize.spi.DataSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackDataSerializer extends MsgPackSupport implements DataSerializer<InputStream, OutputStream> {

	public MsgPackDataSerializer(InputStream in) {
		super(in);
	}

	public MsgPackDataSerializer(OutputStream out) {
		super(out);
	}

	@Override
	public boolean readBool() throws IOException {
		assertUnPackMode();
		return unpacker.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		assertUnPackMode();
		return unpacker.readByte();
	}

	@Override
	public short readShort() throws IOException {
		assertUnPackMode();
		return unpacker.readShort();
	}

	@Override
	public int readInt() throws IOException {
		assertUnPackMode();
		return unpacker.readInt();
	}

	@Override
	public long readLong() throws IOException {
		assertUnPackMode();
		return unpacker.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		assertUnPackMode();
		return unpacker.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		assertUnPackMode();
		return unpacker.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		assertUnPackMode();
		return unpacker.readString();
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void writeUTF(String v) throws IOException {
		assertPackMode();
		packer.write(v);
	}

	@Override
	public void flushWrite() throws IOException {
		assertPackMode();
		packer.flush();
	}

	@Override
	public char readChar() throws IOException {
		assertPackMode();
		return (char) unpacker.readShort();
	}

	@Override
	public void writeChar(char v) throws IOException {
		assertUnPackMode();
		packer.write((short) v);
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		packer.write(v);
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = unpacker.readByte();
		}
	}

}
