package com.falsework.core.common;

import io.netty.util.NettyRuntime;

import java.util.concurrent.*;

public enum MoreExecuters {
    ;

    /**
     * 新建线程池防止过多空闲的线程
     *
     * @param max
     * @param factory
     * @return
     */
    public static Executor newStretchThreadPool(int max, ThreadFactory factory) {
        int min = NettyRuntime.availableProcessors();
        if (max < min) {
            min = max;
        }
        return new ThreadPoolExecutor(min, max, 300L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(8192), factory, new ThreadPoolExecutor.AbortPolicy());
    }
}
