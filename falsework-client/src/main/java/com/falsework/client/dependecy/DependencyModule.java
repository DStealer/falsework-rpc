package com.falsework.client.dependecy;

import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.service.generated.EchoServiceGrpc;
import com.google.inject.AbstractModule;

public class DependencyModule extends AbstractModule {
    @Override
    protected void configure() {
        ChannelManager manager = ChannelManagerBuilder.newBuilder()
                .name("dynamic://service-v1")
                .build();
        try {
            manager.start();
        } catch (Exception e) {
            addError(e);
        }
        EchoServiceGrpc.EchoServiceStub stub = manager.newStub(EchoServiceGrpc::newStub);
        bind(EchoServiceGrpc.EchoServiceStub.class).toInstance(stub);

    }
}
