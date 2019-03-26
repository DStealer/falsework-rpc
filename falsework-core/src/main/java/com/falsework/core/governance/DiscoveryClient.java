package com.falsework.core.governance;

import com.falsework.core.config.Props;
import com.falsework.core.config.PropsVars;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.generated.common.ResponseMeta;
import com.falsework.core.generated.governance.*;
import com.falsework.core.grpc.HttpResolverProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
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
                .keepAliveWithoutCalls(true)
                .eventLoopGroup(new NioEventLoopGroup(2, new ThreadFactoryBuilder().
                        setNameFormat("discovery-event-loop-%d").setDaemon(true).build()))
                .usePlaintext()
                .build();
        this.discoveryServiceStub = DiscoveryServiceGrpc.newBlockingStub(this.discoveryChannel);

        this.governor = new ResolverGovernor(this);
        this.scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setDaemon(true).setNameFormat("discovery-%d").build());

        String serviceName = this.props.getProperty(PropsVars.SERVER_NAME);
        String serviceGroup = this.props.getProperty(PropsVars.SERVER_GROUP);
        String serviceIp = this.props.getProperty(PropsVars.SERVER_IP);
        int servicePort = this.props.getInt(PropsVars.SERVER_PORT);
        String instanceId = String.format("%s:%d", serviceIp, servicePort);

        this.myInfo = InstanceInfo.newBuilder()
                .setInstanceId(instanceId)
                .setServiceName(serviceName)
                .setGroupName(serviceGroup)
                .setIpAddress(serviceIp)
                .setPort(servicePort)
                .setStatus(InstanceStatus.UP)
                .setHash("")
                .build();
        this.localCacheInstanceHash = new ConcurrentHashMap<>();
        this.registerSelf = this.props.getBoolean(PropsVars.DISCOVERY_REGISTER_SELF);
        this.fetchRegistry = this.props.getBoolean(PropsVars.DISCOVERY_FETCH_REGISTRY);
    }

    /**
     * 启动 并查初始化线程
     */
    public synchronized void start() {
        LOGGER.info("discovery client start...");
        this.register();
        Random random = new Random();
        if (this.registerSelf) {
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::heartbeat, random.nextInt(10), 30, TimeUnit.SECONDS);
        }
        if (this.fetchRegistry) {
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::refresh, random.nextInt(10), 30, TimeUnit.SECONDS);
        }
    }

    /**
     * 停止
     */
    public synchronized void stop() {
        LOGGER.info("discovery client stop...");
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
    private boolean register() {
        try {
            LOGGER.info("register service with:\n{}", this.myInfo);
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setInstance(this.myInfo)
                    .build();
            RegisterResponse response = this.discoveryServiceStub.register(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.info("register service success,OK....");
                return true;
            } else {
                LOGGER.warn("register service failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("register local service failed", exception);
        }
        return false;
    }

    /**
     * 注销本身实例
     *
     * @return
     */
    private boolean unregister() {
        try {
            LOGGER.info("unregister service with:\n{}", this.myInfo);
            CancelRequest request = CancelRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setInstanceId(this.myInfo.getInstanceId())
                    .setServiceName(this.myInfo.getServiceName())
                    .setGroupName(this.myInfo.getGroupName())
                    .build();
            CancelResponse response = this.discoveryServiceStub.cancel(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.info("unregister service success,OK....");
                return true;
            } else {
                LOGGER.warn("unregister service failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("unregister service failed", exception);
        }
        return false;
    }

    /**
     * 心跳
     *
     * @return
     */
    private boolean heartbeat() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setInstanceId(this.myInfo.getInstanceId())
                .setServiceName(this.myInfo.getServiceName())
                .setGroupName(this.myInfo.getGroupName())
                .build();
        try {
            HeartbeatResponse response = this.discoveryServiceStub.heartbeat(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.debug("heartbeat success,OK....");
                return true;
            } else if ("GA1002".equals(meta.getErrCode())) {
                LOGGER.warn("heartbeat failed with not register,register again....");
                return this.register();
            } else {
                LOGGER.warn("heartbeat failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("heartbeat failed", exception);
        }
        return false;
    }

    /**
     * 更新
     *
     * @return
     */
    private synchronized boolean refresh() {
        if (this.localCacheInstanceHash.size() == 0) {
            return true;
        }
        DeltaServiceRequest request = DeltaServiceRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(this.myInfo.getGroupName())
                .putAllServiceHashInfo(this.localCacheInstanceHash)
                .build();
        try {
            DeltaServiceResponse response = this.discoveryServiceStub.deltaService(request);
            ResponseMeta meta = response.getMeta();
            if (!"NA".equals(meta.getErrCode())) {
                LOGGER.warn("refresh failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
                return false;
            }
            List<ServiceInfo> deltaServiceInfoList = response.getServiceInfoListList();

            for (ServiceInfo info : deltaServiceInfoList) {
                this.governor.onChange(info.getServiceName(), new ArrayList<>(info.getInstanceMapMap().values()));
            }

            for (ServiceInfo info : deltaServiceInfoList) {
                this.localCacheInstanceHash.put(info.getServiceName(), info.getHash());
            }
            return true;
        } catch (Exception exception) {
            LOGGER.warn("discover refresh failed", exception);
        }
        return false;
    }

    /**
     * 获取依赖服务实例
     *
     * @param serviceName
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<InstanceInfo> getServiceInstance(String serviceName) {
        try {
            ServiceRequest request = ServiceRequest.newBuilder()
                    .setMeta(RequestMeta.getDefaultInstance())
                    .setServiceName(serviceName)
                    .setGroupName(this.myInfo.getGroupName())
                    .build();
            ServiceResponse response = this.discoveryServiceStub.service(request);
            ResponseMeta meta = response.getMeta();
            if (!"NA".equals(meta.getErrCode())) {
                LOGGER.warn("find service failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
                return Collections.EMPTY_LIST;
            }
            this.localCacheInstanceHash.put(serviceName, response.getServiceInfo().getHash());
            return new ArrayList<>(response.getServiceInfo().getInstanceMapMap().values());
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
        this.localCacheInstanceHash.put(serviceName, "");
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
