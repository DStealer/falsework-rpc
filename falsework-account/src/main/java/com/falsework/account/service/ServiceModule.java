package com.falsework.account.service;

import com.falsework.core.aop.common.EnvAwareModule;

public class ServiceModule extends EnvAwareModule {
    @Override
    protected void configure() {
        bind(LoginService.class).asEagerSingleton();
    }
}
