package com.falsework.core.config;

public interface PropsVars {
    /**
     * 服务名称
     */
    String SERVER_NAME = "server.name";
    /**
     * 服务地址
     */
    String SERVER_IP = "server.ip";
    /**
     * 服务端口
     */
    String SERVER_PORT = "server.port";

    /**
     * 服务属组
     */
    String SERVER_GROUP = "server.group";

    /**
     * server 线程池
     */
    String SERVER_THREAD_POOL_SIZE = "server.thread.pool.size";

    /**
     * 服务治理配置前缀
     */
    String DISCOVERY_PREFIX = "discovery";

    /**
     * 服务治理的地址 http://127.0.0.1:2377;127.0.0.1:2378
     */
    String DISCOVERY_ADDRESS = "discovery.addresses";
    /**
     * 是否需要使用服务发现功能
     */
    String DISCOVERY_FETCH_REGISTRY = "discovery.fetch.registry";
    /**
     * 是否注册本地服务
     */
    String DISCOVERY_REGISTER_SELF = "discovery.register.self";

    /**
     * jdbc 配置前缀
     */
    String JDBC_PREFIX = "jdbc";

    /**
     * channel 线程池
     */
    String CHANNEL_THREAD_POOL_SIZE = "channel.thread.pool.size";
}
