package com.falsework.core.server;

import java.util.EventListener;

/**
 * 服务器生命周期监听者
 */
public interface ServerListener extends EventListener {

    /**
     * 启动之前
     *
     * @throws Exception
     */
    default void beforeStart() throws Exception {

    }

    /**
     * 启动之后
     *
     * @throws Exception
     */
    default void afterStart() throws Exception {

    }

    /**
     * 停止前
     *
     * @throws Exception
     */
    default void beforeStop() throws Exception {

    }

    /**
     * 停止后
     *
     * @throws Exception
     */
    default void afterStop() throws Exception {

    }
}
