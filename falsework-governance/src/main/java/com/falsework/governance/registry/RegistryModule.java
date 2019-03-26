package com.falsework.governance.registry;

import com.google.inject.AbstractModule;

public class RegistryModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(InstanceRegistry.class).asEagerSingleton();
        bind(RegistryLifeListener.class).asEagerSingleton();
    }
}
