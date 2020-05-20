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
package org.scleropages.serialize.msgpack;

import org.msgpack.template.SetTemplate;
import org.msgpack.template.Template;
import org.msgpack.template.Templates;
import org.scleropages.core.util.Objects;
import org.scleropages.core.util.Objects.CollectionInspection;
import org.scleropages.core.util.Objects.MapInspection;
import org.scleropages.serialize.spi.ObjectSerializer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class MsgPackObjectSerializer extends MsgPackSupport implements ObjectSerializer<InputStream, OutputStream> {

    private final MsgPackIdRegistry msgPackRegistry;


    public MsgPackObjectSerializer(InputStream in, final MsgPackIdRegistry msgPackRegistry) {
        super(in);
        this.msgPackRegistry = msgPackRegistry;
    }

    public MsgPackObjectSerializer(OutputStream out, final MsgPackIdRegistry msgPackRegistry) {
        super(out);
        this.msgPackRegistry = msgPackRegistry;
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        assertPackMode();
        if (obj instanceof Collection) {
            writeCollectionType((Collection<?>) obj);
        } else if (obj instanceof Map) {
            writeMapType((Map<?, ?>) obj);
        } else {
            Integer id = msgPackRegistry.get(obj.getClass());
            Assert.notNull(id, "not found in msgpack registry by given class: " + obj.getClass().getName()
                    + ". msgpack encoder must register customer types before use.");
            packer.write(id);
            packer.write(obj);
        }
    }

    protected void writeCollectionType(Collection<?> obj) throws IOException {
        CollectionInspection inspection = Objects.inspect(obj);
        if (inspection.commonElementType == null)
            throw new IllegalArgumentException("not allowed difference element type of collection.");
        Integer id = msgPackRegistry.get(inspection.abstractType);
        Assert.notNull(id, inspection.abstractType + " must register before use.");
        packer.write(id);
        id = msgPackRegistry.get(inspection.commonElementType);
        Assert.notNull(id, inspection.commonElementType + " must register before use.");
        packer.write(id);
        packer.write(obj);
    }

    protected void writeMapType(Map<?, ?> map) throws IOException {
        MapInspection inspection = Objects.inspect(map);
        if (inspection.commonEntryType == null)
            throw new IllegalArgumentException("not allowed difference key or value type of map.");
        Integer id = msgPackRegistry.get(inspection.abstractType);
        Assert.notNull(id, inspection.abstractType + " must register before use.");
        packer.write(id);
        id = msgPackRegistry.get(inspection.commonEntryType.getKey());
        Assert.notNull(id, inspection.commonEntryType.getKey() + " must register before use.");
        packer.write(id);
        id = msgPackRegistry.get(inspection.commonEntryType.getValue());
        Assert.notNull(id, inspection.commonEntryType.getValue() + " must register before use.");
        packer.write(id);
        packer.write(map);
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        assertUnPackMode();
        int id = unpacker.readInt();
        Class<?> clazz = msgPackRegistry.get(id);
        Assert.notNull(clazz, "clazz not found in msgpack registry by given id: " + id
                + ". msgpack encoder must register customer types before use.");
        if (ClassUtils.isAssignable(Collection.class, clazz)) {
            return readCollectionType(clazz);
        } else if (ClassUtils.isAssignable(Map.class, clazz)) {
            return readMapType(clazz);
        } else
            return unpacker.read(clazz);
    }

    protected Object readCollectionType(final Class<?> clazz) throws IOException {
        Template<?> templete = null;
        if (ClassUtils.isAssignable(List.class, clazz)) {
            templete = Templates.tList(getElementTemplate());
        } else if (ClassUtils.isAssignable(Set.class, clazz)) {
            templete = new SetTemplate<>(getElementTemplate());
        } else if (ClassUtils.isAssignable(Collection.class, clazz)) {
            templete = Templates.tCollection(getElementTemplate());
        }
        Assert.notNull(templete, "un-supported connection type: " + clazz);
        return unpacker.read(templete);
    }

    protected Object readMapType(final Class<?> clazz) throws IOException {
        if (ClassUtils.isAssignable(Map.class, clazz)) {
            Template<?> templete = Templates.tMap(getElementTemplate(), getElementTemplate());
            return unpacker.read(templete);
        }
        throw new IllegalArgumentException("not a map class: " + clazz);
    }

    protected Template<?> getElementTemplate() throws IOException {
        int id = unpacker.readInt();
        Class<?> elementType = msgPackRegistry.get(id);
        Assert.notNull(elementType, id + " not found from msgpack registry.");
        Template<?> elementTempate = messagePack.lookup(elementType);
        Assert.notNull(elementTempate, "elementTempate not register by given type: " + elementType);
        return elementTempate;
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        assertUnPackMode();
        return unpacker.read(cls);
    }
}
