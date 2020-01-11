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

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.primitives.Shorts;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.MessageLite;
import org.apache.shiro.util.Assert;
import org.scleropages.serialize.spi.ObjectSerializer;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */

public class ProtobufObjectSerializer extends ProtobufSupport implements ObjectSerializer<InputStream, OutputStream> {

	private final ProtobufIdRegistry registry;
	
	public ProtobufObjectSerializer(InputStream in, final ProtobufIdRegistry registry) {
		super(in);
		this.registry=registry;
	}

	public ProtobufObjectSerializer(OutputStream out, final ProtobufIdRegistry registry) {
		super(out);
		this.registry=registry;

	}

	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		return readObject(null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
		Short id = Shorts.fromByteArray(input.readRawBytes(2));
		Class<?> clazz = registry.get(id);
		Assert.notNull(clazz, "clazz not found from protobuf registry of id: " + id);
		if (null == cls || ClassUtils.isAssignable(cls, clazz)) {
			return (T) MethodAccess.get(clazz).invoke(null, "parseFrom", PARAM_TYPE, input);
		}
		throw new IllegalArgumentException(
				"expected class type not match by given class: " + cls + " and actual is :" + clazz);
	}

	private static final Class<?>[] PARAM_TYPE = new Class[] { CodedInputStream.class };

	@Override
	public void writeObject(Object obj) throws IOException {
		Short id = registry.get(obj.getClass());
		Assert.notNull(id, "id not found from protobuf registry of class: " + obj.getClass());
		output.writeRawBytes(Shorts.toByteArray(id));
		if (obj instanceof MessageLite) {
			((MessageLite) obj).writeTo(output);
		}
		if (obj instanceof MessageLite.Builder) {
			((MessageLite.Builder) obj).build().writeTo(output);
		}
	}

}
