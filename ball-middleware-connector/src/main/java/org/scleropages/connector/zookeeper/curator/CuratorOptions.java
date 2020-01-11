package org.scleropages.connector.zookeeper.curator;


public class CuratorOptions {

    /**
     * Connection string to the Zookeeper cluster.
     */
    private String connectString = "localhost:2181";

    /**
     * Initial amount of time to wait between retries.
     */
    private Integer baseSleepTimeMs = 50;

    /**
     * Max number of times to retry.
     */
    private Integer maxRetries = 29;

    /**
     * Max time in ms to sleep on each retry.
     */
    private Integer maxSleepMs = 500;

    /**
     * Wait time to block on connection to Zookeeper.
     */
    private Integer blockUntilConnectedWaitMs = 6000;


    private String namespace;

    private int sessionTimeoutMs = 2000;

    private int connectionTimeoutMs = 5000;

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConnectString() {
        return this.connectString;
    }


    public Integer getBaseSleepTimeMs() {
        return this.baseSleepTimeMs;
    }

    public Integer getMaxRetries() {
        return this.maxRetries;
    }

    public Integer getMaxSleepMs() {
        return this.maxSleepMs;
    }


    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }


    public void setBaseSleepTimeMs(Integer baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setMaxSleepMs(Integer maxSleepMs) {
        this.maxSleepMs = maxSleepMs;
    }

    public Integer getBlockUntilConnectedWaitMs() {
        return blockUntilConnectedWaitMs;
    }

    public void setBlockUntilConnectedWaitMs(Integer blockUntilConnectedWaitMs) {
        this.blockUntilConnectedWaitMs = blockUntilConnectedWaitMs;
    }
}
