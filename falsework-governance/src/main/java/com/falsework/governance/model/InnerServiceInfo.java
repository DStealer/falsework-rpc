package com.falsework.governance.model;

import com.falsework.core.generated.governance.ServiceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;
import com.falsework.governance.generated.RegistryServiceInfo;
import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InnerServiceInfo implements Comparable<InnerServiceInfo> {
    private final String serviceName;
    private final String groupName;
    private final ConcurrentHashMap<String, InstanceLeaseInfo> LeaseMap;
    private volatile String hash;

    public InnerServiceInfo(String groupName, String serviceName) {
        this.groupName = groupName;
        this.serviceName = serviceName;
        this.LeaseMap = new ConcurrentHashMap<>();
        this.hash = "";
    }

    public InnerServiceInfo(RegistryServiceInfo serviceInfo) {
        this.serviceName = serviceInfo.getServiceName();
        this.groupName = serviceInfo.getGroupName();
        this.LeaseMap = new ConcurrentHashMap<>();
        for (RegistryLeaseInfo info : serviceInfo.getLeasesList()) {
            this.LeaseMap.put(info.getInstance().getInstanceId(), new InstanceLeaseInfo(info));
        }
        this.hash = serviceInfo.getHash();
    }

    public String getHash() {
        return hash;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public ConcurrentHashMap<String, InstanceLeaseInfo> getLeaseMap() {
        return LeaseMap;
    }

    public ServiceInfo snapshot() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setGroupName(this.groupName)
                .setServiceName(this.serviceName)
                .setHash(this.reHash());
        this.LeaseMap.values().forEach(info -> builder.addInstances(info.snapshot()));
        return builder.build();
    }

    @SuppressWarnings("all")
    public String reHash() {
        this.hash = Hashing.goodFastHash(128)
                .hashUnencodedChars(this.LeaseMap.values()
                        .stream()
                        .sorted()
                        .map(InstanceLeaseInfo::reHash)
                        .collect(Collectors.joining()))
                .toString();
        return this.hash;
    }

    public RegistryServiceInfo replicaSnapshot() {
        RegistryServiceInfo.Builder builder = RegistryServiceInfo.newBuilder()
                .setServiceName(this.serviceName)
                .setGroupName(this.groupName)
                .setHash(this.reHash());
        this.getLeaseMap().values().forEach(e -> builder.addLeases(e.replicaSnapshot()));
        return builder.build();
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
