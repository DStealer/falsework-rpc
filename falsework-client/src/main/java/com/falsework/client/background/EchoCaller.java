package com.falsework.client.background;

import com.falsework.core.server.ServerListener;
import com.falsework.service.generated.EchoRequest;
import com.falsework.service.generated.EchoResponse;
import com.falsework.service.generated.EchoServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class EchoCaller implements ServerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoCaller.class);
    private final Timer timer;
    private final EchoServiceGrpc.EchoServiceStub stub;

    @Inject
    public EchoCaller(EchoServiceGrpc.EchoServiceStub stub) {
        this.stub = stub;
        timer = new Timer("echo-caller");
    }

    @Override
    public void afterStart() throws Exception {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                EchoRequest request = EchoRequest.newBuilder()
                        .setMsg(UUID.randomUUID().toString()).build();
                LOGGER.info("send msg:{}", request.getMsg());
                stub.echo(request, new StreamObserver<EchoResponse>() {
                    @Override
                    public void onNext(EchoResponse echoResponse) {
                        LOGGER.info("receive msg:{}", echoResponse.getMsg());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.warn("receive failed", throwable);
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.info("call ending...");
                    }
                });
            }
        }, 1000, 1000);
    }
}
