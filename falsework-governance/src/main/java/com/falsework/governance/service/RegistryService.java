package com.falsework.governance.service;

import com.falsework.governance.generated.RegistryRequest;
import com.falsework.governance.generated.RegistryResponse;
import com.falsework.governance.generated.RegistryServiceGrpc;
import com.falsework.governance.registry.InstanceRegistry;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryService extends RegistryServiceGrpc.RegistryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryService.class);
    private final InstanceRegistry registry;

    @Inject
    public RegistryService(InstanceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registry(RegistryRequest request, StreamObserver<RegistryResponse> responseObserver) {
        super.registry(request, responseObserver);
    }
}
