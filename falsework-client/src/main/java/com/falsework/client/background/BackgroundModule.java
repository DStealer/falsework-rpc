package com.falsework.client.background;

import com.google.inject.AbstractModule;

public class BackgroundModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EchoCaller.class).asEagerSingleton();
    }
}
