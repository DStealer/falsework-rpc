package com.falsework.gateway;

import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;
import com.falsework.gateway.server.H2cProxyServer;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        Props props = PropsManager.initConfig("bootstrap.properties");
        new H2cProxyServer().runSync(props);
    }
}
