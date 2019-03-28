package com.falsework.governance.model;

import com.falsework.core.generated.governance.GroupInfo;
import com.falsework.governance.generated.RegistryGroupInfo;
import com.falsework.governance.generated.RegistryServiceInfo;
import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InnerGroupInfo implements Comparable<InnerGroupInfo> {
    private final String groupName;
    private final ConcurrentHashMap<String, InnerServiceInfo> serviceMap;
    private volatile String hash;

    public InnerGroupInfo(String groupName) {
        this.groupName = groupName;
        this.serviceMap = new ConcurrentHashMap<>();
        this.hash = "";
    }

    public InnerGroupInfo(RegistryGroupInfo groupInfo) {
        this.groupName = groupInfo.getGroupName();
        this.serviceMap = new ConcurrentHashMap<>();
        for (RegistryServiceInfo info : groupInfo.getServicesList()) {
            this.serviceMap.put(info.getServiceName(), new InnerServiceInfo(info));
        }
        this.hash = groupInfo.getHash();
    }

    public String getGroupName() {
        return groupName;
    }

    public ConcurrentHashMap<String, InnerServiceInfo> getServiceMap() {
        return serviceMap;
    }

    public String getHash() {
        return hash;
    }

    public GroupInfo snapshot() {
        GroupInfo.Builder builder = GroupInfo.newBuilder()
                .setGroupName(this.groupName)
                .setHash(this.reHash());
        this.serviceMap.values().forEach(info -> builder.addServices(info.snapshot()));
        return builder.build();
    }

    @SuppressWarnings("all")
    public String reHash() {
        this.hash = Hashing.goodFastHash(128)
                .hashUnencodedChars(this.serviceMap.values().stream()
                        .sorted()
                        .map(InnerServiceInfo::reHash)
                        .collect(Collectors.joining()))
                .toString();
        return this.hash;
    }

    public RegistryGroupInfo replicaSnapshot() {
        RegistryGroupInfo.Builder builder = RegistryGroupInfo.newBuilder()
                .setGroupName(this.groupName)
                .setHash(this.reHash());
        this.serviceMap.values().forEach(info -> builder.addServices(info.replicaSnapshot()));
        return builder.build();
    }


    @Override
    public int compareTo(@Nonnull InnerGroupInfo o) {
        return this.groupName.compareTo(o.groupName);
    }
}
