package com.falsework.core.server;

import java.util.Set;

/**
 * 服务端的生命周期
 */
public interface ServerLifecycle {
    /**
     * 添加监听器
     *
     * @param listener
     */
    void addListener(ServerListener listener);

    /**
     * 查找监听器
     *
     * @return
     */
    Set<ServerListener> findListeners();

    /**
     * 移除监听器
     *
     * @param listener
     */
    void removeListener(ServerListener listener);

    /**
     * 启动
     *
     * @throws Exception
     */
    void start() throws Exception;

    /**
     * 停止
     *
     * @throws Exception
     */
    void stop() throws Exception;
}
