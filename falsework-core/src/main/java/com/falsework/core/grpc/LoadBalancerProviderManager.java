package com.falsework.core.grpc;

import io.grpc.LoadBalancerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负载均衡器
 */
public enum LoadBalancerProviderManager {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerProviderManager.class);
    private static LoadBalancerProvider PROVIDER;

    public static void set(LoadBalancerProvider provider) {
        if (PROVIDER == null) {
            PROVIDER = provider;
        } else {
            LOGGER.warn("provider in using...");
        }
    }

    public static LoadBalancerProvider get() {
        return PROVIDER;
    }
}
