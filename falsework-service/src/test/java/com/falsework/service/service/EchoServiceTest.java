package com.falsework.service.service;

import com.falsework.service.generated.EchoRequest;
import com.falsework.service.generated.EchoResponse;
import com.falsework.service.generated.EchoServiceGrpc;
import org.junit.Test;

public class EchoServiceTest extends BaseServiceTest {

    @Test
    public void echo() {
        EchoServiceGrpc.EchoServiceBlockingStub stub = this.channelManager.newStub(EchoServiceGrpc::newBlockingStub);
        EchoResponse response = stub.echo(EchoRequest.newBuilder().setMsg("hello").build());
        System.out.println(response);
    }

    @Test
    public void forever() {
        EchoServiceGrpc.EchoServiceBlockingStub stub = this.channelManager.newStub(EchoServiceGrpc::newBlockingStub);
        while (true) {
            stub.echo(EchoRequest.newBuilder().setMsg("hello").build());
        }
    }
}