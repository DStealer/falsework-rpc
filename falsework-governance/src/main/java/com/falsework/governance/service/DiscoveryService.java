package com.falsework.governance.service;

import com.falsework.core.generated.common.ResponseMeta;
import com.falsework.governance.generated.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * 服务查询
 */
@Singleton
public class DiscoveryService extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryService.class);

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        LOGGER.info("heart beat from:{}-{}-{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        HeartbeatResponse.Builder builder = HeartbeatResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .setStatus(RelayStatus.OK);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        LOGGER.info("register from:{}", request.getInstance());
        RegisterResponse.Builder builder = RegisterResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .setStatus(RelayStatus.OK);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancel(CancelRequest request, StreamObserver<CancelResponse> responseObserver) {
        LOGGER.info("cancel from:{}-{}-{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        CancelResponse.Builder builder = CancelResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .setStatus(RelayStatus.OK);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void change(StatusChangeRequest request, StreamObserver<StatusChangeResponse> responseObserver) {
        LOGGER.info("cancel from:{}-{}-{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        StatusChangeResponse.Builder builder = StatusChangeResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .setStatus(RelayStatus.OK);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
