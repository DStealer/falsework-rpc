package com.falsework.governance.service;

import com.falsework.core.composite.ResponseMetaException;
import com.falsework.core.generated.governance.*;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.model.LeaseInfo;
import com.falsework.governance.registry.InstanceRegistry;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 服务查询
 */
@Singleton
public class DiscoveryService extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryService.class);
    private final InstanceRegistry registry;

    @Inject
    public DiscoveryService(InstanceRegistry registry) {
        this.registry = registry;
    }


    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        LOGGER.info("register from:\n{}", request.getInstance());
        RegisterResponse.Builder builder = RegisterResponse.newBuilder();
        try {
            this.registry.register(request.getInstance(), LeaseInfo.DEFAULT_DURATION_MS);
            builder.setMeta(ErrorCode.NA.toResponseMeta());
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void heartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        LOGGER.info("heart beat from:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        HeartbeatResponse.Builder builder = HeartbeatResponse.newBuilder();
        try {
            this.registry.heartbeat(request.getGroupName(), request.getServiceName(), request.getInstanceId());
            builder.setMeta(ErrorCode.NA.toResponseMeta());
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void cancel(CancelRequest request, StreamObserver<CancelResponse> responseObserver) {
        LOGGER.info("cancel from:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        CancelResponse.Builder builder = CancelResponse.newBuilder();
        try {
            registry.cancel(request.getGroupName(), request.getServiceName(), request.getInstanceId());
            builder.setMeta(ErrorCode.NA.toResponseMeta());
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void change(StatusChangeRequest request, StreamObserver<StatusChangeResponse> responseObserver) {
        LOGGER.info("change from:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        StatusChangeResponse.Builder builder = StatusChangeResponse.newBuilder();
        try {
            registry.change(request.getGroupName(), request.getServiceName(), request.getInstanceId(), request.getStatus());
            builder.setMeta(ErrorCode.NA.toResponseMeta());
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void groupName(GroupNameRequest request, StreamObserver<GroupNameResponse> responseObserver) {
        LOGGER.info("group name find...");
        GroupNameResponse.Builder builder = GroupNameResponse.newBuilder();
        try {
            Collection<String> names = registry.groupName();
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllGroupNameList(names);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void serviceName(ServiceNameRequest request, StreamObserver<ServiceNameResponse> responseObserver) {
        LOGGER.info("find service name for:{}", request.getGroupName());
        ServiceNameResponse.Builder builder = ServiceNameResponse.newBuilder();
        try {
            Collection<String> names = registry.serviceName(request.getGroupName());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllServiceNameList(names);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void service(ServiceRequest request, StreamObserver<ServiceResponse> responseObserver) {
        LOGGER.info("find service for:{}|{}", request.getGroupName(), request.getServiceName());
        ServiceResponse.Builder builder = ServiceResponse.newBuilder();
        try {
            Optional<ServiceInfo> service = registry.service(request.getGroupName(), request.getServiceName());
            if (service.isPresent()) {
                builder.setMeta(ErrorCode.NA.toResponseMeta())
                        .setServiceInfo(service.get());
            } else {
                throw ErrorCode.NOT_FOUND.asException("service not found");
            }
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void group(GroupRequest request, StreamObserver<GroupResponse> responseObserver) {
        LOGGER.info("find group name for:{}", request.getGroupName());
        GroupResponse.Builder builder = GroupResponse.newBuilder();
        try {
            Optional<GroupInfo> groupInfo = registry.group(request.getGroupName());
            if (groupInfo.isPresent()) {
                builder.setMeta(ErrorCode.NA.toResponseMeta())
                        .setGroupInfo(groupInfo.get());
            } else {
                throw ErrorCode.NOT_FOUND.asException("group not found");
            }
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deltaService(DeltaServiceRequest request, StreamObserver<DeltaServiceResponse> responseObserver) {
        LOGGER.info("find delta service for:{}/{}", request.getGroupName(), request.getServiceHashInfoMap().keySet());
        DeltaServiceResponse.Builder builder = DeltaServiceResponse.newBuilder();
        try {
            List<ServiceInfo> deltaServices = this.registry.deltaService(request.getGroupName(), request.getServiceHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllServiceInfoList(deltaServices);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deltaGroup(DeltaGroupRequest request, StreamObserver<DeltaGroupResponse> responseObserver) {
        LOGGER.info("find delta group for:{}", request.getGroupHashInfoMap().keySet());
        DeltaGroupResponse.Builder builder = DeltaGroupResponse.newBuilder();
        try {
            List<GroupInfo> deltaGroups = this.registry.deltaGroup(request.getGroupHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllGroupInfoList(deltaGroups);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deltaInstance(DeltaInstanceRequest request, StreamObserver<DeltaInstanceResponse> responseObserver) {
        LOGGER.info("find delta instance for:{}|{}|{}", request.getGroupName(), request.getServiceName(),
                request.getInstanceHashInfoMap().keySet());
        DeltaInstanceResponse.Builder builder = DeltaInstanceResponse.newBuilder();
        try {
            List<InstanceInfo> deltaInstance = this.registry.deltaInstance(request.getGroupName(), request.getServiceName(),
                    request.getInstanceHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllInstanceInfoList(deltaInstance);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
}
