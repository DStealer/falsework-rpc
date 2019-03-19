package com.falsework.core.aop.common;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameObjectModule<T> extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(NameObjectModule.class);
    private final String name;
    private final Class<T> clazz;
    private final T instance;

    public NameObjectModule(String name, Class<T> clazz, T instance) {
        this.name = name;
        this.clazz = clazz;
        this.instance = instance;
    }

    @Override
    protected void configure() {
        LOGGER.info("Export key:{}:{} <=> {} to context", clazz, name, instance);
        bind(clazz).annotatedWith(Names.named(name)).toInstance(instance);
    }
}
