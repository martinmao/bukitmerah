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
package org.scleropages.serialize.netty;

import io.netty.buffer.ByteBuf;
import org.scleropages.serialize.spi.DataSerializer;

import java.io.IOException;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ByteBufSerializer implements DataSerializer<ByteBuf, ByteBuf> {

	private final ByteBuf byteBuffer;

	public ByteBufSerializer(ByteBuf byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	@Override
	public boolean readBool() throws IOException {
		return byteBuffer.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return byteBuffer.readByte();
	}

	@Override
	public short readShort() throws IOException {
		return byteBuffer.readShort();
	}

	@Override
	public int readInt() throws IOException {
		return byteBuffer.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return byteBuffer.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return byteBuffer.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return byteBuffer.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		byte[] bytes = new byte[4];
		byteBuffer.readBytes(bytes);
		return new String(bytes, "utf-8");
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		byteBuffer.writeBoolean(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		byteBuffer.writeByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		byteBuffer.writeShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		byteBuffer.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		byteBuffer.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		byteBuffer.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		byteBuffer.writeDouble(v);
	}

	@Override
	public void flushWrite() throws IOException {
	}

	@Override
	public void writeUTF(String v) throws IOException {
		byteBuffer.writeBytes(v.getBytes("utf-8"));
	}

	public void close() throws IOException {

	}

	@Override
	public char readChar() throws IOException {
		return byteBuffer.readChar();
	}

	@Override
	public void writeChar(char v) throws IOException {
		byteBuffer.writeChar(v);
	}

	@Override
	public ByteBuf getReadSource() {
		return byteBuffer;
	}

	@Override
	public ByteBuf getWriteSource() {
		return byteBuffer;
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		byteBuffer.writeBytes(v);
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		byteBuffer.readBytes(bytes);
	}
}
