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

/**
 * Ad-hoc {@link DataObjectReader} and {@link DataObjectWriter} holder
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SerializerContextHolder {

	private static SerializerContextHolderStrategy contextHolderStrategy = new ThreadLocalSerializerContextHolder();

	public interface SerializerContextHolderStrategy {

		DataObjectReader<?> getDataObjectReader();

		DataObjectWriter<?> getDataObjectWriter();

		void setDataObjectReader(DataObjectReader<?> reader);

		void setDataObjectWriter(DataObjectWriter<?> writer);

		void clear();
	}

	public interface DataObjectWriterInitializer {
		DataObjectWriter<?> initDataObjectWriter();
	}

	public static DataObjectReader<?> getDataObjectReader() {
		return contextHolderStrategy.getDataObjectReader();
	}

	public static DataObjectWriter<?> getDataObjectWriter() {
		return contextHolderStrategy.getDataObjectWriter();
	}

	public static void setDataObjectReader(DataObjectReader<?> reader) {
		contextHolderStrategy.setDataObjectReader(reader);
	}

	public static void setDataObjectWriter(DataObjectWriter<?> writer) {
		contextHolderStrategy.setDataObjectWriter(writer);
	}

	public static void clear() {
		contextHolderStrategy.clear();
	}
}
