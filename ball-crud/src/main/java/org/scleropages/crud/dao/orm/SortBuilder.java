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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Represents sort used for as order to fetch data from repository.
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class SortBuilder {

    /**
     * sort parameters prefix.
     */
    public static final String SORT_PREFIX = "sort_";

    /**
     *
     */
    public static final String SORT_FIELDS_PARAM_NAME = "fields";

    public static final String SORT_DIRECTION_PARAM_NAME = "direction";

    public static final String DEFAULT_SORT_FIELDS = "id";

    public static final Direction DEFAULT_DIRECTION = Direction.DESC;

    public static final Sort DEFAULT_SORT = Sort.by(DEFAULT_DIRECTION, DEFAULT_SORT_FIELDS);

    public static final Sort DEFAULT_NO_SORT = Sort.unsorted();

    /**
     * A easy way to build {@link Sort} from given sort map. make sure sort params keys with same prefix '{@link #SORT_PREFIX}'.
     * The key of {@link #SORT_FIELDS_PARAM_NAME} as sort field. and {@link #DEFAULT_DIRECTION} as sort direction.<br>
     * The sort field must defined as follow format:
     * <pre>
     *     sort_fields: fieldName1_fieldName2_fieldNameN...(use '_' as filed separator)
     * </pre>
     * The sort direction as follow:
     * <pre>
     *     sort_direction: {@link Direction#ASC} or {@link Direction#DESC}
     * </pre>
     *
     * @param sortParams
     * @param defaultSortField if sortParams is empty use this as default sort field with {@link #DEFAULT_DIRECTION}.
     * @return
     */
    public static Sort build(Map<String, Object> sortParams, String defaultSortField) {
        if (CollectionUtils.isEmpty(sortParams)) {
            if (StringUtils.isBlank(defaultSortField))
                return DEFAULT_NO_SORT;
            else
                return Sort.by(DEFAULT_DIRECTION, defaultSortField);
        }
        List<Order> orders = Lists.newArrayList();

        Object _sortBy = sortParams.get(SORT_FIELDS_PARAM_NAME);
        Object _direction = sortParams.get(SORT_DIRECTION_PARAM_NAME);

        String sortBy = null == _sortBy || StringUtils.isBlank(_sortBy.toString()) ? DEFAULT_SORT_FIELDS
                : _sortBy.toString();

        Direction direction = null == _direction || StringUtils.isBlank(_direction.toString()) ? DEFAULT_DIRECTION
                : Direction.fromString(_direction.toString());

        String[] fields = StringUtils.split(sortBy, "_");
        for (String field : fields) {
            Order order = new Order(direction, field);
            orders.add(order);
        }
        return Sort.by(orders);
    }

    /**
     * A easy way to build {@link Sort} from given sort map. make sure sort params keys with same prefix 'sort_'.
     * The key of {@link #SORT_FIELDS_PARAM_NAME} as sort field. and {@link #DEFAULT_DIRECTION} as sort direction.
     * The sort field must defined as follow format:
     * <pre>
     *     fieldName1_fieldName2_fieldNameN...(use '_' as filed separator)
     * </pre>
     * The sort direction as follow:
     * <pre>
     *     {@link Direction#ASC}
     *     {@link Direction#DESC}
     * </pre>
     *
     * @param sortParams
     * @return
     */
    public static Sort build(Map<String, Object> sortParams) {
        return build(sortParams, "");
    }
}
