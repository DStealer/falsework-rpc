package com.falsework.core.governance;

import com.falsework.core.composite.SystemUtil;
import com.falsework.core.config.Props;
import com.falsework.core.config.PropsVars;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.grpc.HttpResolverProvider;
import com.falsework.governance.generated.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 客户端服务治理实现
 */
public class DiscoveryClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryClient.class);
    private final Props props;
    private ScheduledExecutorService scheduler;
    private InstanceInfo myInfo;
    private ResolverGovernor governor;
    private ManagedChannel discoveryChannel;
    private DiscoveryServiceGrpc.DiscoveryServiceBlockingStub discoveryServiceStub;
    private Map<String, String> localCacheInstanceHash;
    private boolean registerSelf;
    private boolean fetchRegistry;

    public DiscoveryClient(Props props) {
        this.props = props;
    }

    public synchronized void init() {
        String address = this.props.getProperty(PropsVars.DISCOVERY_ADDRESS);
        this.discoveryChannel = NettyChannelBuilder.forTarget(address)
                .nameResolverFactory(HttpResolverProvider.SINGLTON)
                .directExecutor()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .eventLoopGroup(new NioEventLoopGroup(2,
                        new DefaultThreadFactory("discovery-event-loop", true)))
                .usePlaintext()
                .build();
        this.discoveryServiceStub = DiscoveryServiceGrpc.newBlockingStub(this.discoveryChannel);

        this.governor = new ResolverGovernor(this);
        this.scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setDaemon(true).setNameFormat("discovery-%d").build());

        String instanceId = UUID.randomUUID().toString();
        String serviceName = this.props.getProperty(PropsVars.SERVER_NAME);
        String serviceGroup = this.props.getProperty(PropsVars.SERVER_GROUP);
        String serviceIp = this.props.getProperty(PropsVars.SERVER_IP);
        int servicePort = this.props.getInt(PropsVars.SERVER_PORT);

        this.myInfo = InstanceInfo.newBuilder()
                .setInstanceId(instanceId)
                .setServiceName(serviceName)
                .setGroupName(serviceGroup)
                .setIpAddress(serviceIp)
                .setPort(servicePort)
                .setHostname(SystemUtil.hostname())
                .setStatus(ServingStatus.SERVICE_UNKNOWN)
                .build();
        this.localCacheInstanceHash = new ConcurrentHashMap<>();
        this.registerSelf = this.props.getBoolean(PropsVars.DISCOVERY_REGISTER_SELF);
        this.fetchRegistry = this.props.getBoolean(PropsVars.DISCOVERY_FETCH_REGISTRY);
    }

    /**
     * 启动 并查初始化线程
     */
    public synchronized void start() {
        LOGGER.info("DiscoveryClient start...");
        Random random = new Random();

        if (this.registerSelf) {
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::heartbeat, random.nextInt(10), 10, TimeUnit.SECONDS);
        }
        if (this.fetchRegistry) {
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::refresh, random.nextInt(10), 10, TimeUnit.SECONDS);
        }
    }

    /**
     * 停止
     */
    public synchronized void stop() {
        LOGGER.info("DiscoveryClient stop...");
        try {
            this.scheduler.shutdown();
            if (this.scheduler.isShutdown()) {
                this.scheduler.awaitTermination(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            this.scheduler.shutdownNow();
        }

        this.unregister();

        this.discoveryChannel.shutdown();
        try {
            this.discoveryChannel.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.discoveryChannel.shutdownNow();
        }
    }

    /**
     * 注册本身实例
     *
     * @return
     */
    private synchronized boolean register() {
        try {
            LOGGER.info("register local service with:{}", this.myInfo);
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setInstance(this.myInfo)
                    .build();
            RegisterResponse response = this.discoveryServiceStub.register(request);
            return RelayStatus.OK.equals(response.getStatus());
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("register local service failed, will try later", exception);
            return false;
        }
    }

    /**
     * 注销本身实例
     *
     * @return
     */
    private synchronized boolean unregister() {
        try {
            LOGGER.info("unregister local service with:{}", this.myInfo);
            CancelRequest request = CancelRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setInstanceId(this.myInfo.getInstanceId())
                    .setServiceName(this.myInfo.getServiceName())
                    .setGroupName(this.myInfo.getGroupName())
                    .build();
            CancelResponse response = this.discoveryServiceStub.cancel(request);
            return RelayStatus.OK.equals(response.getStatus())
                    || RelayStatus.NOT_FOUND.equals(response.getStatus());
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("unregister local service failed, ignore", exception);
            return false;
        }
    }

    /**
     * 心跳
     *
     * @return
     */
    private synchronized boolean heartbeat() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setInstanceId(this.myInfo.getInstanceId())
                .setServiceName(this.myInfo.getServiceName())
                .setGroupName(this.myInfo.getGroupName())
                .build();
        try {
            HeartbeatResponse response = this.discoveryServiceStub.heartbeat(request);
            if (RelayStatus.OK.equals(response.getStatus())) {
                return true;
            } else if (RelayStatus.NOT_FOUND.equals(response.getStatus())) {
                return this.register();
            } else {
                LOGGER.warn("heartbeat failed,return", response.getStatus());
                return false;
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("heartbeat failed", exception);
            return false;
        }
    }

    /**
     * 更新
     *
     * @return
     */
    private synchronized boolean refresh() {
        DeltaRequest request = DeltaRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(this.myInfo.getGroupName())
                .putAllServiceHashInfo(this.localCacheInstanceHash)
                .build();
        try {
            DeltaResponse response = this.discoveryServiceStub.delta(request);
            Map<String, InstanceList> instanceMap = response.getServiceInstanceMap();

            for (Map.Entry<String, InstanceList> entry : instanceMap.entrySet()) {
                this.governor.onChange(entry.getKey(), entry.getValue().getInstanceInfoListList());
            }

            if (instanceMap.size() > 0) {
                for (Map.Entry<String, InstanceList> entry : instanceMap.entrySet()) {
                    this.localCacheInstanceHash.put(entry.getKey(), entry.getValue().getHashCode());
                }
            }
            return true;
        } catch (Exception exception) {
            LOGGER.warn("discover refresh failed,try later", exception);
            return false;
        }
    }

    /**
     * 获取依赖服务实例
     *
     * @param serviceName
     * @return
     */
    public List<InstanceInfo> getServiceInstance(String serviceName) {
        try {
            InstanceRequest request = InstanceRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setServiceName(serviceName)
                    .setGroupName(this.myInfo.getGroupName())
                    .build();
            InstanceResponse response = this.discoveryServiceStub.instance(request);
            this.localCacheInstanceHash.put(serviceName, response.getInstanceList().getHashCode());
            return response.getInstanceList().getInstanceInfoListList();
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("fetch service instance failed", exception);
            return Collections.emptyList();
        }
    }

    /**
     * 注册依赖服务
     *
     * @param serviceName
     */
    public void registerDependency(String serviceName) {
        this.localCacheInstanceHash.put(serviceName, "not updated");
    }

    /**
     * 注销依赖服务
     *
     * @param serviceName
     */
    public void deregisterDependency(String serviceName) {
        this.localCacheInstanceHash.remove(serviceName);
    }

    public ResolverGovernor getGovernor() {
        return this.governor;
    }

    public boolean isFetchRegistry() {
        return this.fetchRegistry;
    }

    public boolean isRegisterSelf() {
        return this.registerSelf;
    }
}
