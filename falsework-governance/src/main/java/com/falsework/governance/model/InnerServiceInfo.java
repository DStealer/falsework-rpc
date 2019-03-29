package com.falsework.governance.model;

import com.falsework.core.generated.governance.ServiceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;
import com.falsework.governance.generated.RegistryServiceInfo;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public class InnerServiceInfo implements Comparable<InnerServiceInfo> {
    private final String serviceName;
    private final String groupName;
    private final ConcurrentHashMap<String, InstanceLeaseInfo> LeaseMap;
    private volatile long lastDirtyTimestamp;

    public InnerServiceInfo(String groupName, String serviceName) {
        this.groupName = groupName;
        this.serviceName = serviceName;
        this.LeaseMap = new ConcurrentHashMap<>();
        this.lastDirtyTimestamp = -1L;
    }

    public InnerServiceInfo(RegistryServiceInfo serviceInfo) {
        this.serviceName = serviceInfo.getServiceName();
        this.groupName = serviceInfo.getGroupName();
        this.LeaseMap = new ConcurrentHashMap<>();
        for (RegistryLeaseInfo info : serviceInfo.getLeasesList()) {
            this.LeaseMap.put(info.getInstance().getInstanceId(), new InstanceLeaseInfo(info));
        }
        this.lastDirtyTimestamp = serviceInfo.getLastDirtyTimestamp();
    }

    public String getGroupName() {
        return groupName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public long getLastDirtyTimestamp() {
        return lastDirtyTimestamp;
    }

    public ServiceInfo snapshot() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setGroupName(this.groupName)
                .setServiceName(this.serviceName)
                .setHash(this.getHash());
        this.LeaseMap.values().forEach(info -> builder.addInstances(info.snapshot()));
        return builder.build();
    }

    public String getHash() {
        return String.format("%d", this.lastDirtyTimestamp);
    }

    public RegistryServiceInfo replicaSnapshot() {
        RegistryServiceInfo.Builder builder = RegistryServiceInfo.newBuilder()
                .setServiceName(this.serviceName)
                .setGroupName(this.groupName)
                .setLastDirtyTimestamp(this.lastDirtyTimestamp);
        this.getLeaseMap().values().forEach(e -> builder.addLeases(e.replicaSnapshot()));
        return builder.build();
    }

    public ConcurrentHashMap<String, InstanceLeaseInfo> getLeaseMap() {
        return LeaseMap;
    }

    public InnerServiceInfo markDirty() {
        this.lastDirtyTimestamp = System.currentTimeMillis();
        return this;
    }

    @Override
    public int compareTo(@Nonnull InnerServiceInfo o) {
        int result = this.groupName.compareTo(o.groupName);
        if (result != 0) {
            return result;
        } else {
            return this.serviceName.compareTo(o.serviceName);
        }
    }
}
