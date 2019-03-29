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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final ScheduledExecutorService scheduler;
    private final InstanceInfo myInfo;
    private final ResolverGovernor governor;
    private final ManagedChannel discoveryChannel;
    private final DiscoveryServiceGrpc.DiscoveryServiceBlockingStub stub;
    private final Map<String, String> hashInfos;
    private final boolean registerSelfEnable;
    private final boolean fetchRegistryEnable;

    public DiscoveryClient(Props props) {
        this.props = props;
        String address = this.props.getProperty(PropsVars.DISCOVERY_ADDRESS);
        this.discoveryChannel = NettyChannelBuilder.forTarget(address)
                .nameResolverFactory(HttpResolverProvider.SINGLTON)
                .keepAliveWithoutCalls(true)
                .eventLoopGroup(new NioEventLoopGroup(2, new ThreadFactoryBuilder().
                        setNameFormat("discovery-event-loop-%d").setDaemon(true).build()))
                .usePlaintext()
                .build();
        this.stub = DiscoveryServiceGrpc.newBlockingStub(this.discoveryChannel);
        this.governor = new ResolverGovernor(this);
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
                .setStatus(InstanceStatus.UNKNOWN)
                .setHash("")
                .build();
        this.hashInfos = new ConcurrentHashMap<>();
        this.registerSelfEnable = this.props.getBoolean(PropsVars.DISCOVERY_REGISTER_SELF);
        this.fetchRegistryEnable = this.props.getBoolean(PropsVars.DISCOVERY_FETCH_REGISTRY);

        this.scheduler = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                .setDaemon(true).setNameFormat("discovery-%d").build());
    }

    /**
     * 启动 并查初始化线程
     */
    public void start() {
        LOGGER.info("discovery client start...");
        //启动心跳和定时刷新任务
        Random random = new Random();
        if (this.registerSelfEnable) {
            this.register();
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::renew, random.nextInt(10), 30, TimeUnit.SECONDS);
        }
        if (this.fetchRegistryEnable) {
            this.scheduler.scheduleWithFixedDelay(DiscoveryClient.this::refresh, random.nextInt(10), 30, TimeUnit.SECONDS);
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
                    .setInstance(InstanceInfo.newBuilder(this.myInfo)
                            .setStatus(InstanceStatus.UP))
                    .build();
            RegisterResponse response = this.stub.register(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.info("register service success,OK....");
                return true;
            } else {
                LOGGER.warn("register service failed,answer:{}:{}", meta);
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("register local service failed", exception.getCause());
        }
        return false;
    }

    /**
     * 心跳
     *
     * @return
     */
    private boolean renew() {
        RenewRequest request = RenewRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setInstanceId(this.myInfo.getInstanceId())
                .setServiceName(this.myInfo.getServiceName())
                .setGroupName(this.myInfo.getGroupName())
                .build();
        try {
            RenewResponse response = this.stub.renew(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.debug("renew success,OK....");
                return true;
            } else if ("GA1002".equals(meta.getErrCode())) {
                LOGGER.warn("renew failed with not register,register again....");
                return this.register();
            } else {
                LOGGER.warn("renew failed,answer:{}:{}", meta.getErrCode(), meta.getDetails());
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("renew failed", exception);
        }
        return false;
    }

    /**
     * 更新
     *
     * @return
     */
    private boolean refresh() {
        if (this.hashInfos.size() == 0) {
            return true;
        }
        ServiceDeltaRequest request = ServiceDeltaRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(this.myInfo.getGroupName())
                .putAllHashInfos(this.hashInfos)
                .build();
        try {
            ServiceDeltaResponse response = this.stub.serviceDelta(request);
            ResponseMeta meta = response.getMeta();
            if (!"NA".equals(meta.getErrCode())) {
                LOGGER.warn("refresh failed,answer:{}", meta);
                return false;
            }
            List<DeltaServiceInfo> infosList = response.getServiceInfosList();
            for (DeltaServiceInfo deltaServiceInfo : infosList) {
                LOGGER.info("notify service change:{}", deltaServiceInfo);
                switch (deltaServiceInfo.getAction()) {
                    case MODIFY: {
                        this.governor.onChange(deltaServiceInfo.getServiceInfo().getServiceName(),
                                deltaServiceInfo.getServiceInfo().getInstancesList());
                        this.hashInfos.put(deltaServiceInfo.getServiceInfo().getServiceName(),
                                deltaServiceInfo.getServiceInfo().getHash());
                    }
                    break;
                    case ADDED:
                    case DELETED:
                    default:
                        //no-op
                }
            }
            return true;
        } catch (Exception exception) {
            LOGGER.warn("discover refresh failed", exception.getCause());
        }
        return false;
    }

    /**
     * 停止
     */
    public void stopAll() {
        LOGGER.info("discovery client stop...");
        try {
            this.scheduler.shutdown();
            this.scheduler.awaitTermination(3, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            this.scheduler.shutdownNow();
        }
        if (this.registerSelfEnable) {
            this.unregister();
        }

        try {
            this.discoveryChannel.shutdown();
            this.discoveryChannel.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.discoveryChannel.shutdownNow();
        }
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
            CancelResponse response = this.stub.cancel(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.info("unregister service success,OK....");
                return true;
            } else {
                LOGGER.warn("unregister service failed,answer:{}", meta);
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("unregister service failed", exception.getCause());
        }
        return false;
    }


    /**
     * 状态和其他信息改变
     *
     * @param status
     * @param additional
     * @return
     */
    private boolean change(InstanceStatus status, Map<String, String> additional) {
        ChangeRequest request = ChangeRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setInstanceId(this.myInfo.getInstanceId())
                .setServiceName(this.myInfo.getServiceName())
                .setGroupName(this.myInfo.getGroupName())
                .setStatus(status)
                .putAllAttributes(additional)
                .build();
        try {
            ChangeResponse response = this.stub.change(request);
            ResponseMeta meta = response.getMeta();
            if ("NA".equals(meta.getErrCode())) {
                LOGGER.info("status change success,OK....");
                return true;
            } else {
                LOGGER.warn("status change,answer:{}", meta);
            }
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("status change failed", exception);
        }
        return false;
    }

    public boolean isRegisterSelfEnable() {
        return this.registerSelfEnable;
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
            ServiceResponse response = this.stub.service(request);
            ResponseMeta meta = response.getMeta();
            if (!"NA".equals(meta.getErrCode())) {
                LOGGER.warn("find service failed,answer:{}", meta);
                return Collections.EMPTY_LIST;
            }
            this.hashInfos.put(serviceName, response.getServiceInfo().getHash());
            return response.getServiceInfo().getInstancesList();
        } catch (StatusRuntimeException exception) {
            LOGGER.warn("fetch service instance failed", exception.getCause());
            return Collections.emptyList();
        }
    }

    /**
     * 注册依赖服务
     *
     * @param serviceName
     */
    public void registerDependency(String serviceName) {
        this.hashInfos.put(serviceName, "");
    }

    /**
     * 注销依赖服务
     *
     * @param serviceName
     */
    public void deregisterDependency(String serviceName) {
        this.hashInfos.remove(serviceName);
    }

    public ResolverGovernor getGovernor() {
        return this.governor;
    }

    public boolean isFetchRegistryEnable() {
        return this.fetchRegistryEnable;
    }
}
