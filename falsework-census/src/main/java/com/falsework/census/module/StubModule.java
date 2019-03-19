package com.falsework.census.module;

import com.falsework.census.service.MetricQueryService;
import com.falsework.census.service.TracingQueryService;
import com.falsework.census.service.MetricCollectService;
import com.falsework.census.service.TracingCollectService;
import com.falsework.core.aop.common.EnvAwareModule;

public class StubModule extends EnvAwareModule {
    @Override
    protected void configure() {
        bind(TracingCollectService.class).asEagerSingleton();
        bind(MetricCollectService.class).asEagerSingleton();
        bind(TracingQueryService.class).asEagerSingleton();
        bind(MetricQueryService.class).asEagerSingleton();
    }
}
