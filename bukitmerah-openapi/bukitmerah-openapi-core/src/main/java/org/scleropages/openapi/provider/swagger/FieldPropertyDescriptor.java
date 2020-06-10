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
package org.scleropages.openapi.provider.swagger;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;


/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class FieldPropertyDescriptor {
    private final PropertyDescriptor propertyDescriptor;
    private final Field propertyField;

    public FieldPropertyDescriptor(Class baseClass, PropertyDescriptor propertyDescriptor) {
        this.propertyDescriptor = propertyDescriptor;
        propertyField = ReflectionUtils.findField(baseClass, propertyDescriptor.getName());
    }

    public FieldPropertyDescriptor(Class baseClass, Field propertyField) {
        this.propertyField = propertyField;
        this.propertyDescriptor = BeanUtils.getPropertyDescriptor(baseClass, propertyField.getName());
    }

    public Field getPropertyField() {
        return propertyField;
    }

    public PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    @Override
    public String toString() {
        return "FieldPropertyDescriptor{" +
                "propertyDescriptor=" + propertyDescriptor +
                ", propertyField=" + propertyField +
                '}';
    }


    public ResolvableType createResolvableType() {
        if (null != propertyField)
            return ResolvableType.forField(propertyField);
        if (null != propertyDescriptor)
            return ResolvableType.forMethodReturnType(propertyDescriptor.getReadMethod());
        return null;
    }
}