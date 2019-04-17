package com.falsework.governance.peers;

import com.falsework.core.config.Props;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.generated.governance.InstanceStatus;
import com.falsework.core.grpc.ChannelConfigurerManager;
import com.falsework.governance.config.PropsVars;
import com.falsework.governance.generated.*;
import com.falsework.governance.model.InstanceLeaseInfo;
import com.falsework.governance.registry.InstanceRegistry;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class ReplicationPeers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationPeers.class);
    private final Props props;
    private final InstanceRegistry registry;
    private final List<ManagedChannel> channels = new CopyOnWriteArrayList<>();
    private final List<RegistryServiceGrpc.RegistryServiceStub> stubs = new CopyOnWriteArrayList<>();
    private final ForkJoinPool forkJoinPool;
    private final String replicaToken;

    public ReplicationPeers(InstanceRegistry registry, Props props) {
        this.props = props;
        this.registry = registry;
        this.forkJoinPool = ForkJoinPool.commonPool();
        this.replicaToken = this.props.getProperty(PropsVars.REGISTER_REPLICA_TOKEN);
    }

    /**
     * 启动
     *
     * @throws Exception
     */
    public void start() throws Exception {
        LOGGER.info("start peer manager ...");
        String peerAddresses = this.props.getProperty(PropsVars.REGISTER_PEER_ADDRESS);
        String[] authorities = new URI(peerAddresses).getAuthority().split(";");

        LOGGER.info("init peer manager ...{}", Arrays.toString(authorities));
        String localAuthority = this.props.getProperty(PropsVars.SERVER_IP);
        int localPort = this.props.getInt(PropsVars.SERVER_PORT);
        for (String authority : authorities) {
            String[] strings = authority.split(":", 2);
            InetSocketAddress address;
            try {
                address = new InetSocketAddress(strings[0], Integer.parseInt(strings[1]));
            } catch (NumberFormatException e) {
                address = new InetSocketAddress(strings[0], 80);
            }
            if (localAuthority.equals(address.getAddress().getHostAddress()) && localPort == address.getPort()) {
                LOGGER.info("filter local service:{}", address);
                continue;
            }
            ManagedChannel channel = NettyChannelBuilder.forAddress(address)
                    .executor(ChannelConfigurerManager.getConfigurer().getDefaultChannelExecutor())
                    .usePlaintext().build();
            this.channels.add(channel);
            this.stubs.add(RegistryServiceGrpc.newStub(channel));
        }
        LOGGER.info("init peer manager finish!");
    }

    /**
     * 停止
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        LOGGER.info("stop peer manager ...");
        try {
            this.forkJoinPool.shutdown();
            this.forkJoinPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.forkJoinPool.shutdownNow();
        }
        for (ManagedChannel channel : this.channels) {
            try {
                channel.shutdown();
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
            }
        }
        this.channels.clear();
        this.stubs.clear();

    }

    @SuppressWarnings("unchecked")
    public Collection<RegistryGroupInfo> tryFetchRegistry() {
        LOGGER.info("try fetch registry...");
        RegistryRequest request = RegistryRequest.newBuilder()
                .setMeta(tokenedMeta()).build();
        for (int i = 0; i < stubs.size(); i++) {
            CompletableFuture<RegistryResponse> future = new CompletableFuture<>();
            stubs.get(i).fetchRegistry(request, new StreamObserver<RegistryResponse>() {
                @Override
                public void onNext(RegistryResponse registryResponse) {
                    if ("NA".equals(registryResponse.getMeta().getErrCode())) {
                        future.complete(registryResponse);
                    } else {
                        LOGGER.warn("can't fetch register,answer:{}", registryResponse.getMeta());
                        future.complete(null);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }

                @Override
                public void onCompleted() {
                }
            });
            try {
                RegistryResponse response = future.get();
                if (response != null) {
                    return response.getGroupInfosList();
                }
            } catch (Exception e) {
                LOGGER.warn("fetch error", e.getCause());
            }
        }
        LOGGER.warn("can't sync up from remote peer registry,ignore");
        return Collections.EMPTY_LIST;
    }

    private RequestMeta tokenedMeta() {
        return RequestMeta.newBuilder()
                .putAttributes("replica-token", this.replicaToken)
                .build();
    }

    /**
     * 注销
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     */
    public void cancel(String groupName, String serviceName, String instanceId) {
        this.forkJoinPool.submit(() -> {
            CancelRequest request = CancelRequest.newBuilder().setMeta(tokenedMeta())
                    .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId).build();
            for (RegistryServiceGrpc.RegistryServiceStub stub : stubs) {
                stub.cancel(request, new StreamObserver<CancelResponse>() {
                    @Override
                    public void onNext(CancelResponse response) {
                        if ("NA".equals(response.getMeta().getErrCode())) {
                            LOGGER.info("replicas cancel success");
                        } else {
                            LOGGER.warn("replicas cancel failed,remote answer:{}", response.getMeta());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.warn("replicas cancel failed", throwable);
                    }

                    @Override
                    public void onCompleted() {
                    }
                });
            }
        });
    }

    /**
     * 续约
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @param lastDirtyTimestamp
     */
    public void renew(String groupName, String serviceName, String instanceId, long lastDirtyTimestamp) {
        this.forkJoinPool.submit(() -> {
            RenewRequest request = RenewRequest.newBuilder().setMeta(tokenedMeta())
                    .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId)
                    .setLastDirtyTimestamp(lastDirtyTimestamp).build();
            for (RegistryServiceGrpc.RegistryServiceStub stub : stubs) {
                stub.renew(request, new StreamObserver<RenewResponse>() {
                    @Override
                    public void onNext(RenewResponse response) {
                        if ("NA".equals(response.getMeta().getErrCode())) {
                            LOGGER.info("replicas renew success");
                        } else if ("GA1002".equals(response.getMeta().getErrCode())) {//不存在
                            LOGGER.warn("replicas renew failed,remote answer:{}-{},register again", response.getMeta().getErrCode()
                                    , response.getMeta().getDetails());
                            Optional<InstanceLeaseInfo> leaseOptional = registry.findLeaseOptional(groupName, serviceName, instanceId);
                            if (leaseOptional.isPresent()) {
                                RegisterRequest registerRequest = RegisterRequest.newBuilder()
                                        .setMeta(RequestMeta.getDefaultInstance())
                                        .setLease(leaseOptional.get().replicaSnapshot()).build();
                                stub.register(registerRequest, new StreamObserver<RegisterResponse>() {
                                    @Override
                                    public void onNext(RegisterResponse registerResponse) {
                                        if ("NA".equals(registerResponse.getMeta().getErrCode())) {
                                            LOGGER.info("replicas register success");
                                        } else {
                                            LOGGER.warn("replicas register failed,remote answer:{}", registerResponse.getMeta());
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {
                                        LOGGER.warn("replicas register failed", throwable);
                                    }

                                    @Override
                                    public void onCompleted() {
                                    }
                                });

                            } else {
                                LOGGER.warn("local copy not found,ignore");
                            }
                        } else if ("GA1003".equals(response.getMeta().getErrCode())) {//对方数据新
                            LOGGER.warn("replicas renew failed,remote answer:{}-{},need update local copy",
                                    response.getMeta().getErrCode());
                            RegistryLeaseInfo remoteLease = response.getLease();
                            registry.replicaRegister(remoteLease);
                        } else {
                            LOGGER.warn("replicas renew failed,remote answer:{}", response.getMeta());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.warn("replicas renew failed", throwable);
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
            }

        });

    }

    /**
     * 注册
     *
     * @param info
     */
    public void register(RegistryLeaseInfo info) {
        this.forkJoinPool.submit(() -> {
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setMeta(tokenedMeta())
                    .setLease(info).build();
            for (RegistryServiceGrpc.RegistryServiceStub stub : stubs) {
                stub.register(request, new StreamObserver<RegisterResponse>() {
                    @Override
                    public void onNext(RegisterResponse registerResponse) {
                        if ("NA".equals(registerResponse.getMeta().getErrCode())) {
                            LOGGER.info("replicas register success");
                        } else {
                            LOGGER.warn("replicas register failed,remote answer:{}", registerResponse.getMeta());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.warn("replicas register failed", throwable);
                    }

                    @Override
                    public void onCompleted() {

                    }
                });
            }
        });
    }

    /**
     * 改变
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @param status
     * @param attributesMap
     * @param lastDirtyTimestamp
     */
    public void change(String groupName, String serviceName, String instanceId, InstanceStatus status,
                       Map<String, String> attributesMap, long lastDirtyTimestamp) {
        this.forkJoinPool.submit(() -> {
            ChangeRequest request = ChangeRequest.newBuilder().setMeta(tokenedMeta())
                    .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId)
                    .setStatus(status).putAllAttributes(attributesMap)
                    .setLastDirtyTimestamp(lastDirtyTimestamp).build();
            for (RegistryServiceGrpc.RegistryServiceStub stub : stubs) {

                stub.change(request, new StreamObserver<ChangeResponse>() {
                    @Override
                    public void onNext(ChangeResponse changeResponse) {
                        if ("NA".equals(changeResponse.getMeta().getErrCode())) {
                            LOGGER.info("replicas change success");
                        } else {
                            LOGGER.warn("replicas change failed,remote answer:{}", changeResponse.getMeta());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        LOGGER.warn("replicas change failed", throwable);
                    }

                    @Override
                    public void onCompleted() {

                    }
                });

            }
        });
    }
}
