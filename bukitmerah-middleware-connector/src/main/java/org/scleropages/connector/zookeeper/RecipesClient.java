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

import java.util.Collection;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface RecipesClient {

    ReentrantLock reentrantLock(String path);

    ReentrantReadWriteLock reentrantReadWriteLock(String path);

    Semaphore semaphore(String path, int permits);

    void startLeaderSelector(String path, boolean autoRequeue);

    void startLead(String path);

    void stopLead(String path);

    void stopLeaderSelector(String path);

    Collection<LeaderParticipant> leaderParticipants(String path) throws Exception;

    LeaderParticipant getLeader(String path) throws Exception;

    LeaderParticipant getCurrentParticipant(String path);

    JoinLeaveBarrier barrier(String path, int parties);

    Latch latch(String path);

    LatchOwner latchOwner(String path);
}
