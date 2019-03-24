package com.falsework.governance.service;

import com.google.inject.AbstractModule;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DiscoveryService.class).asEagerSingleton();
    }
}
