/*******************************************************************************
 * Copyright (c) 2005, 2014 springside.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package org.scleropages.crud.orm.jpa;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.scleropages.core.util.Collections3;
import org.scleropages.crud.orm.SearchFilter;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DynamicSpecifications {

    public static <T> Specification<T> bySearchFilter(final Collection<SearchFilter> filters,
                                                      final Class<T> entityClazz, final FieldFetch[] fieldFetches) {
        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
                if (Long.class != query.getResultType() && ArrayUtils.isNotEmpty(fieldFetches)) {
                    for (FieldFetch fieldFetch : fieldFetches) {
                        root.fetch(fieldFetch.getField(), fieldFetch.getJoinType());
                    }
                }
                if (Collections3.isEmpty(filters)) {
                    return builder.conjunction();
                }

                List<Predicate> predicates = Lists.newArrayList();

                for (SearchFilter filter : filters) {
                    List<Predicate> filterPredicates = Lists.newArrayList();
                    for (String fieldName : filter.fieldNames) {
                        // nested path translate,
                        // 如Task的名为"user.name"的filedName,
                        // 转换为Task.user.name属性
                        String[] names = StringUtils.split(fieldName, "$");
                        Path expression = root.get(names[0]);
                        for (int i = 1; i < names.length; i++) {
                            expression = expression.get(names[i]);
                        }
                        if (SearchFilter.VALUE_IS_NULL.equals(filter.value)) {
                            filterPredicates.add(builder.isNull(expression));
                            continue;
                        }
                        if (SearchFilter.VALUE_IS_NOT_NULL.equals(filter.value)) {
                            filterPredicates.add(builder.isNotNull(expression));
                            continue;
                        }
                        switch (filter.operator) {
                            case EQ:
                                filterPredicates.add(builder.equal(expression, filter.value));
                                break;
                            case NEQ:
                                filterPredicates.add(builder.notEqual(expression, filter.value));
                                break;
                            case LIKE:
                                filterPredicates.add(builder.like(expression, "%" + filter.value + "%"));
                                break;
                            case NLIKE:
                                filterPredicates.add(builder.notLike(expression, "%" + filter.value + "%"));
                                break;
                            case GT:
                                filterPredicates.add(builder.greaterThan(expression, (Comparable) filter.value));
                                break;
                            case LT:
                                filterPredicates.add(builder.lessThan(expression, (Comparable) filter.value));
                                break;
                            case GTE:
                                filterPredicates.add(builder.greaterThanOrEqualTo(expression, (Comparable) filter.value));
                                break;
                            case LTE:
                                filterPredicates.add(builder.lessThanOrEqualTo(expression, (Comparable) filter.value));
                                break;
                            case NULL:
                                filterPredicates.add(builder.isNull(expression));
                                break;
                            case NOTNULL:
                                filterPredicates.add(builder.isNotNull(expression));
                                break;
                            case IN:
                                Object[] values = StringUtils.split((String) filter.value, ",");
                                filterPredicates.add(expression.in(values));
                        }
                    }
                    if (filterPredicates.size() == 1) {
                        predicates.add(filterPredicates.get(0));
                    } else if (filterPredicates.size() > 1) {
                        if (SearchFilter.Conjunction.AND.equals(filter.conjunction))
                            predicates
                                    .add(builder.and(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
                        else if (SearchFilter.Conjunction.OR.equals(filter.conjunction))
                            predicates
                                    .add(builder.or(filterPredicates.toArray(new Predicate[filterPredicates.size()])));
                    }

                }

                // 将所有条件用 and 联合起来
                if (!predicates.isEmpty()) {
                    return builder.and(predicates.toArray(new Predicate[predicates.size()]));
                }
                return builder.conjunction();
            }
        };
    }

    public static <T> Specification<T> bySearchFilter(final Collection<SearchFilter> filters,
                                                      final Class<T> entityClazz, final String[] fetchAttributesNames, final JoinType joinType) {
        FieldFetch[] fieldFetchs = null;
        if (ArrayUtils.isNotEmpty(fetchAttributesNames)) {
            fieldFetchs = new FieldFetch[fetchAttributesNames.length];
            for (int i = 0; i < fetchAttributesNames.length; i++) {
                fieldFetchs[i] = new FieldFetch(fetchAttributesNames[i], joinType);
            }
        }
        return bySearchFilter(filters, entityClazz, fieldFetchs);
    }

    public static <T> Specification<T> bySearchFilter(final Collection<SearchFilter> filters,
                                                      final Class<T> entityClazz) {
        return bySearchFilter(filters, entityClazz, null, null);
    }
}
