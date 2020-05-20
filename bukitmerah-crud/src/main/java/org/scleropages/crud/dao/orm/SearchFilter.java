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
package org.scleropages.crud.dao.orm;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Map.Entry;


/**
 * Represents search filter used for as condition to fetch data from repository.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class SearchFilter {

    /**
     * Enumerate a set of query operators. used for as a condition to fetch data from repository.
     * <pre>
     * {@link #EQ}: =
     * {@link #NEQ}: !=
     * {@link #LIKE}: like
     * {@link #GT}: >
     * {@link #LT}: <
     * {@link #GTE}：>=
     * {@link #LTE}: <=
     * {@link #NULL}: is null
     * {@link #NOTNULL}: is not null
     * {@link #IN}: in
     * {@link #NLIKE}: not like
     * {@link #RANGE}: >=x or <= y
     * {@link #RANGEIN}: >x or < y
     * </pre>
     */
    public enum Operator {
        EQ, NEQ, LIKE, GT, LT, GTE, LTE, NULL, NOTNULL, IN, NLIKE, RANGE, RANGEIN
    }

    /**
     * Enumerate a set of conjunction. make the {@link Operator} conjunctive.
     * <pre>
     *     {@link #OR}: or
     *     {@link #AND}: and
     * </pre>
     */
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
     * Utility class for build list of search filters by given map.
     */
    public static class SearchFilterBuilder {

        public static final String SEARCH_PREFIX = "search_";

        /**
         * A easy way to build a {@link SearchFilter}(s) map by given search params.<br>
         * given map keys format is: [Operator]_[Conjunction]_fieldName1_[fieldNameN...].<br>
         * The {@link Operator} and {@link Conjunction} is optional. that means use {@link Operator#EQ} as default operator and no conjunction
         * <pre>
         * eg: name->rabbit(name equals to 'rabbit')
         * eg: GTE_age->30(age >= 30)
         * eg: LIKE_OR_firstName_lastName->rabbit(first name or last name like 'rabbit')
         * eg: email->IS_NOT_NULL/IS_NULL(email is not null/is null). Can also be expressed as: NULL/NOTNULL_email->any value(must not be empty text.)
         * </pre>
         *
         * @param searchParams
         * @return
         * @see Operator
         * @see Conjunction
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
