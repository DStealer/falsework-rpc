package com.falsework.governance.service;

import com.falsework.core.aop.common.EnvAwareModule;

public class ServiceModule extends EnvAwareModule {
    @Override
    protected void configure() {
        bind(LookupService.class).asEagerSingleton();
    }
}
