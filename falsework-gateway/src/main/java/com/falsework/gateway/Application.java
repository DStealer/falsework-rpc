package com.falsework.gateway;

import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            Props props = PropsManager.initConfig("bootstrap.properties");
        } else {
            Props props = PropsManager.initConfig(args[0]);
        }
    }
}
