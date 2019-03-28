package com.falsework.core.client;

import java.util.Set;

public interface ChannelLifecycle {
    /**
     * 添加监听器
     *
     * @param listener
     */
    void addListener(ChannelListener listener);

    /**
     * 查找监听器
     *
     * @return
     */
    Set<ChannelListener> findListeners();

    /**
     * 移除监听器
     *
     * @param listener
     */
    void removeListener(ChannelListener listener);

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
