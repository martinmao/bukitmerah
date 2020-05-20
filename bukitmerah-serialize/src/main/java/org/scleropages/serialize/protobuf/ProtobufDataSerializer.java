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
package org.scleropages.serialize.protobuf;

import com.google.common.primitives.Chars;
import com.google.common.primitives.Shorts;
import org.scleropages.serialize.spi.DataSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ProtobufDataSerializer extends ProtobufSupport implements DataSerializer<InputStream, OutputStream> {

	/* not thread safety used thread local only. */
	private int writeIndex = 1;

	public ProtobufDataSerializer(InputStream in) {
		super(in);
	}

	public ProtobufDataSerializer(OutputStream out) {
		super(out);
	}

	@Override
	public boolean readBool() throws IOException {
		assertInReadMode();
		return input.readBool();
	}

	@Override
	public byte readByte() throws IOException {
		assertInReadMode();
		return input.readRawByte();
	}

	@Override
	public short readShort() throws IOException {
		assertInReadMode();
		return Shorts.fromByteArray(input.readRawBytes(2));
	}

	@Override
	public int readInt() throws IOException {
		assertInReadMode();
		return input.readInt32();
	}

	@Override
	public long readLong() throws IOException {
		assertInReadMode();
		return input.readInt64();
	}

	@Override
	public float readFloat() throws IOException {
		assertInReadMode();
		return input.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		assertInReadMode();
		return input.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		assertInReadMode();
		return input.readStringRequireUtf8();
	}

	@Override
	public char readChar() throws IOException {
		assertInReadMode();
		return Chars.fromByteArray(input.readRawBytes(2));
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		assertInReadMode();
		byte[] src = input.readRawBytes(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = src[i];
		}
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		assertInWriteMode();
		output.writeBool(writeIndex++, v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		assertInWriteMode();
		output.writeRawByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		assertInWriteMode();
		output.writeRawBytes(Shorts.toByteArray(v));
	}

	@Override
	public void writeInt(int v) throws IOException {
		assertInWriteMode();
		output.writeInt32(writeIndex++, v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		assertInWriteMode();
		output.writeInt64(writeIndex++, v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		assertInWriteMode();
		output.writeFloat(writeIndex++, v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		assertInWriteMode();
		output.writeDouble(writeIndex++, v);
	}

	@Override
	public void writeUTF(String v) throws IOException {
		assertInWriteMode();
		output.writeString(writeIndex++, v);
	}

	@Override
	public void writeChar(char v) throws IOException {
		assertInWriteMode();
		output.writeRawBytes(Chars.toByteArray(v));
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		assertInWriteMode();
		output.writeRawBytes(v);
	}

}
