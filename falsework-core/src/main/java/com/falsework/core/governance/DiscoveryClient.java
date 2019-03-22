package com.falsework.core.governance;

import com.falsework.governance.generated.ServiceInfo;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class DiscoveryClient {
    private final ClientConfig clientConfig;
    private final List<ServiceInfo> serviceInfos;
    private final ScheduledExecutorService scheduler;
    private final ServiceInfo myInfo;

    public DiscoveryClient(ClientConfig clientConfig,
                           List<ServiceInfo> serviceInfos,
                           ScheduledExecutorService scheduler,
                           ServiceInfo myInfo) {
        this.clientConfig = clientConfig;
        this.serviceInfos = serviceInfos;
        this.scheduler = scheduler;
        this.myInfo = myInfo;
    }
}
