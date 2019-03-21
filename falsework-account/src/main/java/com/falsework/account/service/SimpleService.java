package com.falsework.account.service;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleService extends SimpleServiceGrpc.SimpleServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleService.class);

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        LOGGER.info("receive:{}", request);
        responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage("您好么").build());
        responseObserver.onCompleted();
    }
}
