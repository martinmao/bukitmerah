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

import org.springframework.data.redis.core.ClusterOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface RedisClient {

    ClusterOperations<RedisKey, String> clusterOperations();

    ValueOperations<RedisKey, String> stringValueOperations();

    HashOperations<RedisKey, String, String> stringHashOperations();

    ListOperations<RedisKey, String> stringListOperations();

    SetOperations<RedisKey, String> stringSetOperations();

    ZSetOperations<RedisKey, String> stringZSetOperations();

    RedisTemplate<RedisKey, String> stringValueTemplate();

    ValueOperations<RedisKey, Object> objectValueOperations();

    HashOperations<RedisKey, String, Object> objectHashOperations();

    ListOperations<RedisKey, Object> objectListOperations();

    SetOperations<RedisKey, Object> objectSetOperations();

    ZSetOperations<RedisKey, Object> objectZSetOperations();

    RedisTemplate<RedisKey, Object> objectValueTemplate();
}
