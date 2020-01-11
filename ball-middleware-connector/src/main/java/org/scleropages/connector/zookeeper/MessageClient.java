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
package org.scleropages.connector.zookeeper;

/**
 * <pre>
 * 实现特定path下的消息交互集群，为分布式应用集群节点提供信息交换能力，需动态加入并感知.
 *
 * 设计思路：
 * 所有参与者需创建CONTAINER节点到该path下，并监控该path下所有子（孙）节点变化,在CONTAINER节点下先创建临时节点维持会话及在线动态感知.
 * 所有参与者需将消息发布到孙节点：
 * 消息节点为TTL节点，根据需要设置存活时间
 * -cluster
 *      -uuid(CONTAINER)
 *              -127.0.0.1(EPHEMERAL)
 *              -message00000000000(PERSISTENT_WITH_TTL/PERSISTENT_SEQUENTIAL_WITH_TTL)
 *              -message00000000001(PERSISTENT_WITH_TTL/PERSISTENT_SEQUENTIAL_WITH_TTL)
 *              ....
 *      -uuid(CONTAINER)
 *              -127.0.0.2(EPHEMERAL)
 *              -message00000000000(PERSISTENT_WITH_TTL/PERSISTENT_SEQUENTIAL_WITH_TTL)
 *              -message00000000001(PERSISTENT_WITH_TTL/PERSISTENT_SEQUENTIAL_WITH_TTL)
 *              ....
 * </pre>
 *
 * <pre>
 * 消息协议：序列化方案暂定
 * 消息头：
 *      QOS（级别）：
 *              PUB_ONLY：消息发布完什么都不做，不管消息是否能达到
 *              PUB_ACK：接收到消息的节点都必须要发送回执到REPLY_PATH，发布端决定后续工作（消息清除应有发布端自行进行，且设置一个较大的TTL时间）
 *      消息ID：消息唯一标识（主要用于关联发送消息与回执消息）
 *      关联ID：回执消息需要设置，便于发布端进行关联
 *      REPLY_PATH：发送端指定消息回执发送到指定PATH，考虑直接放在CONTAINER节点下
 *
 * </pre>
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface MessageClient {

}
