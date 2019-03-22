package com.falsework.core.grpc;

import io.grpc.NameResolver;

/**
 * 名称解析工厂
 */
public enum CompositeResolverFactoryManager {
    ;
    private static final CompositeResolverFactory FACTORY = new CompositeResolverFactory();

    /**
     * 添加工厂
     *
     * @param factory
     */
    public static void addFactory(NameResolver.Factory factory) {
        FACTORY.addFactory(factory);
    }

    /**
     * 移除工厂
     *
     * @param factory
     */
    public static void removeFactory(NameResolver.Factory factory) {
        FACTORY.removeFactory(factory);
    }


    /**
     * 获取工厂
     *
     * @return
     */
    public static NameResolver.Factory getFactory() {
        return FACTORY;
    }

}
