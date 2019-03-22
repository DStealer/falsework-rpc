package com.falsework.core.composite;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class FixInstanceModule<T> extends AbstractModule {
    private final String name;
    private final Class<T> clazz;
    private final T instance;

    public FixInstanceModule(String name, Class<T> clazz, T instance) {
        this.name = name;
        this.clazz = clazz;
        this.instance = instance;
    }

    public FixInstanceModule(Class<T> clazz, T instance) {
        this(null, clazz, instance);
    }

    @Override
    protected void configure() {
        if (name != null) {
            bind(clazz).annotatedWith(Names.named(name)).toInstance(instance);
        } else {
            bind(clazz).toInstance(instance);
        }
    }
}
