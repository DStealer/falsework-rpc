package com.falsework.governance.service;

import com.falsework.core.composite.ResponseMetaException;
import com.falsework.core.generated.governance.*;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.model.InstanceLeaseInfo;
import com.falsework.governance.registry.InstanceRegistry;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;

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
    public void renew(RenewRequest request, StreamObserver<RenewResponse> responseObserver) {
        LOGGER.info("renew from:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        RenewResponse.Builder builder = RenewResponse.newBuilder();
        try {
            this.registry.renew(request.getGroupName(), request.getServiceName(), request.getInstanceId());
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
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        LOGGER.info("register from:\n{}", request.getInstance());
        RegisterResponse.Builder builder = RegisterResponse.newBuilder();
        try {
            this.registry.register(request.getInstance(), InstanceLeaseInfo.DEFAULT_DURATION_MS);
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
            registry.change(request.getGroupName(), request.getServiceName(), request.getInstanceId(),
                    request.getStatus(), request.getAttributesMap());
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
                    .addAllGroupNames(names);
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
                    .addAllServiceNames(names);
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
            ServiceInfo service = registry.service(request.getGroupName(), request.getServiceName());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .setServiceInfo(service);
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
            GroupInfo groupInfo = registry.group(request.getGroupName());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .setGroupInfo(groupInfo);
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
            Collection<GroupInfo> deltaGroups = this.registry.deltaGroup(request.getGroupHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllGroupInfos(deltaGroups);
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
            Collection<ServiceInfo> deltaServices = this.registry.deltaService(request.getGroupName(), request.getServiceHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllServiceInfos(deltaServices);
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
            Collection<InstanceInfo> deltaInstance = this.registry.deltaInstance(request.getGroupName(), request.getServiceName(),
                    request.getInstanceHashInfoMap());
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllInstanceInfos(deltaInstance);
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
