package com.example.mngsys.redisevent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.event")
/**
 * RedisEventNotifyProperties。
 */
public class RedisEventNotifyProperties {
    private String streamKey = "portal:events";
    private String dedupKeyPrefix = "event:dedup:";
    private long dedupTtlSeconds = 7 * 24 * 60 * 60;
    private String groupName = "portal-group";
    private String consumerName = "portal-consumer";
    private int batchSize = 10;
    private long blockMillis = 2000;
    private String systemCode = "portal";

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getDedupKeyPrefix() {
        return dedupKeyPrefix;
    }

    public void setDedupKeyPrefix(String dedupKeyPrefix) {
        this.dedupKeyPrefix = dedupKeyPrefix;
    }

    public long getDedupTtlSeconds() {
        return dedupTtlSeconds;
    }

    public void setDedupTtlSeconds(long dedupTtlSeconds) {
        this.dedupTtlSeconds = dedupTtlSeconds;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBlockMillis() {
        return blockMillis;
    }

    public void setBlockMillis(long blockMillis) {
        this.blockMillis = blockMillis;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }
}
