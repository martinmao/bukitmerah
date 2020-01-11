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
package org.scleropages.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.scleropages.serialize.spi.ObjectSerializer;
import org.scleropages.serialize.spi.StreamSerializerSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class KryoObjectSerializer extends StreamSerializerSupport
		implements ObjectSerializer<InputStream, OutputStream> {

	private final Input input;
	private final Output output;
	private final KryoPool kryoPool;

	private volatile boolean typeUnkown = true;

	public KryoObjectSerializer(InputStream in, final KryoPool kryoPool) {
		super(in);
		input = new Input(in);
		this.kryoPool = kryoPool;
		output = null;
	}

	public KryoObjectSerializer(OutputStream out, final KryoPool kryoPool) {
		super(out);
		output = new Output(out);
		this.kryoPool = kryoPool;
		input = null;
	}

	@Override
	public Object readObject() throws IOException, ClassNotFoundException {
		return kryoPool.run(new KryoCallback<Object>() {
			@Override
			public Object execute(Kryo kryo) {
				if (typeUnkown)
					return kryo.readClassAndObject(input);
				return kryo.readObject(input, Object.class);
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T readObject(final Class<T> cls) throws IOException, ClassNotFoundException {
		return kryoPool.run(new KryoCallback<T>() {
			@Override
			public T execute(Kryo kryo) {
				if (typeUnkown)
					return (T) kryo.readClassAndObject(input);
				return kryo.readObject(input, cls);
			}
		});
	}

	@Override
	public void writeObject(final Object obj) throws IOException {
		kryoPool.run(new KryoCallback<Void>() {
			@Override
			public Void execute(Kryo kryo) {
				if (typeUnkown)
					kryo.writeClassAndObject(output, obj);
				else
					kryo.writeObject(output, obj);
				return null;
			}
		});

	}

	public void setTypeUnkown(boolean typeUnkown) {
		this.typeUnkown = typeUnkown;
	}

	@Override
	public void flushWrite() throws IOException {
		output.flush();
	}

}
