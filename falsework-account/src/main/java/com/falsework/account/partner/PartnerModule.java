package com.falsework.account.partner;

import com.falsework.account.generated.LoginGrpc;
import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartnerModule extends EnvAwareModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerModule.class);


    @Override
    protected void configure() {
        try {
            ChannelManager manager = ChannelManagerBuilder.newBuilder()
                    .name("etcd://account-v1")
                    .build();
            manager.start();
            bind(Key.get(ChannelManager.class, Names.named("account-v1"))).toInstance(manager);

            LoginGrpc.LoginBlockingStub stub = manager.newStub(LoginGrpc::newBlockingStub);
            bind(LoginGrpc.LoginBlockingStub.class).toInstance(stub);
        } catch (Exception e) {
            this.addError(e);
        }
    }
}
