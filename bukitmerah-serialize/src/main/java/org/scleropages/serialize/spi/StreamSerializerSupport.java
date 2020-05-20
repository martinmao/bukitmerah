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

import org.scleropages.serialize.ReadSourceHolder;
import org.scleropages.serialize.WriteSourceHolder;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class StreamSerializerSupport implements ReadSourceHolder<InputStream>, WriteSourceHolder<OutputStream> {

	public static final byte READ_MODE = 1;
	public static final byte WRITE_MODE = 2;

	private final int mode;

	private final InputStream inputStream;

	private final OutputStream outputStream;

	public StreamSerializerSupport() {
		throw new IllegalStateException("This constructor must not be used in external");
	}

	public StreamSerializerSupport(InputStream in) {
		Assert.notNull(in);
		this.inputStream = in;
		this.outputStream = null;
		mode = READ_MODE;
	}

	public StreamSerializerSupport(OutputStream out) {
		Assert.notNull(out);
		this.inputStream = null;
		this.outputStream = out;
		mode = WRITE_MODE;
	}

	protected InputStream getInputStream() {
		return inputStream;
	}

	protected OutputStream getOutputStream() {
		return outputStream;
	}

	protected int getMode() {
		return mode;
	}

	protected void assertInReadMode() {
		if (READ_MODE != getMode())
			throw new IllegalStateException("Not in read mode.");

	}

	protected void assertInWriteMode() {
		if (WRITE_MODE != getMode())
			throw new IllegalStateException("Not in write mode.");
	}

	@Override
	public InputStream getReadSource() {
		return inputStream;
	}

	@Override
	public OutputStream getWriteSource() {
		return outputStream;
	}

	@Override
	public void flushWrite() throws IOException {
		outputStream.flush();
	}
}
