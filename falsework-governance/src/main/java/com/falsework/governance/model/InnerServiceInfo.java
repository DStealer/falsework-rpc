package com.falsework.governance.model;

import com.falsework.core.generated.governance.ServiceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;
import com.falsework.governance.generated.RegistryServiceInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InnerServiceInfo {
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

    public String reHash() {
        this.hash = this.instanceMap.values()
                .stream()
                .map(LeaseInfo::getHolder)
                .sorted()
                .map(InnerInstanceInfo::getHash)
                .reduce("", String::concat);
        return this.hash;
    }

    public ServiceInfo snapshot() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setGroupName(this.groupName)
                .setServiceName(this.serviceName)
                .setHash(this.hash);
        for (Map.Entry<String, LeaseInfo> entry : this.instanceMap.entrySet()) {
            builder.putInstanceMap(entry.getKey(), entry.getValue().getHolder().snapshot());
        }
        return builder.build();
    }
}
