package com.falsework.governance.config;

/**
 * 配置文件
 */
public interface PropsVars extends com.falsework.core.config.PropsVars {
    /**
     * 注册中心地址 http://127.0.0.1:8001;127.0.0.1:8002;127.0.0.1:8003
     */
    String REGISTER_PEER_ADDRESS = "register.peer.addresses";
    /**
     * 自我保护模式
     */
    String REGISTER_SELF_PRESERVATION_MODE_ENABLED = "register.self.preservation.mode.enabled";
    /**
     * 多长时间清除失效节点
     */
    String REGISTER_EVICTION_INTERVAL = "register.eviction.interval";

    /**
     * 组白名单
     */
    String REGISTER_GROUP_WHITE_LIST = "register.group.white.list";
}
