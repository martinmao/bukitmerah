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
package org.scleropages.crud.orm.jpa;

import com.google.common.collect.Maps;
import org.scleropages.crud.FrameworkContext;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.util.Assert;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class used fetch jpa provider information. managed entities metadata...
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JpaContexts {

    private static volatile List<String> ENTITIES_CLASSES_NAMES = null;

    private static final Map<Class, EntityMetaModel<?>> ENTITIES_ATTRIBUTES = Maps.newConcurrentMap();


    public static Map<String, Object> jpaProperties() {
        return getRequiredEntityManagerFactoryBean().getJpaPropertyMap();
    }

    /**
     * return all entity classes names.
     *
     * @return
     */
    public static List<String> entityClassesNames() {
        if (null != ENTITIES_CLASSES_NAMES)
            return ENTITIES_CLASSES_NAMES;
        ENTITIES_CLASSES_NAMES = Collections.unmodifiableList(getRequiredEntityManagerFactoryBean().getPersistenceUnitInfo().getManagedClassNames());
        return ENTITIES_CLASSES_NAMES;
    }


    /**
     * get required {@link EntityType} by given entity class.
     *
     * @param entityClass
     * @param <T>
     * @return
     */
    public static <T> EntityType<T> getEntityType(Class<T> entityClass) {
        return getRequiredEntityManagerFactory().getMetamodel().entity(entityClass);
    }

    /**
     * get required {@link EntityMetaModel} by given entity class.
     *
     * @param entityClass
     * @param <T>
     * @return
     */
    public static <T> EntityMetaModel<T> getEntityMetaModel(Class<T> entityClass) {
        return (EntityMetaModel<T>) ENTITIES_ATTRIBUTES.computeIfAbsent(entityClass, clazz -> new EntityMetaModel(clazz));
    }

    /**
     * return true if given entity class is a entity type.
     *
     * @param entityClass
     * @return
     */
    public static boolean isEntityType(Class entityClass) {
        try {
            return null != getRequiredEntityManagerFactory().getMetamodel().entity(entityClass);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * return true if given object is instance of an entity type.
     *
     * @param entityClass
     * @return
     */
    public static boolean isEntity(Object object) {
        if (null == object)
            return false;
        return isEntityType(object.getClass());
    }


    /**
     * a easy way and fast attributes metadata navigation api.
     * it's was already grouped by attribute type before use.
     *
     * @param <T>
     */
    public static final class EntityMetaModel<T> {

        /**
         * defined a group of navigation map. search field metadata quickly.
         */
        private final EntityType<T> entityType;
        private final Map<String, Attribute<T, ?>> attributes;//all fields
        private final Map<String, SingularAttribute<T, ?>> singularAttributes;//single value type fields
        private final Map<String, SingularAttribute<T, ?>> singularReferencedAttributes;//referenced single value type fields.
        private final Map<String, SingularAttribute<T, ?>> singularBasicAttributes;//basic value type fields.
        private final Map<String, PluralAttribute<T, ?, ?>> pluralAttributes;//collection value type fields

        private EntityMetaModel(Class<T> entityClass) {
            Assert.notNull(entityClass, "entityType is required.");
            entityType = getEntityType(entityClass);
            Map<String, Attribute<T, ?>> attributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularAttributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularReferencedAttributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularBasicAttributes = Maps.newHashMap();
            Map<String, PluralAttribute<T, ?, ?>> pluralAttributes = Maps.newHashMap();
            entityType.getAttributes().forEach(attr -> {
                String attrName = attr.getName();
                attributes.put(attrName, (Attribute<T, ?>) attr);
                if (attr instanceof SingularAttribute) {
                    SingularAttribute singularAttribute = (SingularAttribute<T, ?>) attr;
                    singularAttributes.put(attrName, singularAttribute);
                    if (attr.isAssociation()) {
                        singularReferencedAttributes.put(attrName, singularAttribute);
                    } else {
                        singularBasicAttributes.put(attrName, singularAttribute);
                    }
                }
                if (attr.isCollection() && attr instanceof PluralAttribute) {
                    pluralAttributes.put(attrName, (PluralAttribute<T, ?, ?>) attr);
                }
            });
            this.attributes = Collections.unmodifiableMap(attributes);
            this.singularAttributes = Collections.unmodifiableMap(singularAttributes);
            this.singularReferencedAttributes = Collections.unmodifiableMap(singularReferencedAttributes);
            this.singularBasicAttributes = Collections.unmodifiableMap(singularBasicAttributes);
            this.pluralAttributes = Collections.unmodifiableMap(pluralAttributes);
        }

        /**
         * return {@link EntityType}
         *
         * @return
         */
        public EntityType<T> entityType() {
            return entityType;
        }

        /**
         * return all persist attributes metadata
         *
         * @return
         */
        public Collection<Attribute<T, ?>> attributes() {
            return attributes.values();
        }

        /**
         * return all singular value persist attributes metadata
         *
         * @return
         */
        public Collection<SingularAttribute<T, ?>> singularAttributes() {
            return singularAttributes.values();
        }

        /**
         * return all singular value(referenced field) persist attributes metadata
         *
         * @return
         */
        public Collection<SingularAttribute<T, ?>> singularReferencedAttributes() {
            return singularReferencedAttributes.values();
        }

        /**
         * return all singular value(basic field) persist attributes metadata
         *
         * @return
         */
        public Collection<SingularAttribute<T, ?>> singularBasicAttributes() {
            return singularBasicAttributes.values();
        }

        /**
         * return all plural value(list,set,map...) persist attributes metadata
         *
         * @return
         */
        public Collection<PluralAttribute<T, ?, ?>> pluralAttributes() {
            return pluralAttributes.values();
        }

        /**
         * return persist attribute metadata by given name.
         *
         * @param name
         * @param <Y>
         * @return
         */
        public <Y> Attribute<T, Y> attribute(String name) {
            return (Attribute<T, Y>) attributes.get(name);
        }

        /**
         * return singular value persist attribute metadata by given name.
         *
         * @param name
         * @param <Y>
         * @return
         */
        public <Y> SingularAttribute<T, Y> singularAttribute(String name) {
            return (SingularAttribute<T, Y>) singularAttributes.get(name);
        }

        /**
         * return plural value(list,set,map...) persist attribute metadata by given name.
         *
         * @param name
         * @param <Y>
         * @return
         */
        public <Y, E> PluralAttribute<T, Y, E> pluralAttribute(String name) {
            return (PluralAttribute<T, Y, E>) pluralAttributes.get(name);
        }
    }


    private static EntityManagerFactory getRequiredEntityManagerFactory() {
        return getRequiredEntityManagerFactoryBean().getObject();
    }

    private static AbstractEntityManagerFactoryBean getRequiredEntityManagerFactoryBean() {
        return FrameworkContext.getBean(AbstractEntityManagerFactoryBean.class);
    }
}
