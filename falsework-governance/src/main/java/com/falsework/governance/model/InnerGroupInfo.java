package com.falsework.governance.model;

import com.falsework.core.generated.governance.GroupInfo;
import com.falsework.governance.generated.RegistryGroupInfo;
import com.falsework.governance.generated.RegistryServiceInfo;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

public class InnerGroupInfo implements Comparable<InnerGroupInfo> {
    private final String groupName;
    private final ConcurrentHashMap<String, InnerServiceInfo> serviceMap;
    private volatile long lastDirtyTimestamp;


    public InnerGroupInfo(String groupName) {
        this.groupName = groupName;
        this.serviceMap = new ConcurrentHashMap<>();
        this.lastDirtyTimestamp = System.currentTimeMillis();
    }

    public InnerGroupInfo(RegistryGroupInfo groupInfo) {
        this.groupName = groupInfo.getGroupName();
        this.serviceMap = new ConcurrentHashMap<>();
        for (RegistryServiceInfo info : groupInfo.getServicesList()) {
            this.serviceMap.put(info.getServiceName(), new InnerServiceInfo(info));
        }
        this.lastDirtyTimestamp = groupInfo.getLastDirtyTimestamp();
    }

    public String getGroupName() {
        return groupName;
    }

    public ConcurrentHashMap<String, InnerServiceInfo> getServiceMap() {
        return serviceMap;
    }

    public GroupInfo snapshot() {
        GroupInfo.Builder builder = GroupInfo.newBuilder()
                .setGroupName(this.groupName)
                .setHash(this.getHash());
        this.serviceMap.values().forEach(info -> builder.addServices(info.snapshot()));
        return builder.build();
    }

    public String getHash() {
        return String.format("%d", this.lastDirtyTimestamp);
    }

    public RegistryGroupInfo replicaSnapshot() {
        RegistryGroupInfo.Builder builder = RegistryGroupInfo.newBuilder()
                .setGroupName(this.groupName)
                .setLastDirtyTimestamp(this.lastDirtyTimestamp);
        this.serviceMap.values().forEach(info -> builder.addServices(info.replicaSnapshot()));
        return builder.build();
    }

    public InnerGroupInfo markDirty() {
        this.lastDirtyTimestamp = System.currentTimeMillis();
        return this;
    }

    @Override
    public int compareTo(@Nonnull InnerGroupInfo o) {
        return this.groupName.compareTo(o.groupName);
    }
}
