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

import org.scleropages.serialize.SerializerContextHolder.SerializerContextHolderStrategy;
import org.springframework.util.Assert;

/**
 * {@link ThreadLocal} impl for ad-hoc with {@link DataObjectReader} and
 * {@link DataObjectWriter}
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ThreadLocalSerializerContextHolder implements SerializerContextHolderStrategy {

	private ThreadLocal<DataObjectReader<?>> dataObjectReaderThreadLocal = new ThreadLocal<>();
	private ThreadLocal<DataObjectWriter<?>> dataObjectWriterThreadLocal = new ThreadLocal<>();

	public DataObjectReader<?> getDataObjectReader() {
		return dataObjectReaderThreadLocal.get();
	}

	@Override
	public DataObjectWriter<?> getDataObjectWriter() {
		return dataObjectWriterThreadLocal.get();
	}

	@Override
	public void setDataObjectReader(DataObjectReader<?> reader) {
		Assert.notNull(reader);
		dataObjectReaderThreadLocal.set(reader);
	}

	@Override
	public void setDataObjectWriter(DataObjectWriter<?> writer) {
		Assert.notNull(writer);
		dataObjectWriterThreadLocal.set(writer);
	}

	@Override
	public void clear() {
		dataObjectReaderThreadLocal.remove();
		dataObjectWriterThreadLocal.remove();
	}
}
