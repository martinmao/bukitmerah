/*******************************************************************************
 * Copyright (c) 2005, 2014 springside.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package org.scleropages.crud.dao.orm;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Map.Entry;

public class SearchFilter {
    public enum Operator {
        EQ, NEQ, LIKE, GT, LT, GTE, LTE, NULL, NOTNULL, IN, NLIKE
    }

    public enum Conjunction {
        OR, AND
    }

    public static final String VALUE_IS_NULL = "IS_NULL";
    public static final String VALUE_IS_NOT_NULL = "IS_NOT_NULL";

    public String[] fieldNames;
    public Object value;
    public Operator operator;
    public Conjunction conjunction;

    public SearchFilter(String[] fieldNames, Operator operator, Conjunction conjunction, Object value) {
        this.fieldNames = fieldNames;
        this.value = value;
        this.operator = operator;
        this.conjunction = conjunction;
    }

    public SearchFilter(String fieldName, Operator operator, Object value) {
        this.fieldNames = new String[]{fieldName};
        this.value = value;
        this.operator = operator;
    }

    /**
     * Util class for build list of search filters by given map.
     */
    public static class SearchFilterBuilder {

        public static final String SEARCH_PREFIX = "search_";

        /**
         * given map key format is
         * :OPERATOR_[CONJUNCTION]_FIELDNAME_[FIELDNAME...].<br>
         * eg:LIKE_fieldName<br>
         * eg:LIKE_OR_filedName1_fieldName2<br>
         *
         * @param searchParams
         * @return
         */
        public static Map<String, SearchFilter> build(Map<String, Object> searchParams) {

            Map<String, SearchFilter> filters = Maps.newHashMap();

            for (Entry<String, Object> entry : searchParams.entrySet()) {
                // 过滤掉空值
                String key = entry.getKey();
                Object value = entry.getValue();
                if (null == value || StringUtils.isBlank(value.toString())) {
                    continue;
                }
                SearchFilter filter;

                String[] names = StringUtils.split(key, "_");
                if (names.length == 1) {/* no operator and conjunction. by default. used EQ as operator */
                    filter = new SearchFilter(names[0], Operator.EQ, value);
                } else if (names.length == 2) {/* no conjunction. apply operator and field*/
                    filter = new SearchFilter(names[1], Operator.valueOf(names[0]), value);
                } else if (names.length > 2) {
                    String[] fields = new String[]{};
                    for (int i = 2; i < names.length; i++) {
                        fields = ArrayUtils.add(fields, names[i]);
                    }
                    filter = new SearchFilter(fields, Operator.valueOf(names[0]), Conjunction.valueOf(names[1]), value);
                } else
                    throw new IllegalArgumentException(key + " is not a valid search filter name");

                filters.put(key, filter);
            }

            return filters;
        }
    }

}
