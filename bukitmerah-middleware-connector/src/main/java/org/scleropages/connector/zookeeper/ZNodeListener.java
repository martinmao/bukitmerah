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
 * 监控当前(默认)及子节点变化
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class ZNodeListener {


    public static final int ZNODE_EVENT_ADDED = 1;

    public static final int ZNODE_EVENT_REMOVED = 2;

    public static final int ZNODE_EVENT_UPDATED = 3;


    protected String eventDesc(int event){
        return 1==event?"ZNODE_EVENT_ADDED":2==event?"ZNODE_EVENT_REMOVED":3==event?"ZNODE_EVENT_UPDATED":"ZNODE_EVENT_UNKNOWN";
    }


    public abstract String watchedPath();

    /**
     * 最大深度,默认0，仅观察当前节点
     *
     * @return
     */
    public int depth() {
        return 0;
    }

    public void changed(ZookeeperClient client, int event, ZNode node) {
        switch (event) {
            case ZNODE_EVENT_ADDED:
                added(client, event, node);
                break;
            case ZNODE_EVENT_REMOVED:
                removed(client, event, node);
                break;
            case ZNODE_EVENT_UPDATED:
                updated(client, event, node);
            default:
                throw new IllegalStateException("unknown event: " + event);
        }
    }

    protected void added(ZookeeperClient client, int event, ZNode node) {
    }

    protected void removed(ZookeeperClient client, int event, ZNode node) {
    }

    protected void updated(ZookeeperClient client, int event, ZNode node) {
    }

}
