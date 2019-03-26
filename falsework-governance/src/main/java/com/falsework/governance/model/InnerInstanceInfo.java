package com.falsework.governance.model;

import com.falsework.core.generated.governance.InstanceInfo;
import com.falsework.core.generated.governance.InstanceStatus;

public class InnerInstanceInfo implements Comparable {
    private final String instanceId;
    private final String serviceName;
    private final String groupName;
    private final String ipAddress;
    private final int port;
    private volatile InstanceStatus status;
    private volatile long lastUpdateTimestamp;
    private volatile String hash;

    public InnerInstanceInfo(InstanceInfo instanceInfo) {
        this.instanceId = instanceInfo.getInstanceId();
        this.serviceName = instanceInfo.getServiceName();
        this.groupName = instanceInfo.getGroupName();
        this.ipAddress = instanceInfo.getIpAddress();
        this.port = instanceInfo.getPort();
        this.status = instanceInfo.getStatus();
        this.hash = instanceInfo.getHash();
        this.lastUpdateTimestamp = System.currentTimeMillis();

    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getGroupName() {
        return groupName;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public String reHash() {
        this.hash = String.format("%s|%d", this.instanceId, this.lastUpdateTimestamp);
        return this.hash;
    }

    public String getHash() {
        return hash;
    }

    public InstanceInfo snapshot() {
        return InstanceInfo.newBuilder()
                .setInstanceId(this.instanceId)
                .setServiceName(this.serviceName)
                .setGroupName(this.groupName)
                .setIpAddress(this.ipAddress)
                .setPort(this.port)
                .setStatus(this.status)
                .setHash(this.hash)
                .build();
    }

    @Override
    public int compareTo(Object o) {
        return -1;
    }
}
