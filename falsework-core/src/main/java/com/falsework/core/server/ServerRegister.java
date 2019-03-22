package com.falsework.core.server;

/**
 * 如何注册服务
 */
public interface ServerRegister {
    ServerRegister NO_OP = new ServerRegister() {
        @Override
        public void register() throws Exception {

        }

        @Override
        public void unregister() throws Exception {

        }
    };

    void register() throws Exception;

    void unregister() throws Exception;
}
