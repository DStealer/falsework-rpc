package com.falsework.gateway.discovery;

import com.falsework.core.config.Props;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.generated.common.ResponseMeta;
import com.falsework.core.generated.governance.*;
import com.falsework.core.grpc.HttpResolverProvider;
import com.falsework.gateway.config.PropsVars;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GateWayDiscoveryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateWayDiscoveryClient.class);
    private final ManagedChannel discoveryChannel;
    private final DiscoveryServiceGrpc.DiscoveryServiceBlockingStub stub;
    private final ScheduledExecutorService scheduler;
    private final Props props;
    private final Table<String, String, List<InstanceInfo>> instanceTable;
    private final Table<String, String, String> hashInfoTable;

    @SuppressWarnings("UnstableApiUsage")
    public GateWayDiscoveryClient(Props props) {
        this.props = props;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().
                setNameFormat("discovery-schedule-%d").setDaemon(true).build());
        this.discoveryChannel = NettyChannelBuilder
                .forTarget(this.props.getProperty(PropsVars.DISCOVERY_ADDRESS))
                .nameResolverFactory(HttpResolverProvider.SINGLTON)
                .keepAliveWithoutCalls(true)
                .eventLoopGroup(new NioEventLoopGroup(2, new ThreadFactoryBuilder().
                        setNameFormat("discovery-event-loop-%d").setDaemon(true).build()))
                .usePlaintext()
                .build();
        this.stub = DiscoveryServiceGrpc.newBlockingStub(this.discoveryChannel);
        this.instanceTable = Tables.newCustomTable(new ConcurrentHashMap<>(), ConcurrentHashMap::new);
        this.hashInfoTable = Tables.newCustomTable(new ConcurrentHashMap<>(), ConcurrentHashMap::new);
    }

    public void start() {
        this.scheduler.scheduleWithFixedDelay(this::refresh, 5, 15, TimeUnit.SECONDS);
    }

    private void refresh() {
        try {
            for (Map.Entry<String, Map<String, String>> entry : this.hashInfoTable.rowMap().entrySet()) {
                ServiceDeltaRequest request = ServiceDeltaRequest.newBuilder()
                        .setMeta(RequestMeta.getDefaultInstance())
                        .setGroupName(entry.getKey())
                        .putAllHashInfos(entry.getValue())
                        .build();
                ServiceDeltaResponse response = this.stub.serviceDelta(request);
                ResponseMeta meta = response.getMeta();
                if (!"NA".equals(meta.getErrCode())) {
                    LOGGER.warn("refresh failed,answer:{}", meta);
                }
                List<DeltaServiceInfo> infosList = response.getServiceInfosList();
                for (DeltaServiceInfo deltaServiceInfo : infosList) {
                    LOGGER.info("notify service change:{}", deltaServiceInfo);
                    switch (deltaServiceInfo.getAction()) {
                        case MODIFY: {
                            this.hashInfoTable.put(deltaServiceInfo.getServiceInfo().getGroupName(),
                                    deltaServiceInfo.getServiceInfo().getServiceName(),
                                    deltaServiceInfo.getServiceInfo().getHash());
                            this.instanceTable.put(deltaServiceInfo.getServiceInfo().getGroupName(),
                                    deltaServiceInfo.getServiceInfo().getServiceName(),
                                    Collections.unmodifiableList(deltaServiceInfo.getServiceInfo().getInstancesList()));
                        }
                        break;
                        case ADDED:
                        case DELETED:
                        default:
                            //no-op
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("discover refresh failed", exception.getCause());
        }
    }


    public void stop() {
        LOGGER.info("discovery client stop...");
        try {
            this.scheduler.shutdown();
            this.scheduler.awaitTermination(3, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            this.scheduler.shutdownNow();
        }
        try {
            this.discoveryChannel.shutdown();
            this.discoveryChannel.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.discoveryChannel.shutdownNow();
        }
    }

    /**
     * 获取依赖服务实例
     *
     * @param group
     * @param serviceName
     * @return
     */
    public List<InstanceInfo> getServiceInstance(String group, String serviceName) {
        return this.instanceTable.get(group, serviceName);
    }

    /**
     * 注册依赖服务
     *
     * @param group
     * @param serviceName
     */
    public void registerDependency(String group, String serviceName) {
        this.hashInfoTable.put(group, serviceName, "");
    }

    /**
     * 注销依赖服务
     *
     * @param group
     * @param serviceName
     */
    public void deregisterDependency(String group, String serviceName) {
        this.hashInfoTable.remove(group, serviceName);
    }
}
