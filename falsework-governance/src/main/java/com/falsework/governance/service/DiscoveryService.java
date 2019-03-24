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

    @Override
    public void group(GroupRequest request, StreamObserver<GroupResponse> responseObserver) {
        LOGGER.info("group ...");
        GroupResponse.Builder builder = GroupResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .addGroupNameList("default")
                .addGroupNameList("develop")
                .addGroupNameList("online");
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void service(ServiceRequest request, StreamObserver<ServiceResponse> responseObserver) {
        LOGGER.info("service ... group:{}", request.getGroupName());
        ServiceResponse.Builder builder = ServiceResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .addServiceNameList("service-v1")
                .addServiceNameList("service-v2");
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void instance(InstanceRequest request, StreamObserver<InstanceResponse> responseObserver) {
        LOGGER.info("instance ... group:{},service:{}", request.getGroupName(), request.getServiceName());
        InstanceList.Builder instanceList = InstanceList.newBuilder();
        instanceList.addInstanceInfoList(InstanceInfo.newBuilder()
                .setInstanceId("server-1")
                .setServiceName("service-v1")
                .setGroupName("default")
                .setHostname("localhost")
                .setIpAddress("127.0.0.1")
                .setPort(9001)
                .setStatus(ServingStatus.SERVING)
                .build())
                .setHashCode("-1");
        InstanceResponse.Builder builder = InstanceResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .setInstanceList(instanceList.build());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void full(FullRequest request, StreamObserver<FullResponse> responseObserver) {
        LOGGER.info("full ... group:{}", request.getGroupName());
        InstanceList.Builder instanceList = InstanceList.newBuilder();
        instanceList.addInstanceInfoList(InstanceInfo.newBuilder()
                .setInstanceId("server-1")
                .setServiceName("service-v1")
                .setGroupName("default")
                .setHostname("localhost")
                .setIpAddress("127.0.0.1")
                .setPort(9001)
                .setStatus(ServingStatus.SERVING)
                .build())
                .setHashCode("-1");
        FullResponse.Builder builder = FullResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance())
                .putServiceInstance("service-v1", instanceList.build());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void delta(DeltaRequest request, StreamObserver<DeltaResponse> responseObserver) {
        LOGGER.info("delta ... group:{}:{}", request.getGroupName(), request.getServiceHashInfoMap());
        DeltaResponse.Builder builder = DeltaResponse.newBuilder()
                .setMeta(ResponseMeta.getDefaultInstance());
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
