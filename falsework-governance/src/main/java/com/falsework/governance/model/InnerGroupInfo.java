package com.falsework.governance.model;

import com.falsework.core.generated.governance.GroupInfo;
import com.falsework.governance.generated.RegistryGroupInfo;
import com.falsework.governance.generated.RegistryServiceInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InnerGroupInfo {
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
        for (Map.Entry<String, RegistryServiceInfo> infoEntry : groupInfo.getServiceMapMap().entrySet()) {
            this.serviceMap.put(infoEntry.getKey(), new InnerServiceInfo(infoEntry.getValue()));
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

    public String reHash() {
        this.hash = "";
        return this.hash;
    }

    public GroupInfo snapshot() {
        GroupInfo.Builder builder = GroupInfo.newBuilder()
                .setGroupName(this.groupName).setHash(this.hash);
        for (Map.Entry<String, InnerServiceInfo> infoEntry : this.serviceMap.entrySet()) {
            builder.putServiceMap(infoEntry.getKey(), infoEntry.getValue().snapshot());
        }
        return builder.build();
    }
}
