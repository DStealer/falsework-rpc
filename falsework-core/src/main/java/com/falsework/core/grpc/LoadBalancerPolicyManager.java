package com.falsework.core.grpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负载均衡器
 */
public enum LoadBalancerPolicyManager {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerPolicyManager.class);
    private static String  POLICY;

    public static void set(String policy) {
        if (POLICY == null) {
            POLICY = policy;
        } else {
            LOGGER.warn("policy in using...");
        }
    }

    public static String get() {
        return POLICY;
    }
}
