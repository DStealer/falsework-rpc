package com.falsework.governance.service;

import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.core.governance.FalseWorkNameResolverProvider;
import com.falsework.governance.generated.LookupServiceGrpc;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupServiceTest extends BaseServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LookupServiceTest.class);

    @Test
    public void lookup() throws Exception {
        LookupServiceGrpc.LookupServiceStub stub = this.channelManager.newStub(LookupServiceGrpc::newStub);
        FalseWorkNameResolverProvider resolverProvider = new FalseWorkNameResolverProvider(stub);
        ChannelManager accountChannel1 = ChannelManagerBuilder.newBuilder()
                .nameFactory(resolverProvider)
                .name("dynamic://account")
                .build();
        accountChannel1.start();
        ChannelManager accountChannel2 = ChannelManagerBuilder.newBuilder()
                .nameFactory(resolverProvider)
                .name("dynamic://account1")
                .build();
        accountChannel2.start();

        SimpleServiceGrpc.SimpleServiceBlockingStub stub1 = accountChannel1.newStub(SimpleServiceGrpc::newBlockingStub);
        SimpleResponse rpc1 = stub1.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("good morning").build());
        System.out.println(rpc1);

        SimpleServiceGrpc.SimpleServiceBlockingStub stub2 = accountChannel2.newStub(SimpleServiceGrpc::newBlockingStub);
        SimpleResponse rpc2 = stub2.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("nin,hao").build());
        System.out.println(rpc2);

    }

    @Test
    public void heartbeat() {
    }
}