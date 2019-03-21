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
public class LookupService extends LookupServiceGrpc.LookupServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(LookupService.class);

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        LOGGER.info("lookup service:{}", request.getServiceName());
        LookupResponse response = LookupResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .addServiceInfoList(ServiceInfo.newBuilder()
                        .setServiceId("some_id")
                        .setServiceName("account")
                        .setGroupName("beijing")
                        .setHostname("localhost")
                        .setIpAddress("127.0.0.1")
                        .setPort(8081)
                        .setStatus(ServingStatus.NOT_SERVING)
                        .putAttributes("key1", "value1")
                        .putAttributes("key2", "value2")
                        .build())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {

    }
}
