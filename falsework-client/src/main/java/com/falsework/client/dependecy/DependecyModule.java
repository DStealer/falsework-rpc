package com.falsework.client.dependecy;

import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.service.generated.EchoServiceGrpc;
import com.google.inject.AbstractModule;

public class DependecyModule extends AbstractModule {
    @Override
    protected void configure() {
        ChannelManager manager = ChannelManagerBuilder.newBuilder()
                .name("http://127.0.0.1:8081")
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
