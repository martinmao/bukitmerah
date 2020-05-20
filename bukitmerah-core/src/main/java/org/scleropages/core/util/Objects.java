package org.scleropages.core.util; /**
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

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/**
 * 
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Objects {

	public static class CollectionInspection {
		public final Class<?> concreteType;
		public final Class<?> abstractType;
		public final Class<?> commonElementType;

		public CollectionInspection(final Class<?> concreteType, final Class<?> abstractType,
				Class<?> commonElementType) {
			this.concreteType = concreteType;
			this.abstractType = abstractType;
			this.commonElementType = commonElementType;
		}
	}

	public static class MapInspection {
		public final Class<?> concreteType;
		public final Class<?> abstractType;
		public final Entry<Class<?>, Class<?>> commonEntryType;

		public MapInspection(final Class<?> concreteType, final Class<?> abstractType,
				final Entry<Class<?>, Class<?>> commonEntryType) {
			this.concreteType = concreteType;
			this.abstractType = abstractType;
			this.commonEntryType = commonEntryType;
		}
	}

	public static MapInspection inspect(Map<?, ?> map) {
		Class<?> concreteType = map.getClass();
		Class<?> abstractType = Map.class;
		Class<?> keyType = null;
		Class<?> valueType = null;
		for (Entry<?, ?> entry : map.entrySet()) {
			Assert.notNull(entry.getKey(), "key must not be null.");
			Class<?> _keyType = entry.getKey().getClass();
			if (keyType == null)
				keyType = _keyType;
			if (_keyType != keyType)
				return new MapInspection(concreteType, abstractType, null);
			Class<?> _valueType = entry.getValue() != null ? entry.getValue().getClass() : valueType;
			if (valueType == null)
				valueType = _valueType;
			if (_valueType != valueType) {
				return new MapInspection(concreteType, abstractType, null);
			}
		}
		final Class<?> entryKey = keyType;
		final Class<?> entryValue = valueType;
		return new MapInspection(concreteType, abstractType, new Entry<Class<?>, Class<?>>() {
			@Override
			public Class<?> setValue(Class<?> value) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Class<?> getValue() {
				return entryValue;
			}

			@Override
			public Class<?> getKey() {
				return entryKey;
			}
		});

	}

	public static CollectionInspection inspect(Collection<?> obj) {
		Class<?> concreteType;
		Class<?> abstractType;
		Class<?> commonElementType;
		Assert.notNull(obj, "inspector collection must not be null.");
		concreteType = obj.getClass();
		commonElementType = CollectionUtils.findCommonElementType(obj);
		if (ClassUtils.isAssignableValue(List.class, obj))
			abstractType = List.class;
		else if (ClassUtils.isAssignableValue(Set.class, obj))
			abstractType = Set.class;
		else if (ClassUtils.isAssignableValue(Queue.class, obj))
			abstractType = Queue.class;
		else
			abstractType = Collection.class;

		return new CollectionInspection(concreteType, abstractType, commonElementType);
	}

}
