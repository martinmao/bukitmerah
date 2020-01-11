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
package org.scleropages.serialize.spi;

import java.io.IOException;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class UnsupportedDataSerializer implements DataSerializer {
    @Override
    public boolean readBool() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readShort() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readLong() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(byte[] bytes) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(byte v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeShort(short v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeInt(int v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeLong(long v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeFloat(float v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDouble(double v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeUTF(String v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeChar(char v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getReadSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getWriteSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushWrite() throws IOException {
    }
}
