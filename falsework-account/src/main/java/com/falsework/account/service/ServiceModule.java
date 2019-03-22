package com.falsework.account.service;


import com.google.inject.AbstractModule;

public class ServiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LoginService.class).asEagerSingleton();
        bind(SimpleService.class).asEagerSingleton();
    }
}
