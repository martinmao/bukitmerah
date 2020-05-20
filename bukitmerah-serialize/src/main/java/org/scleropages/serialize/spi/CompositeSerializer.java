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
package org.scleropages.serialize.spi;

import org.scleropages.serialize.DataObjectReader;
import org.scleropages.serialize.DataObjectWriter;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * 
 * 简化SPI程序开发.组合序列化器，将数据序列化器 {@link DataSerializer} 以及 对象序列化器
 * {@link ObjectSerializer}组合起来，提供一致的数据、对象、以及二者皆可的，序列化反序列化功能<br>
 * <b>注意：必须确保输入、输出类型是一致的，即无论数据序列化器或对象序列化器 R,W必须是同一实例，否则会存在不一致现象。</b>
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class CompositeSerializer<R, W> implements DataObjectReader<R>, DataObjectWriter<W> {

	private final DataSerializer<R, W> dataSerializer;

	private final ObjectSerializer<R, W> objectSerializer;

	public CompositeSerializer(DataSerializer<R, W> dataSerializer, ObjectSerializer<R, W> objectSerializer) {
		Assert.notNull(dataSerializer);
		Assert.notNull(objectSerializer);
		this.dataSerializer = dataSerializer;
		this.objectSerializer = objectSerializer;
	}

	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		return objectSerializer.readObject();
	}

	@Override
	public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
		return objectSerializer.readObject(cls);
	}

	@Override
	public void writeObject(Object obj) throws IOException {
		objectSerializer.writeObject(obj);
	}

	@Override
	public boolean readBool() throws IOException {
		return dataSerializer.readBool();
	}

	@Override
	public byte readByte() throws IOException {
		return dataSerializer.readByte();
	}

	@Override
	public short readShort() throws IOException {
		return dataSerializer.readShort();
	}

	@Override
	public int readInt() throws IOException {
		return dataSerializer.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return dataSerializer.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return dataSerializer.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return dataSerializer.readDouble();
	}

	@Override
	public String readUTF() throws IOException {
		return dataSerializer.readUTF();
	}

	@Override
	public void writeBool(boolean v) throws IOException {
		dataSerializer.writeBool(v);
	}

	@Override
	public void writeByte(byte v) throws IOException {
		dataSerializer.writeByte(v);
	}

	@Override
	public void writeShort(short v) throws IOException {
		dataSerializer.writeShort(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		dataSerializer.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		dataSerializer.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		dataSerializer.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		dataSerializer.writeDouble(v);
	}

	@Override
	public void writeUTF(String v) throws IOException {
		dataSerializer.writeUTF(v);
	}

	@Override
	public void flushWrite() throws IOException {
		dataSerializer.flushWrite();
		objectSerializer.flushWrite();
	}

	@Override
	public char readChar() throws IOException {
		return dataSerializer.readChar();
	}

	@Override
	public void writeChar(char v) throws IOException {
		dataSerializer.writeChar(v);
	}

	public DataSerializer<R, W> getDataSerializer() {
		return dataSerializer;
	}

	public ObjectSerializer<R, W> getObjectSerializer() {
		return objectSerializer;
	}

	@Override
	public R getReadSource() {
		return dataSerializer.getReadSource();
	}

	@Override
	public W getWriteSource() {
		return dataSerializer.getWriteSource();
	}

	@Override
	public void writeBytes(byte[] v) throws IOException {
		dataSerializer.writeBytes(v);
	}

	@Override
	public void readBytes(byte[] bytes) throws IOException {
		dataSerializer.readBytes(bytes);
	}

}
