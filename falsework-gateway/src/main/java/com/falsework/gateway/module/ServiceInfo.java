package com.falsework.gateway.module;

public class ServiceInfo {
    private final String group;
    private final String service;

    public ServiceInfo(String group, String service) {
        this.group = group;
        this.service = service;
    }

    public String getGroup() {
        return group;
    }

    public String getService() {
        return service;
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "group='" + group + '\'' +
                ", service='" + service + '\'' +
                '}';
    }
}
