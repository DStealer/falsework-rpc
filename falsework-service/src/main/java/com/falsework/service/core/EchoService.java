package com.falsework.service.core;

import com.falsework.service.generated.EchoRequest;
import com.falsework.service.generated.EchoResponse;
import com.falsework.service.generated.EchoServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Echo服务实现
 */
public class EchoService extends EchoServiceGrpc.EchoServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoService.class);

    @Override
    public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        LOGGER.info("service receive msg:{}", request.getMsg());
        EchoResponse response = EchoResponse.newBuilder().setMsg(request.getMsg()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
