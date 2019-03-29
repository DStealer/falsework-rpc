package com.falsework.governance.service;

import com.falsework.core.composite.ResponseMetaException;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.generated.*;
import com.falsework.governance.registry.InstanceRegistry;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class RegistryService extends RegistryServiceGrpc.RegistryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryService.class);
    private final InstanceRegistry registry;
    private final SecureService secureService;

    @Inject
    public RegistryService(InstanceRegistry registry, SecureService secureService) {
        this.registry = registry;
        this.secureService = secureService;
    }

    @Override
    public void fetchRegistry(RegistryRequest request, StreamObserver<RegistryResponse> responseObserver) {
        RegistryResponse.Builder builder = RegistryResponse.newBuilder();
        try {
            secureService.replicaTokenCheck(request.getMeta());
            Collection<RegistryGroupInfo> groupInfos = this.registry.fetchRegistry();
            builder.setMeta(ErrorCode.NA.toResponseMeta())
                    .addAllGroupInfos(groupInfos);
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
        LOGGER.info("replica register:\n{}", request.getLease());
        RegisterResponse.Builder builder = RegisterResponse.newBuilder();
        try {
            secureService.replicaTokenCheck(request.getMeta());
            this.registry.replicaRegister(request.getLease());
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
        LOGGER.info("replica cancel:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        CancelResponse.Builder builder = CancelResponse.newBuilder();
        try {
            secureService.replicaTokenCheck(request.getMeta());
            this.registry.replicaCancel(request.getGroupName(), request.getServiceName(), request.getInstanceId());
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
    public void renew(RenewRequest request, StreamObserver<RenewResponse> responseObserver) {
        LOGGER.info("replica renew :{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        RenewResponse.Builder builder = RenewResponse.newBuilder();
        try {
            secureService.replicaTokenCheck(request.getMeta());
            RegistryLeaseInfo newlyLease = registry.replicaRenew(request.getGroupName(), request.getServiceName(), request.getInstanceId()
                    , request.getLastDirtyTimestamp());
            if (newlyLease == null) {
                builder.setMeta(ErrorCode.NA.toResponseMeta());
            } else {
                builder.setMeta(ErrorCode.ALREADY_EXISTS.toResponseMeta())
                        .setLease(newlyLease);
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
    public void change(ChangeRequest request, StreamObserver<ChangeResponse> responseObserver) {
        LOGGER.info("replica change:{}|{}|{}", request.getGroupName(), request.getServiceName(), request.getInstanceId());
        ChangeResponse.Builder builder = ChangeResponse.newBuilder();
        try {
            secureService.replicaTokenCheck(request.getMeta());
            this.registry.replicaChange(request.getGroupName(), request.getServiceName(), request.getInstanceId(),
                    request.getStatus(), request.getAttributesMap(), request.getLastDirtyTimestamp());
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
}
