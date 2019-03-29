package com.falsework.core.grpc;

public enum ChannelConfigurerManager {
;
private static final ChannelConfigurer configurer = new ChannelConfigurer();

    public static ChannelConfigurer getConfigurer() {
        return configurer;
    }
}
