package com.falsework.governance.model;

import com.falsework.core.generated.governance.ServiceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;
import com.falsework.governance.generated.RegistryServiceInfo;
import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InnerServiceInfo implements Comparable<InnerServiceInfo> {
    private final String serviceName;
    private final String groupName;
    private final ConcurrentHashMap<String, LeaseInfo> instanceMap;
    private volatile String hash;

    public InnerServiceInfo(String groupName, String serviceName) {
        this.groupName = groupName;
        this.serviceName = serviceName;
        this.instanceMap = new ConcurrentHashMap<>();
        this.hash = "";
    }

    public InnerServiceInfo(RegistryServiceInfo serviceInfo) {
        this.serviceName = serviceInfo.getServiceName();
        this.groupName = serviceInfo.getGroupName();
        this.instanceMap = new ConcurrentHashMap<>();

        for (Map.Entry<String, RegistryLeaseInfo> entry : serviceInfo.getLeaseMapMap().entrySet()) {
            this.instanceMap.put(entry.getKey(), new LeaseInfo(entry.getValue()));
        }
        this.hash = serviceInfo.getHash();
    }

    public String getHash() {
        return hash;
    }

    public ConcurrentHashMap<String, LeaseInfo> getInstanceMap() {
        return instanceMap;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @SuppressWarnings("all")
    public String reHash() {
        this.hash = Hashing.goodFastHash(32)
                .hashUnencodedChars(this.instanceMap.values()
                        .stream()
                        .map(LeaseInfo::getHolder)
                        .sorted()
                        .map(InnerInstanceInfo::reHash)
                        .collect(Collectors.joining()))
                .toString();
        return this.hash;
    }


    public ServiceInfo snapshot() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setGroupName(this.groupName)
                .setServiceName(this.serviceName)
                .setHash(this.reHash());
        for (Map.Entry<String, LeaseInfo> entry : this.instanceMap.entrySet()) {
            builder.putInstanceMap(entry.getKey(), entry.getValue().getHolder().snapshot());
        }
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
