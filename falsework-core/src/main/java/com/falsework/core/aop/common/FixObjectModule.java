package com.falsework.core.aop.common;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixObjectModule<T> extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixObjectModule.class);
    private final Class<T> clazz;
    private final T instance;

    public FixObjectModule(Class<T> clazz, T instance) {
        this.clazz = clazz;
        this.instance = instance;
    }

    @Override
    protected void configure() {
        LOGGER.info("Export key:{} <=> {} to context", clazz, instance);
        bind(clazz).toInstance(instance);
    }
}
