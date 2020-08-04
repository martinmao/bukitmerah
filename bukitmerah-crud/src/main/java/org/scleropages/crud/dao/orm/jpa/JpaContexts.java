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
package org.scleropages.crud.dao.orm.jpa;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.scleropages.crud.FrameworkContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class used a easy way to obtain jpa provider information, managed entities, attributes, annotations metadata...
 * <p>
 * NOTE: this class must used after persistence manager already initialized (required management by spring container).
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class JpaContexts {

    private static volatile List<String> ENTITIES_CLASSES_NAMES = null;

    private static final Map<Class, ManagedTypeModel<?>> MANAGED_TYPE_MODELS = Maps.newConcurrentMap();

    private static volatile Map<String, EntityType> TABLES_TO_ENTITY_TYPES;


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
     * return all entity types mapped by database table name.
     *
     * @return key as table name, value as {@link EntityType}
     */
    public static Map<String, EntityType> databaseTableEntityTypes() {
        if (null != TABLES_TO_ENTITY_TYPES)
            return TABLES_TO_ENTITY_TYPES;
        Map<String, EntityType> tablesToEntityTypes = Maps.newHashMap();
        getRequiredEntityManagerFactory().getMetamodel().getEntities().forEach(entityType -> {
            Table table = AnnotationUtils.findAnnotation(entityType.getJavaType(), Table.class);
            if (null != table)
                tablesToEntityTypes.putIfAbsent(table.name(), entityType);
        });
        TABLES_TO_ENTITY_TYPES = Collections.unmodifiableMap(tablesToEntityTypes);
        return TABLES_TO_ENTITY_TYPES;
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
     * get required {@link ManagedType} by given entity class.
     *
     * @param typeClass
     * @param <T>
     * @return
     */
    public static <T> ManagedType<T> getManagedType(Class<T> typeClass) {
        return getRequiredEntityManagerFactory().getMetamodel().managedType(typeClass);
    }

    /**
     * get required {@link ManagedTypeModel} by given entity class.
     *
     * @param entityClass
     * @param <T>
     * @return
     */
    public static <T> ManagedTypeModel<T> getManagedTypeModel(Class<T> entityClass) {
        return (ManagedTypeModel<T>) MANAGED_TYPE_MODELS.computeIfAbsent(entityClass, clazz -> new ManagedTypeModel(clazz));
    }


    /**
     * get required embeddable metadata:{@link EmbeddableType} by given class.
     *
     * @param entityClass
     * @param <E>
     * @return
     */
    public static <E> EmbeddableType<E> getEmbeddableType(Class<E> entityClass) {
        return getRequiredEntityManagerFactory().getMetamodel().embeddable(entityClass);
    }

    /**
     * return true if given class is a managed type.
     *
     * @param typeClass
     * @return
     */
    public static boolean isManagedType(Class typeClass) {
        try {
            return null != getRequiredEntityManagerFactory().getMetamodel().managedType(typeClass);
        } catch (IllegalArgumentException e) {
            return false;
        }
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
     * @param object
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
    public static final class ManagedTypeModel<T> {

        private final ManagedType<T> managedType;

        //used if current managedType is a entity type.
        private volatile String table;

        private volatile String columnOfId;

        /**
         * defined a group of navigation map. search field metadata quickly.
         * follow map use attribute name as key and Attribute(or subclasses) as value. grouped by attribute type.
         */
        private final Map<String, Attribute<T, ?>> attributes;//all fields
        private final Map<String, SingularAttribute<T, ?>> singularAttributes;//single value type fields
        private final Map<String, SingularAttribute<T, ?>> singularReferencedAttributes;//referenced single value type fields.
        private final Map<String, SingularAttribute<T, ?>> singularBasicAttributes;//basic value type fields.
        private final Map<String, PluralAttribute<T, ?, ?>> pluralAttributes;//collection value type fields


        /**
         * table name({@link Table#name()}) ->attribute( binding when {@link Attribute#isAssociation()} is true).
         */
        private final Map<String, Attribute<T, ?>> tableAttributes;

        /**
         * column name({@link Column#name()} or {@link JoinColumn#name()} or {@link AttributeOverride#name()}...)->attribute
         */
        private final Map<String, Attribute<T, ?>> columnAttributes;

        /**
         * attribute->{annotation Class->annotation instance}
         */
        private final Map<Attribute, Map<Class, Annotation>> attributeAnnotations;


        private ManagedTypeModel(Class<T> typeClass) {
            Assert.notNull(typeClass, "typeClass is required.");
            managedType = getManagedType(typeClass);
            Map<String, Attribute<T, ?>> attributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularAttributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularReferencedAttributes = Maps.newHashMap();
            Map<String, SingularAttribute<T, ?>> singularBasicAttributes = Maps.newHashMap();
            Map<String, PluralAttribute<T, ?, ?>> pluralAttributes = Maps.newHashMap();
            Map<String, Attribute<T, ?>> columnAttributes = Maps.newHashMap();
            Map<String, Attribute<T, ?>> tableAttributes = Maps.newHashMap();
            managedType.getAttributes().forEach(attr -> {
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
                Stream.of(obtainDatabaseColumnsFromAttribute(attr)).forEach(column -> {
                    Assert.isNull(columnAttributes.putIfAbsent(column.toLowerCase(), (Attribute<T, ?>) attr), "duplicate column name found: " + column + " from: " + typeClass);
                });
                String table = obtainDatabaseTableFromAttribute(attr);
                if (StringUtils.hasText(table))
                    tableAttributes.put(table.toLowerCase(), (Attribute<T, ?>) attr);
            });
            this.attributes = Collections.unmodifiableMap(attributes);
            this.singularAttributes = Collections.unmodifiableMap(singularAttributes);
            this.singularReferencedAttributes = Collections.unmodifiableMap(singularReferencedAttributes);
            this.singularBasicAttributes = Collections.unmodifiableMap(singularBasicAttributes);
            this.pluralAttributes = Collections.unmodifiableMap(pluralAttributes);
            this.columnAttributes = Collections.unmodifiableMap(columnAttributes);
            this.tableAttributes = Collections.unmodifiableMap(tableAttributes);
            this.attributeAnnotations = Maps.newConcurrentMap();
        }


        /**
         * return {@link Attribute} by given database column name.
         *
         * @param columnName
         * @param <Y>
         * @return
         */
        public <Y> Attribute<T, Y> attributeByDatabaseColumn(String columnName) {
            Assert.hasText(columnName, "not allowed empty column name.");
            return (Attribute<T, Y>) columnAttributes.get(columnName.toLowerCase());
        }

        /**
         * return {@link Attribute} by given database table name.
         *
         * @param tableName
         * @param <Y>
         * @return
         */
        public <Y> Attribute<T, Y> attributeByDatabaseTable(String tableName) {
            Assert.hasText(tableName, "not allowed empty table name.");
            return (Attribute<T, Y>) tableAttributes.get(tableName.toLowerCase());
        }


        /**
         * find attribute annotation by given attribute and annotation class. if no annotation found will null.
         *
         * @param attribute
         * @param annotationClass
         * @param <A>
         * @return annotation or null if not found.
         */
        public <A extends Annotation> A attributeAnnotation(Attribute attribute, Class<A> annotationClass) {
            Map<Class, Annotation> annotations = attributeAnnotations.computeIfAbsent(attribute, k -> Maps.newConcurrentMap());
            Annotation annotation = annotations.computeIfAbsent(annotationClass, k -> {
                Member member = attribute.getJavaMember();
                return findAnnotation(member, k);
            });
            //if no annotation found and already mapped annotations is empty. perform clear
            if (annotation == null && annotations.size() == 1) {
                if (!attributeAnnotations.remove(attribute, annotations)) {
                    annotations.remove(annotationClass);
                }
            }
            return (A) annotation;
        }

        /**
         * return {@link ManagedType}
         *
         * @return
         */
        public ManagedType<T> managedType() {
            return managedType;
        }


        /**
         * return true if current {@link ManagedType} can assigned to {@link EntityType}
         *
         * @return
         */
        public boolean isEntityType() {
            return managedType instanceof EntityType;
        }

        /**
         * return true if current {@link ManagedType} can assigned to {@link EmbeddableType}
         *
         * @return
         */
        public boolean isEmbeddableType() {
            return managedType instanceof EmbeddableType;
        }

        /**
         * convert current managed type as {@link EntityType}
         *
         * @return entity type or throws {@link IllegalArgumentException} not an entity type.
         */
        public EntityType<T> asEntityType() {
            Assert.isTrue(isEntityType(), "not an entity type.");
            return (EntityType<T>) managedType();
        }

        /**
         * return table name if current managed type is a {@link EntityType}
         *
         * @return table name or throw {@link IllegalArgumentException}(not an entity type or no @Table declared. )
         */
        public String table() {
            if (this.table != null)
                return table;
            EntityType<T> entityType = asEntityType();
            Table tableAnnotation = AnnotationUtils.findAnnotation(entityType.getJavaType(), Table.class);
            Assert.notNull(tableAnnotation, "no @Table declared from: " + entityType);
            this.table = tableAnnotation.name();
            return table;
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
         * return true if given name is a persist attribute.
         *
         * @param name
         * @return
         */
        public boolean isAttribute(String name) {
            return attributes.containsKey(name);
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
         * return true if given name is a single value persist attribute.
         *
         * @param name
         * @return
         */
        public boolean isSingularAttribute(String name) {
            return singularAttributes.containsKey(name);
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

        /**
         * return id column name.
         *
         * @return
         */
        public String getColumnOfId() {
            return columnOfId;
        }

        /**
         * 返回属性关联的表（仅单值关联属性以及关联集合元素属性对应的表名会被返回，其他情况返回null）
         *
         * @param attribute
         * @return
         */
        protected String obtainDatabaseTableFromAttribute(Attribute attribute) {
            if (attribute.isAssociation()) {
                Class attributeJavaType;
                if (attribute.isCollection()) {
                    attributeJavaType = ((PluralAttribute) attribute).getElementType().getJavaType();
                } else
                    attributeJavaType = attribute.getJavaType();
                Table table = AnnotationUtils.findAnnotation(attributeJavaType, Table.class);
                Assert.notNull(table, "can not found @Table on :" + attributeJavaType);
                return table.name();
            }
            return null;
        }


        /**
         * 返回属性对应的数据库列
         * 会有多个column 返回的情况，例如 {@link Embedded} 属性 会关联一组column.
         *
         * @param attribute
         * @return
         */
        protected String[] obtainDatabaseColumnsFromAttribute(Attribute attribute) {
            Member member = attribute.getJavaMember();
            Id id = findAnnotation(member, Id.class);
            if (null != id) {
                Column column = findAnnotation(member, Column.class);
                columnOfId = null != column ? column.name() : "id";
                return new String[]{columnOfId};
            }
            Column column = findAnnotation(member, Column.class);
            if (null != column)
                return new String[]{column.name()};
            JoinColumn joinColumn = findAnnotation(member, JoinColumn.class);
            if (null != joinColumn)
                return new String[]{joinColumn.name()};
            if (null != findAnnotation(member, Embedded.class)) {
                AttributeOverrides attributeOverrides = findAnnotation(member, AttributeOverrides.class);
                if (null != attributeOverrides) {
                    AttributeOverride[] overrides = attributeOverrides.value();
                    return Stream.of(overrides)
                            .map(attributeOverride -> attributeOverride.column().name())
                            .collect(Collectors.toList())
                            .toArray(new String[overrides.length]);
                }
                EmbeddableType embeddable = getRequiredEntityManagerFactory().getMetamodel().embeddable(attribute.getJavaType());
                List<String> embeddableColumns = Lists.newArrayList();
                for (Object o : embeddable.getAttributes()) {
                    Column embeddableColumn = findAnnotation(((Attribute) o).getJavaMember(), Column.class);
                    if (null != embeddableColumn) {
                        embeddableColumns.add(embeddableColumn.name());
                    }
                }
                return embeddableColumns.toArray(new String[embeddableColumns.size()]);
            }
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }


    }

    private static <A extends Annotation> A findAnnotation(
            Member member, Class<A> annotationType) {
        Assert.isInstanceOf(AnnotatedElement.class, member, "attribute member not an instance of AnnotatedElement: " + member);
        return AnnotationUtils.findAnnotation((AnnotatedElement) member, annotationType);
    }


    private static EntityManagerFactory getRequiredEntityManagerFactory() {
        return getRequiredEntityManagerFactoryBean().getObject();
    }

    private static AbstractEntityManagerFactoryBean getRequiredEntityManagerFactoryBean() {
        return FrameworkContext.getBean(AbstractEntityManagerFactoryBean.class);
    }
}
