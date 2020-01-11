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
package org.scleropages.serialize;

import java.io.IOException;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface DataWriter<W> extends WriteSourceHolder<W> {

	void writeBool(boolean v) throws IOException;

	void writeByte(byte v) throws IOException;

	void writeShort(short v) throws IOException;

	void writeInt(int v) throws IOException;

	void writeLong(long v) throws IOException;

	void writeFloat(float v) throws IOException;

	void writeDouble(double v) throws IOException;

	void writeUTF(String v) throws IOException;

	void writeChar(char v) throws IOException;

	void writeBytes(byte[] v) throws IOException;
}
