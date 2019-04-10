package com.falsework.gateway.resolver;

import com.falsework.gateway.module.ServiceInfo;

import java.util.Optional;

public interface PathResolver {
    Optional<ServiceInfo> resolve(String path);
}
