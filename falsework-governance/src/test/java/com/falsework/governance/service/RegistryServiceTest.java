package com.falsework.governance.service;

import com.falsework.core.generated.common.RequestMeta;
import com.falsework.governance.generated.RegistryRequest;
import com.falsework.governance.generated.RegistryResponse;
import com.falsework.governance.generated.RegistryServiceGrpc;
import org.junit.Test;

public class RegistryServiceTest extends BaseServiceTest {

    @Test
    public void registry() {
        RegistryServiceGrpc.RegistryServiceBlockingStub stub = this.channelManager.newStub(RegistryServiceGrpc::newBlockingStub);

        RegistryRequest request = RegistryRequest.newBuilder().setMeta(RequestMeta.newBuilder()
                .putAttributes("replica-token","falsework").build()).build();
        RegistryResponse response = stub.fetchRegistry(request);
        System.out.println(response);

    }
}