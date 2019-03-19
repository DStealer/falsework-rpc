package com.falsework.core.client;

import java.util.Set;

public interface ChannelLifecycle {
    /**
     * 添加监听器
     *
     * @param listener
     */
    void addLifecycleListener(ChannelLifecycleListener listener);

    /**
     * 查找监听器
     *
     * @return
     */
    Set<ChannelLifecycleListener> findLifecycleListeners();

    /**
     * 移除监听器
     *
     * @param listener
     */
    void removeLifecycleListener(ChannelLifecycleListener listener);

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
