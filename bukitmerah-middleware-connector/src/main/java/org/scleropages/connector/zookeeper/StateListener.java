package org.scleropages.connector.zookeeper;


public interface StateListener {

    int DISCONNECTED = 0;

    int CONNECTED = 1;

    int RECONNECTED = 2;

    int START_LEAD = 4;

    int STOP_LEAD = 5;

    int JOIN_LEADER_GROUP = 6;

    int LEAVE_LEADER_GROUP = 7;

    default void stateChanged(ZookeeperClient client, int state) {
        if (state == DISCONNECTED)
            disconnected(client);
        else if (state == CONNECTED)
            connected(client);
        else if (state == RECONNECTED)
            reconnected(client);
        else if (state == START_LEAD)
            startLead(client);
        else if (state == STOP_LEAD)
            stopLead(client);
        else if (state == JOIN_LEADER_GROUP)
            joinLeaderGroup(client);
        else if (state == LEAVE_LEADER_GROUP)
            leaveLeaderGroup(client);
        else
            throw new IllegalStateException("unknown state: " + state);
    }

    default void disconnected(ZookeeperClient client) {
    }

    default void connected(ZookeeperClient client) {
    }

    default void reconnected(ZookeeperClient client) {
    }

    default void startLead(ZookeeperClient client) {
    }

    default void stopLead(ZookeeperClient client) {
    }

    default void joinLeaderGroup(ZookeeperClient client) {

    }

    default void leaveLeaderGroup(ZookeeperClient client) {

    }


}
