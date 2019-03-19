package com.falsework.core.server;

/**
 * 如何注册服务
 */
public interface ServerRegister {
    void register() throws Exception;

    void unregister() throws Exception;
}
