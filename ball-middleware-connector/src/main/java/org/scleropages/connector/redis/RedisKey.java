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
package org.scleropages.connector.redis;

import io.lettuce.core.cluster.SlotHash;
import org.apache.commons.lang3.StringUtils;
import org.scleropages.core.util.Namings;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;

/**
 * 规范redis key 命名 [catalog]/[type]:[id]:[property_name][{slot_key}]<br>
 * <pre>
 * catalog--->业务分类
 * type---->业务实体类型
 * id----->业务实体唯一标识
 * property_name---->业务属性
 * slot_key---->哈希槽计算key，相同key会被redis cluster map到固定槽位
 * <b>NOTE: 建议所有域命名上应使用 snake-case（单次全部小写通过下划线分隔）即关系型数据的命名策略</b>
 * </pre>
 *
 * <pre>
 * examples:
 *
 * users/app_member:3519231:email（with catalog,type,id,property）
 * users/wechat_member:oYCdh5jn0JpRMyGtiAA5HWBmbRcY(with catalog,type,id)
 *
 * topic:351231(with type and id)
 * topic:351231:title(with type and id and property)
 *
 * online_user:oYCdh5jn0JpRMyGtiAA5HWBmbRcY{wechat_member}(slot always mapped to crc16({app_member}) mod 16384)
 *
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface RedisKey {

    String evalKey();

    String getCatalog();

    String getType();

    String getId();

    String getPropertyName();

    int computeSlot();


    final class RedisKeyBuilder {

        private static final char FIELD_SEPARATOR = ':';

        private static final char TYPE_SEPARATOR = '/';

        private static final char SLOT_KEY_START = '{';

        private static final char SLOT_KEY_END = '}';

        private static final char[] ILLEGAL_CHARACTERS;

        static {
            ILLEGAL_CHARACTERS = new char[]{FIELD_SEPARATOR, TYPE_SEPARATOR, SLOT_KEY_START, SLOT_KEY_END};
        }

        private String catalog;

        private String type;

        private String id;

        private String propertyName;

        private String slotKey;

        private String evalKey;


        private void checkField(String source) {
            if (StringUtils.containsAny(source, ILLEGAL_CHARACTERS))
                throw new IllegalArgumentException("invalid characters in: " + Arrays.toString(ILLEGAL_CHARACTERS));
        }

        private RedisKeyBuilder(String catalog, String type) {
            checkField(catalog);
            checkField(type);
            this.catalog = catalog;
            this.type = type;
        }

        public static RedisKeyBuilder fromType(String type) {
            Assert.hasText(type, "type must not empty.");
            return new RedisKeyBuilder(null, type);
        }

        public static RedisKeyBuilder fromType(Class type) {
            Assert.notNull(type, "type must not empty.");
            return fromType(format(type));
        }


        public static RedisKeyBuilder fromCatalogAndType(String catalog, Class type) {
            return fromCatalogAndType(catalog, format(type));
        }

        public static RedisKeyBuilder fromCatalogAndType(String catalog, String type) {
            Assert.hasText(catalog, "catalog must not empty.");
            Assert.hasText(type, "type must not empty.");
            return new RedisKeyBuilder(catalog, type);
        }

        public static RedisKeyBuilder fromKey(String key) {
            Assert.hasText(key, "key must  not empty.");
            String[] splitFields = StringUtils.split(key, FIELD_SEPARATOR);
            RedisKeyBuilder redisKeyBuilder;
            String[] catalogAndType = StringUtils.split(splitFields[0], TYPE_SEPARATOR);//split catalog and type
            if (catalogAndType.length == 2) {// has catalog and type
                String[] fieldAndSlotKey = splitSlotKey(catalogAndType[1]);
                redisKeyBuilder = fromCatalogAndType(catalogAndType[0], fieldAndSlotKey[0]);
                if (fieldAndSlotKey.length > 1) {
                    redisKeyBuilder.withSlotKey(fieldAndSlotKey[1]);
                    return redisKeyBuilder;// slot key is last part of key. return directly if occur
                }
            } else {// has type only
                String[] fieldAndSlotKey = splitSlotKey(catalogAndType[1]);
                redisKeyBuilder = fromType(fieldAndSlotKey[0]);
                if (fieldAndSlotKey.length > 1) {
                    redisKeyBuilder.withSlotKey(fieldAndSlotKey[1]);
                    return redisKeyBuilder;
                }
            }
            if (splitFields.length > 1) {//split id
                String[] fieldAndSlotKey = splitSlotKey(splitFields[1]);
                redisKeyBuilder.withId(fieldAndSlotKey[0]);
                if (fieldAndSlotKey.length > 1) {
                    redisKeyBuilder.withSlotKey(fieldAndSlotKey[1]);
                    return redisKeyBuilder;
                }
            }
            if (splitFields.length > 2) {//split property name
                String[] fieldAndSlotKey = splitSlotKey(splitFields[2]);
                redisKeyBuilder.withId(fieldAndSlotKey[0]);
                if (fieldAndSlotKey.length > 1) {
                    redisKeyBuilder.withSlotKey(fieldAndSlotKey[1]);
                    return redisKeyBuilder;
                }
            }
            return redisKeyBuilder;
        }

        private static String[] splitSlotKey(String field) {
            String[] splited = StringUtils.split(field, SLOT_KEY_START);
            if (splited.length == 2) {
                String slotKey = splited[1];
                if ((field.charAt(slotKey.length() - 1) == SLOT_KEY_END)) {//slotKey must end with '}' is a valid format
                    return new String[]{splited[0], slotKey.substring(0, slotKey.length() - 1)};
                }
                throw new IllegalArgumentException("invalid slot key (not end with '}') from: " + field);
            } else if (splited.length == 1) {// no slot key found, return single source field directly
                return splited;
            } else //find more than one slot keys.
                throw new IllegalArgumentException("find more than one slot key from: " + field);
        }

        public RedisKeyBuilder withSlotKey(String slotKey) {
            checkField(slotKey);
            this.slotKey = slotKey;
            return this;
        }

        public RedisKeyBuilder withId(String id) {
            checkField(id);
            this.id = id;
            return this;
        }

        public RedisKeyBuilder withIdAndProperty(String id, String propertyName) {
            withId(id);
            checkField(propertyName);
            this.propertyName = propertyName;
            return this;
        }


        public RedisKey build() {
            return build(true);
        }


        public RedisKey build(boolean useSnakeCase) {
            return new RedisKey() {
                @Override
                public String evalKey() {
                    if (null != evalKey)
                        return evalKey;
                    StringBuilder sb = new StringBuilder();
                    boolean hasPrefix = false;
                    if (StringUtils.isNotBlank(catalog)) {
                        hasPrefix = true;
                        sb.append(catalog);
                    }
                    if (StringUtils.isNotBlank(type)) {
                        if (hasPrefix) {
                            sb.append('/');
                        }
                        sb.append(type);
                        hasPrefix = true;
                    }
                    if (StringUtils.isNotBlank(id)) {
                        if (hasPrefix) {
                            sb.append(':');
                        }
                        sb.append(id);
                        hasPrefix = true;
                    }
                    if (StringUtils.isNotBlank(propertyName)) {
                        if (hasPrefix) {
                            sb.append(':');
                        }
                        sb.append(propertyName);
                    }
                    if (StringUtils.isNotBlank(slotKey)) {
                        sb.append(SLOT_KEY_START);
                        sb.append(slotKey);
                        sb.append(SLOT_KEY_END);
                    }
                    if (useSnakeCase) {
                        return Namings.snakeCaseName(sb.toString());
                    }
                    evalKey = sb.toString();
                    return evalKey;
                }

                @Override
                public String getCatalog() {
                    return catalog;
                }

                @Override
                public String getType() {
                    return type;
                }

                @Override
                public String getId() {
                    return id;
                }

                @Override
                public String getPropertyName() {
                    return propertyName;
                }

                @Override
                public int computeSlot() {
                    try {
                        return SlotHash.getSlot(evalKey().getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public int hashCode() {
                    return evalKey().hashCode();
                }

                @Override
                public String toString() {
                    return evalKey();
                }

                @Override
                public boolean equals(Object obj) {
                    if (obj == null || !ClassUtils.isAssignableValue(this.getClass(), obj))
                        return false;
                    return Objects.equals(evalKey(), ((RedisKey) obj).evalKey());
                }
            };


        }

        private static String format(Class type) {
            return type.getSimpleName();
        }
    }

}
