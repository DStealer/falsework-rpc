package com.falsework.census.service;

import com.falsework.census.generated.grpc.*;
import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.Test;

public class MetricQueryServiceTest {


    @Test
    public void tt01() {
        ManagedChannel channel = NettyChannelBuilder.forAddress("192.168.105.1", 8082)
                .usePlaintext()
                .build();
        MetricQueryServiceGrpc.MetricQueryServiceBlockingStub stub = MetricQueryServiceGrpc.newBlockingStub(channel);
        MetricReplay metric = stub.metric(MetricRequest.newBuilder().setMetricId("192.168.105.1:8081").build());
        System.out.println(metric);
    }

    @Test
    public void tt02() throws Exception {
        ChannelManager channelManager = ChannelManagerBuilder.newBuilder().name("http://106.75.20.96:80")
                .build();
        channelManager.start();
        MetricQueryServiceGrpc.MetricQueryServiceBlockingStub stub = channelManager.newStub(MetricQueryServiceGrpc::newBlockingStub);
        MetricsReplay metrics = stub.metrics(MetricsRequest.getDefaultInstance());
        System.out.println(metrics);
    }

}