package com.falsework.governance.service;

import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.core.grpc.HttpResolverProvider;
import org.junit.After;
import org.junit.Before;

public class BaseServiceTest {
    protected ChannelManager channelManager;

    @Before
    public void before() throws Exception {
        this.channelManager = ChannelManagerBuilder.newBuilder()
                .name("http://127.0.0.1:8080")
                .nameFactory(new HttpResolverProvider())
                .build();
        this.channelManager.start();
    }

    @After
    public void after() throws Exception {
        this.channelManager.stop();
    }

}
