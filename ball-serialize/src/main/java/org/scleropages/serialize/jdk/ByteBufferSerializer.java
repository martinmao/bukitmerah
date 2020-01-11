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
package org.scleropages.serialize.jdk;

import org.scleropages.serialize.spi.DataSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ByteBufferSerializer implements DataSerializer<ByteBuffer,ByteBuffer> {

	private final ByteBuffer byteBuffer;

	public ByteBufferSerializer(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	@Override
	public boolean readBool() throws IOException {
		byte b = byteBuffer.get();
		return b == 1 ? true : false;
	}

	@Override
	public byte readByte() throws IOException {
		return byteBuffer.get();
	}

	@Override
	public short readShort() throws IOException {
		return byteBuffer.getShort();
	}

	@Override
	public int readInt() throws IOException {
		return byteBuffer.getInt();
	}

	@Override
	public long readLong() throws IOException {
		return byteBuffer.getLong();
	}

	@Override
	public float readFloat() throws IOException {
		return byteBuffer.getFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return byteBuffer.getDouble();
	}

	@Override
	public String readUTF() throws IOException {
		byte[] bytes = new byte[4];
		byteBuffer.get(bytes);
		return new String(bytes, "utf-8");
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		byteBuffer.put((byte) (v == true ? 1 : 0));
	}

	@Override
	public void writeByte(byte v) throws IOException {
		byteBuffer.put(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		byteBuffer.putShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		byteBuffer.putInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		byteBuffer.putLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		byteBuffer.putFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		byteBuffer.putDouble(v);
	}



	@Override
	public void writeUTF(String v) throws IOException {
		byteBuffer.put(v.getBytes("utf-8"));
	}

	public void close() throws IOException {

	}

	@Override
	public char readChar() throws IOException {
		return byteBuffer.getChar();
	}

	@Override
	public void writeChar(char v) throws IOException {
		byteBuffer.putChar(v);
	}

	@Override
	public ByteBuffer getReadSource() {
		return byteBuffer;
	}

	@Override
	public ByteBuffer getWriteSource() {
		return byteBuffer;
	}
	
	@Override
	public void flushWrite() throws IOException {
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		byteBuffer.put(v);
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		byteBuffer.get(bytes);
	}

}
