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
package org.scleropages.serialize.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.scleropages.serialize.spi.DataSerializer;
import org.scleropages.serialize.spi.StreamSerializerSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class KryoDataSerializer extends StreamSerializerSupport implements DataSerializer<InputStream, OutputStream> {

	private final Input input;
	private final Output output;

	public KryoDataSerializer(InputStream in) {
		super(in);
		input = new Input(in);
		output = null;
	}

	public KryoDataSerializer(OutputStream out) {
		super(out);
		output = new Output(out);
		input = null;
	}

	@Override
	public boolean readBool() throws IOException {
		assertInReadMode();
		return input.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		assertInReadMode();
		return input.readByte();
	}

	@Override
	public short readShort() throws IOException {
		assertInReadMode();
		return input.readShort();
	}

	@Override
	public int readInt() throws IOException {
		assertInReadMode();
		return input.readInt();
	}

	@Override
	public long readLong() throws IOException {
		assertInReadMode();
		return input.readLong();
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
		return input.readString();
	}

	@Override
	public char readChar() throws IOException {
		assertInReadMode();
		return input.readChar();
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		assertInReadMode();
		input.read(bytes);
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		assertInWriteMode();
		output.writeBoolean(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		assertInWriteMode();
		output.writeByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		assertInWriteMode();
		output.writeShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		assertInWriteMode();
		output.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		assertInWriteMode();
		output.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		assertInWriteMode();
		output.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		assertInWriteMode();
		output.writeDouble(v);
	}

	@Override
	public void writeUTF(String v) throws IOException {
		assertInWriteMode();
		output.writeString(v);
	}

	@Override
	public void writeChar(char v) throws IOException {
		assertInWriteMode();
		output.writeChar(v);
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		assertInWriteMode();
		output.write(v);
	}

	@Override
	public void flushWrite() throws IOException {
		output.flush();
	}

}
