package com.falsework.service.core;

import com.falsework.core.client.ChannelBuilder;
import com.falsework.core.client.ChannelManager;
import com.falsework.core.grpc.HttpResolverProvider;
import org.junit.After;
import org.junit.Before;

public class BaseServiceTest {
    protected ChannelManager channelManager;

    @Before
    public void before() throws Exception {
        this.channelManager = ChannelBuilder.newBuilder()
                .name("http://127.0.0.1:8080")
                .nameFactory(HttpResolverProvider.SINGLTON)
                .build();
        this.channelManager.start();
    }

    @After
    public void after() throws Exception {
        this.channelManager.stop();
    }

}
