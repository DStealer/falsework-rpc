package com.falsework.governance.model;

import com.falsework.core.generated.governance.InstanceInfo;
import com.falsework.core.generated.governance.InstanceStatus;
import com.falsework.governance.generated.RegistryInstanceInfo;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InnerInstanceInfo implements Comparable<InnerInstanceInfo> {
    private final String instanceId;
    private final String serviceName;
    private final String groupName;
    private final String ipAddress;
    private final int port;
    private final Map<String, String> attributes;
    private volatile InstanceStatus status;
    private volatile long lastDirtyTimestamp;

    public InnerInstanceInfo(InstanceInfo instanceInfo) {
        this.instanceId = instanceInfo.getInstanceId();
        this.serviceName = instanceInfo.getServiceName();
        this.groupName = instanceInfo.getGroupName();
        this.ipAddress = instanceInfo.getIpAddress();
        this.port = instanceInfo.getPort();
        this.status = instanceInfo.getStatus();
        this.attributes = new ConcurrentHashMap<>(instanceInfo.getAttributesMap());
        //hash值由 lastDirtyTimestamp 维护
        this.lastDirtyTimestamp = System.currentTimeMillis();
    }

    public InnerInstanceInfo(RegistryInstanceInfo instanceInfo) {
        this.instanceId = instanceInfo.getInstanceId();
        this.serviceName = instanceInfo.getServiceName();
        this.groupName = instanceInfo.getGroupName();
        this.ipAddress = instanceInfo.getIpAddress();
        this.port = instanceInfo.getPort();
        this.status = instanceInfo.getStatus();
        this.attributes = new ConcurrentHashMap<>(instanceInfo.getAttributesMap());
        this.lastDirtyTimestamp = instanceInfo.getLastDirtyTimestamp();
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

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public InnerInstanceInfo markDirty() {
        this.lastDirtyTimestamp = System.currentTimeMillis();
        return this;
    }

    public long getLastDirtyTimestamp() {
        return lastDirtyTimestamp;
    }

    public InstanceInfo snapshot() {
        return InstanceInfo.newBuilder()
                .setInstanceId(this.instanceId)
                .setServiceName(this.serviceName)
                .setGroupName(this.groupName)
                .setIpAddress(this.ipAddress)
                .setPort(this.port)
                .setStatus(this.status)
                .putAllAttributes(this.attributes)
                .setHash(this.getHash())
                .build();
    }

    public String getHash() {
        return String.format("%d", this.lastDirtyTimestamp);
    }

    public RegistryInstanceInfo replicaSnapshot() {
        return RegistryInstanceInfo.newBuilder()
                .setInstanceId(this.instanceId)
                .setServiceName(this.serviceName)
                .setGroupName(this.groupName)
                .setIpAddress(this.ipAddress)
                .setPort(this.port)
                .setStatus(this.status)
                .putAllAttributes(this.attributes)
                .setLastDirtyTimestamp(this.lastDirtyTimestamp)
                .build();
    }

    @Override
    public int compareTo(@Nonnull InnerInstanceInfo o) {
        int result = this.groupName.compareTo(o.groupName);
        if (result != 0) {
            return result;
        }
        result = this.serviceName.compareTo(o.serviceName);
        if (result != 0) {
            return result;
        }
        return this.instanceId.compareTo(o.instanceId);
    }
}
