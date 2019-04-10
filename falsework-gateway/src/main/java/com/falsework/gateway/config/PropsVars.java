package com.falsework.gateway.config;

public interface PropsVars {
    /**
     * 服务治理的地址 http://127.0.0.1:2377;127.0.0.1:2378
     */
    String DISCOVERY_ADDRESS = "discovery.addresses";
    /**
     * 代理地址
     */
    String PROXY_IP = "proxy.ip";
    /**
     * 代理端口
     */
    String PROXY_PORT = "proxy.port";
    /**
     * 路由前缀
     */
    String GATEWAY_ROUTES_PREFIX = "gateway.routes";
}
