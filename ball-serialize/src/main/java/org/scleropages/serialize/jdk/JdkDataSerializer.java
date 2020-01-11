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
import org.scleropages.serialize.spi.StreamSerializerSupport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JdkDataSerializer extends StreamSerializerSupport implements DataSerializer<InputStream, OutputStream> {

	private final DataInputStream in;

	private final DataOutputStream out;

	public JdkDataSerializer(InputStream in) {
		super(in);
		this.in = new DataInputStream(in);
		this.out = null;
	}

	public JdkDataSerializer(OutputStream out) {
		super(out);
		this.out = new DataOutputStream(out);
		this.in = null;
	}

	@Override
	public char readChar() throws IOException {
		assertInReadMode();
		return in.readChar();
	}

	@Override
	public void writeChar(char v) throws IOException {
		assertInWriteMode();
		out.writeChar(v);
	}

	@Override
	public boolean readBool() throws IOException {
		assertInReadMode();
		return in.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		assertInReadMode();
		return in.readByte();
	}

	@Override
	public short readShort() throws IOException {
		assertInReadMode();
		return in.readShort();
	}

	@Override
	public int readInt() throws IOException {
		assertInReadMode();
		return in.readInt();
	}

	@Override
	public long readLong() throws IOException {
		assertInReadMode();
		return in.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		assertInReadMode();
		return in.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		assertInReadMode();
		return in.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		assertInReadMode();
		return in.readUTF();
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		assertInWriteMode();
		out.writeBoolean(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		assertInWriteMode();
		out.writeByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		assertInWriteMode();
		out.writeShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		assertInWriteMode();
		out.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		assertInWriteMode();
		out.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		assertInWriteMode();
		out.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		assertInWriteMode();
		out.writeDouble(v);
	}

	@Override
	public void flushWrite() throws IOException {
		assertInWriteMode();
		out.flush();
	}

	@Override
	public void writeUTF(String v) throws IOException {
		out.writeUTF(v);
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		out.write(v);
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		in.read(bytes);
	}

}
