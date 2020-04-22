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
package org.scleropages.crud.dao.orm.jpa.complement;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.scleropages.core.util.Collections3;
import org.scleropages.crud.dao.orm.SearchFilter;
import org.scleropages.crud.dao.orm.jpa.JpaContexts;
import org.scleropages.crud.dao.orm.jpa.JpaContexts.ManagedTypeModel;
import org.springframework.util.Assert;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.jooq.impl.DSL.*;

/**
 * Similar with {@link org.scleropages.crud.dao.orm.jpa.DynamicSpecifications} used to convert a group of SearchFilters applying to {@link SelectQuery}.
 * <pre>
 *     This class must complement with JPA environments. query builder base jpa annotations. it's translate jpa entity relations to condition.
 *     The {@link SearchFilter} support basic, single value association,embedded property. but not support collection property（Because there will be duplicate records and too many table joins（3））.
 *          NOTE: You must ensure the accessibility of one-way associations. where the property navigation is always from the relationship maintainer to the target entity.
 *     This class will auto applying join relationship when association property . So Please don't add join to SelectQuery.
 *
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class DynamicJpaSupportJooqConditions {


    /**
     * create condition by given query and {@link SearchFilter}s.
     * {@link ManagedTypeModel} is a jpa entity managed type model. it's will applying search filter field name in entity context.
     * <pre>
     * support basic, single value association,embedded property. but not support collection property（Because there will be duplicate records and too many table joins（>=3. this will causes database slow query）.
     * NOTE: You must ensure the accessibility of one-way associations. where the property navigation is always from the relationship maintainer to the target entity.
     * This method will auto applying join relationship when association property . So please don't add join to SelectQuery.
     * </pre>
     *
     * @param query
     * @param root
     * @param filters
     * @return
     */
    public static Condition bySearchFilters(SelectQuery query, final ManagedTypeModel<?> root, final Collection<SearchFilter> filters) {
        Assert.notNull(query, "given query must not be null.");
        Assert.notNull(root, "given root of ManagedTypeModel must not be null.");
        Assert.isTrue(root.isEntityType(), "given ManagedTypeModel not an instance of EntityType: " + root.managedType().getJavaType());
        if (Collections3.isEmpty(filters)) {
            return trueCondition();
        }
        List<Condition> allConditions = Lists.newArrayList();
        for (SearchFilter filter : filters) {
            List<Condition> searchFilterConditions = Lists.newArrayList();
            for (String propertyName : filter.fieldNames) {
                String[] pathNames = StringUtils.split(propertyName, "$");
                if (pathNames.length > 1) {//nested property
                    processNestedProperty(filter, searchFilterConditions, query, pathNames, root, null, null, null, -1);
                } else {
                    Column column = root.attributeAnnotation(root.attribute(propertyName), Column.class);
                    Assert.notNull(column, () -> "no @column declared found: " + propertyName + " from: " + root.managedType().getJavaType());
                    processBasicProperty(field(name(root.table(), column.name())), filter, searchFilterConditions);
                }
            }
            if (searchFilterConditions.size() == 1) {
                allConditions.add(searchFilterConditions.get(0));
            } else if (searchFilterConditions.size() > 1) {
                if (SearchFilter.Conjunction.AND.equals(filter.conjunction))
                    allConditions.add(and(searchFilterConditions));
                else if (SearchFilter.Conjunction.OR.equals(filter.conjunction))
                    allConditions.add(or(searchFilterConditions));
            }
        }
        // and all conditions.
        if (!allConditions.isEmpty()) {
            return and(allConditions);
        }
        return trueCondition();
    }


    private static void processNestedProperty(SearchFilter filter, List<Condition> conditions, SelectQuery query, String[] pathNames, ManagedTypeModel<?> previousManagedTypeModel, String previousNestedProperty, ManagedTypeModel<?> currentManagedTypeModel, String currentNestedProperty, int index) {
        Assert.notNull(filter, "filter must not be null.");
        Assert.isTrue(null != pathNames && pathNames.length > 1, "null or empty pathNames or pathNames length < 2.");
        Assert.notNull(previousManagedTypeModel, "previousManagedTypeModel or (root) must not be null.");
        final Class previousManagedJavaType = previousManagedTypeModel.managedType().getJavaType();
        if (StringUtils.isBlank(currentNestedProperty)) {//call first. foreach pathNames.
            String loopNextProperty;
            ManagedTypeModel loopNextModel;
            for (int i = 0; i < pathNames.length; i++) {
                loopNextProperty = pathNames[i];
                //when iterators to last nested property is's a basic property. the currentManagedTypeModel will be null.
                loopNextModel = i == pathNames.length - 1 ? null : JpaContexts.getManagedTypeModel(previousManagedTypeModel.attribute(loopNextProperty).getJavaType());
                processNestedProperty(filter, conditions, query, pathNames, previousManagedTypeModel, previousNestedProperty, loopNextModel, loopNextProperty, i);
                previousManagedTypeModel = loopNextModel;
                previousNestedProperty = loopNextProperty;
            }
        }
        if (currentManagedTypeModel.isEmbeddableType()) {
            //embedded property not need to join noting to do and return directly.
            return;
        }
        Attribute<?, Object> currentAttribute = previousManagedTypeModel.attribute(currentNestedProperty);
        if (index < pathNames.length - 1) {//when iterators to last nested property is's a basic property.don't need to join.
            Assert.isTrue(currentAttribute.isAssociation(), () -> "not an association attribute: " + currentNestedProperty + " from:" + previousManagedJavaType);

            //Assert.isTrue(!currentAttribute.isCollection(), () -> "not support collection attribute: " + currentNestedProperty + " from: " + previousManagedJavaType);
            if (currentAttribute.isCollection()) {
                processCollectionAttributeJoin(query, pathNames, previousManagedTypeModel, previousNestedProperty, currentManagedTypeModel, currentNestedProperty, currentAttribute, index);
            }

            JoinColumn joinColumn = previousManagedTypeModel.attributeAnnotation(currentAttribute, JoinColumn.class);
            Assert.notNull(joinColumn, () -> "can not found @JoinColumn from: " + previousManagedJavaType + "." + currentNestedProperty);


            Table joinTable = table(name(currentManagedTypeModel.table()));
            TableField[] primaryKey = joinTable.getPrimaryKey().getFieldsArray();
            Assert.isTrue(primaryKey.length == 1, () -> "could not determined join column. because found an multi fields as primary key: " + currentNestedProperty + "from: " + previousManagedJavaType);
            query.addJoin(joinTable, table(name(previousManagedTypeModel.table())).field(joinColumn.name()).eq(primaryKey[0]));
            return;
        }
        //here to process last nested property. it's a basic attribute
        Assert.isTrue(Objects.equals(currentAttribute.getPersistentAttributeType(),
                PersistentAttributeType.BASIC), () -> "The last of nested property must be a basic property(used for build condition): " + currentNestedProperty + "from: " + previousManagedJavaType);

        Field field = null;
        if (previousManagedTypeModel.isEmbeddableType()) {
            //if AttributeOverrides defined use AttributeOverride column to build field.
            AttributeOverrides attributeOverrides = previousManagedTypeModel.attributeAnnotation(currentAttribute, AttributeOverrides.class);
            if (null != attributeOverrides) {
                for (int i = 0; i < attributeOverrides.value().length; i++) {
                    AttributeOverride attributeOverride = attributeOverrides.value()[i];
                    if (Objects.equals(attributeOverride.name(), currentNestedProperty)) {
                        field = DSL.field(name(previousManagedTypeModel.table(), attributeOverride.column().name()));
                        break;
                    }
                }
            }//or else(not a declared AttributeOverrides and AttributeOverride->column name not matches.) use embedded type property field(declared a @Column)
            if (null == field) {
                Column column = currentManagedTypeModel.attributeAnnotation(currentAttribute, Column.class);
                Assert.notNull(column, () -> "could not determined embedded column. either @AttributeOverrides and Embedded Type field not defined column information: " + currentNestedProperty + " from: " + previousManagedJavaType);
                field = DSL.field(name(previousManagedTypeModel.table(), column.name()));
            }
        } else if (previousManagedTypeModel.isEntityType()) {
            Column column = currentManagedTypeModel.attributeAnnotation(currentAttribute, Column.class);
            field = DSL.field(name(previousManagedTypeModel.table(), column.name()));
        } else
            throw new IllegalArgumentException("unknown managed type model: " + previousManagedJavaType);
        processBasicProperty(field, filter, conditions);
    }

    private static void processCollectionAttributeJoin(SelectQuery query, String[] pathNames, ManagedTypeModel<?> previousManagedTypeModel, String previousNestedProperty, ManagedTypeModel<?> currentManagedTypeModel, String currentNestedProperty, Attribute<?, Object> currentAttribute, int index) {
        Assert.isTrue(false, () -> "not support collection attribute: " + currentNestedProperty + " from: " + previousManagedTypeModel.managedType().getJavaType());
        //several possibles:
        //
        //@OneToMany(mappedBy = "target")反向关联，需根据类型以及属性名称找到对应的关系维护方属性上确认@ManyToOne以及@JoinColumn
        //
        //@OneToMany 单向一对多关联，但需要join3张表
        //@JoinTable(name = "source_target", joinColumns = { @JoinColumn(name = "source_id") }, inverseJoinColumns = { @JoinColumn(name = "target_id") })
        //-------------------------------------------------------------------------------------------------------------------------------------------------
        //@ManyToMany(mappedBy = "target")反向关联，需根据类型以及属性名称找到对应的关系维护方属性上确认@ManyToMany以及@JoinColumn，并join3张表
        //@ManyToMany 单（双）向关联
        //@JoinTable(name = "source_target", joinColumns = { @JoinColumn(name = "source_id") }, inverseJoinColumns = { @JoinColumn(name = "target_id") })

        //实际业务场景不多且会产生重复记录（需要二次处理）3张join较影响性能.. may supports in feature........暂定.....

    }

    public static Condition bySearchFilter(Field field, SearchFilter filter) {
        if (SearchFilter.VALUE_IS_NULL.equals(filter.value))
            return field.isNull();
        if (SearchFilter.VALUE_IS_NOT_NULL.equals(filter.value))
            return field.isNotNull();
        switch (filter.operator) {
            case EQ:
                return field.eq(filter.value);
            case NEQ:
                return field.ne(filter.value);
            case LIKE:
                return field.like("%" + filter.value + "%");
            case NLIKE:
                return field.notLike("%" + filter.value + "%");
            case GT:
                return field.gt(filter.value);
            case LT:
                return field.lt(filter.value);
            case GTE:
                return field.gt(filter.value).or(field.eq(filter.value));
            case LTE:
                return field.lt(filter.value).or(field.eq(filter.value));
            case NULL:
                return field.isNull();
            case NOTNULL:
                return field.isNotNull();
            case IN:
                return field.in(StringUtils.split(String.valueOf(filter.value), ","));
            default:
                throw new IllegalArgumentException("unsupported filter operator: " + filter.operator.name());
        }
    }


    private static void processBasicProperty(Field field, SearchFilter filter, List<Condition> conditions) {
        conditions.add(bySearchFilter(field, filter));
    }
}
