package com.falsework.account.service;

import com.falsework.account.generated.LoginGrpc;
import com.falsework.account.generated.LoginReply;
import com.falsework.account.generated.LoginRequest;
import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoginServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServiceTest.class);

    @Test
    public void login1() throws Exception {
        ChannelManager channelManager = ChannelManagerBuilder.newBuilder().name("http://192.168.105.1:8083").build();
        channelManager.start();
        LoginGrpc.LoginBlockingStub stub = channelManager.newStub(LoginGrpc::newBlockingStub);
        LoginRequest request = LoginRequest.newBuilder().setUsername("u0").setPassword("pwd").build();
        LoginReply reply = stub.login(request);
        LOGGER.info("reply:{}", reply);
        channelManager.stop();
    }

    @Test
    public void login2() throws Exception {
        ChannelManager channelManager = ChannelManagerBuilder
                .newBuilder().name("http://192.168.105.1:8081")
                .build();
        channelManager.start();
        ExecutorService service = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 25; i++) {
            service.submit((Runnable) () -> {
                while (true) {
                    LoginGrpc.LoginBlockingStub stub = channelManager.newStub(LoginGrpc::newBlockingStub);
                    LoginRequest request = LoginRequest.newBuilder().setUsername("u12").setPassword("pwd").build();
                    LoginReply reply = stub.login(request);
                    System.out.println(reply);
                }

            });
            service.awaitTermination(1, TimeUnit.DAYS);
        }
    }

    @Test
    public void login03() throws Exception {
        ChannelManager channelManager = ChannelManagerBuilder
                .newBuilder().name("http://192.168.105.1:8081")
                .build();
        channelManager.start();
        LoginGrpc.LoginFutureStub stub = channelManager.newStub(LoginGrpc::newFutureStub);
        LoginRequest request = LoginRequest.newBuilder().setUsername("u0").setPassword("pwd").build();
        LoginReply loginReply = stub.login(request).get();
        LOGGER.info("reply:{}", loginReply);
        channelManager.stop();
    }

    @Test
    public void login04() throws Exception {
        ChannelManager channelManager = ChannelManagerBuilder
                .newBuilder().name("http://192.168.105.1:8081")
                .executor(MoreExecutors.directExecutor())
                .build();
        channelManager.start();
        LoginGrpc.LoginStub stub = channelManager.newStub(LoginGrpc::newStub);
        LoginRequest request = LoginRequest.newBuilder().setUsername("u0").setPassword("pwd").build();
        stub.login(request, new StreamObserver<LoginReply>() {
            @Override
            public void onNext(LoginReply value) {

                LOGGER.info("reply:{}", value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });
        TimeUnit.SECONDS.sleep(10);
        channelManager.stop();
    }
}