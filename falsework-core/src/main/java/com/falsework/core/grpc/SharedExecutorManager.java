package com.falsework.core.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public enum SharedExecutorManager {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedExecutorManager.class);

    private static Executor single;

    /**
     * 获取共享线程池
     *
     * @return
     */
    public static Executor getShared() {
        return single;
    }

    /**
     * 设置client共享线程池
     *
     * @param executor
     */
    public static void setShared(Executor executor) {
        if (single == null) {
            single = executor;
        } else {
            LOGGER.warn("executor in using...");
        }
    }
}
