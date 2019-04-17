package com.falsework.service.core;

import com.google.inject.AbstractModule;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
       bind(EchoService.class).toProvider(EchoService::new);
    }
}
