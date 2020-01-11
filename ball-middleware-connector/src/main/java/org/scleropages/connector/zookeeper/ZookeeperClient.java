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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public interface ZookeeperClient {

    void create(String path, CreateMode createMode, byte[] data) throws Exception;

    void delete(String path, int version) throws Exception;

    Stat get(String path) throws Exception;

    void set(String path, int version, byte[] data) throws Exception;

    byte[] getData(String path) throws Exception;

    boolean exists(String path) throws Exception;

    List<String> getChildren(String path) throws Exception;

    void addZNodeListener(String path, ZNodeListener listener) throws Exception;

    void removeZNodeListener(String path, ZNodeListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    <T> T getNativeClient();

    boolean isConnected();

    boolean isStarted();

    String[] getWatchedPaths();

    ZNodeListener[] getZNodeListeners(String path);

    Set<StateListener> getStateListeners();
}
