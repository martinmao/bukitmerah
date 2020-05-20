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

import com.google.common.collect.Maps;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public class DefaultRedisClient extends SpringRedisClientSupport implements RedisClient {


    /*记录各种key condition状态, 如counter达到指定值，on-off开关...，默认初始状态false*/
    private final Map<RedisKey, AtomicBoolean> redisKeyConditionStates = Maps.newConcurrentMap();


    public DefaultRedisClient(RedisTemplate<RedisKey, String> stringRedisTemplate, RedisTemplate<RedisKey, Object> objectRedisTemplate) {
        super(stringRedisTemplate, objectRedisTemplate);
    }


    /**
     * 创建Condition并初始化key值，仅确保当前进程原子创建一次，如果key已存在则抛出 {@link IllegalStateException}
     *
     * @param redisKey
     * @param initial
     */
    public void createCondition(RedisKey redisKey, String initial) {
        redisKeyConditionStates.computeIfAbsent(redisKey, key -> {
            Assert.state(stringValueOperations().setIfAbsent(redisKey, initial), redisKey.evalKey() + " already exists.");
            return new AtomicBoolean(false);
        });
    }

    /**
     * 重置Condition并初始化key值，仅确保当前进程原子重置一次
     *
     * @param redisKey
     * @param initial 为null时，仅重置condition，不会更新redis
     * @return true if operation success
     */
    public boolean resetCondition(RedisKey redisKey, String initial) {
        if (getConditionState(redisKey).compareAndSet(true, false)) {
            if (null != initial)
                return stringValueOperations().setIfPresent(redisKey, initial);
        }
        return false;
    }

    /**
     * 删除Condition，condition必须存在且(condition=true)才会删除，仅确保当前进程原子删除一次
     *
     * @param redisKey
     * @return true if operation success
     */
    public boolean cleanCondition(RedisKey redisKey) {
        AtomicBoolean condition = redisKeyConditionStates.get(redisKey);
        if (null == condition)
            return false;
        if (condition.compareAndSet(true, false)) {
            redisKeyConditionStates.remove(redisKey);
            return stringValueTemplate().delete(redisKey);
        }
        return false;
    }

    /**
     * 自增(减)指定大小并检查当前值是否满足既定条件，条件一旦满足后续请求将直接返回true<br>
     * NOTE： 该方法仅用于condition判定及尽可能减少当前进程并发redis io请求（条件一旦满足，pending请求直接返回true）
     * Counter在条件满足时其值也是不确定的
     * <pre>
     * 场景举例：
     *      秒杀库存检查 create(reset)Condition("inventory:remain",99999);
     *                 counterCondition("inventory:remain",1,current -> current<1,false);
     *      说明：此实现方式将库存检查和订单创建分开处理，可能导致以下问题：
     *           库存先减少了，但订单因为某种原因创建失败了（且该失败无法通过重试解决，如账户资金不足），
     *           则需将库存进行回补(+1操作)，且Condition可能已被更新为true，后续请求不会被处理（需重置resetCondition).
     *           也可考虑使用消息队列，依靠消息队列FIFO特性先对请求排队，
     *           并在处理消息时创建订单，如果创建订单失败则继续处理下一个请求（或不ack将消息丢到队列末尾后续在处理），虽然处理速度比当前实现慢，一方面确保业务减库存后发生于创建订单，
     *           另一方囊程序逻辑较为简单，基于此秒杀类场景虽然用Redis性能较高，但存在失败回补的麻烦
     *
     * </pre>
     *
     * @param counterKey         操作的key
     * @param incr               true if increment or decrement
     * @param delta              增量
     * @param conditionEvaluator 条件评估器，满足则返回true，后续请求将不再进行计算直接返回true
     * @return
     */
    public boolean evaluateCounter(RedisKey counterKey, long delta, Function<Long, Boolean> conditionEvaluator, boolean incr) {
        AtomicBoolean condition = getConditionState(counterKey);
        if (condition.get())
            return true;
        boolean evalResult = conditionEvaluator.apply(incr ?
                stringValueOperations().increment(counterKey, delta) :
                stringValueOperations().decrement(counterKey, delta));
        if (evalResult && condition.compareAndSet(false, true)) {
            return true;
        }
        return evalResult;
    }

    /**
     * top N
     *
     * @param key
     * @param value
     * @param count
     */
    public void lpushAndTrim(RedisKey key, Object value, int keepSize) {
        if (value instanceof String) {
            stringListOperations().leftPush(key, value.toString());
            stringListOperations().trim(key, 0, keepSize - 1);
        } else {
            objectListOperations().leftPush(key, value);
            objectListOperations().trim(key, 0, keepSize - 1);
        }
    }


    /**
     * <pre>
     * 获取一批自增序列，数组仅返回两个边界值(incrByBefore，incrByAfter)
     * 如批量获取索引并使用，减少每次获取索引的开销 incrAndGetBy("user_sequence",100)
     * </pre>
     *
     * @param counterKey
     * @param incrBy
     * @return [0]=incrByBefore,[1]=incrByAfter
     */
    public long[] incrAndGetBy(RedisKey counterKey, long incrBy) {
        long rangeLast = stringValueOperations().increment(counterKey, incrBy);
        return new long[]{rangeLast - incrBy, rangeLast};
    }


    protected AtomicBoolean getConditionState(RedisKey redisKey) {
        return redisKeyConditionStates.computeIfAbsent(redisKey, key -> new AtomicBoolean(false));
    }
}
