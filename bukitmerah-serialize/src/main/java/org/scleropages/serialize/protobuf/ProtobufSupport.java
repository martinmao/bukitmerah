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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.scleropages.serialize.spi.StreamSerializerSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ProtobufSupport extends StreamSerializerSupport {

	protected final CodedOutputStream output;

	protected final CodedInputStream input;

	/**
	 * perform deserialize
	 * 
	 * @param in
	 */
	public ProtobufSupport(InputStream in) {
		super(in);
		input = CodedInputStream.newInstance(in);
		output = null;
	}

	/**
	 * perform serialize
	 * 
	 * @param in
	 */
	public ProtobufSupport(OutputStream out) {
		super(out);
		output = CodedOutputStream.newInstance(out);
		input = null;
	}

	@Override
	public void flushWrite() throws IOException {
		output.flush();
		super.flushWrite();
	}
}
