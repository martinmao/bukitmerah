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

import org.scleropages.serialize.SerialIdRegistry;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class ProtobufIdRegistry extends SerialIdRegistry<Short> {

	@Override
	protected Class<? extends Annotation> annotation() {
		return ProtobufId.class;
	}

	@Override
	protected Short asId(Object annotationValue) {
		short id = Short.parseShort(annotationValue.toString());
		Assert.isTrue(id > 100, "@ProtobufId value must more than 100.");
		return id;
	}

}
