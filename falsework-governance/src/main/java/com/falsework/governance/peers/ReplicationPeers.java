package com.falsework.governance.peers;

import com.falsework.core.config.Props;
import com.falsework.core.generated.common.RequestMeta;
import com.falsework.core.generated.governance.InstanceStatus;
import com.falsework.governance.config.PropsVars;
import com.falsework.governance.generated.*;
import com.falsework.governance.model.InstanceLeaseInfo;
import com.falsework.governance.registry.InstanceRegistry;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ReplicationPeers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationPeers.class);
    private final Props props;
    private final InstanceRegistry registry;
    private final List<ManagedChannel> channels = new CopyOnWriteArrayList<>();
    private final List<RegistryServiceGrpc.RegistryServiceBlockingStub> stubs = new CopyOnWriteArrayList<>();

    public ReplicationPeers(InstanceRegistry registry, Props props) {
        this.props = props;
        this.registry = registry;
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
                    .keepAliveWithoutCalls(true).directExecutor().usePlaintext().build();
            this.channels.add(channel);
            this.stubs.add(RegistryServiceGrpc.newBlockingStub(channel));
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
        for (ManagedChannel channel : this.channels) {
            try {
                channel.shutdown();
                channel.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
            }
        }
        this.stubs.clear();
        this.channels.clear();

    }

    @SuppressWarnings("unchecked")
    public Collection<RegistryGroupInfo> tryFetchRegistry() {
        LOGGER.info("try fetch registry...");
        List<RegistryServiceGrpc.RegistryServiceBlockingStub> stubs = new ArrayList<>(this.stubs);
        Collections.shuffle(stubs);//增加随机性避免全部从某一个节点获取
        RegistryRequest request = RegistryRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance()).build();
        for (int i = 0; i < stubs.size(); i++) {
            try {
                RegistryResponse response = stubs.get(i).registry(request);
                if ("NA".equals(response.getMeta().getErrCode())) {
                    return response.getGroupInfosList();
                } else {
                    LOGGER.warn("remote answer:{}-{}", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warn("try fetch registry failed", e.getCause());
            }
        }
        LOGGER.warn("can't sync up from remote peer registry,ignore");
        return Collections.EMPTY_LIST;
    }

    /**
     * 注销
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     */
    public void cancel(String groupName, String serviceName, String instanceId) {
        CancelRequest request = CancelRequest.newBuilder().setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId).build();
        for (int i = 0; i < stubs.size(); i++) {
            try {
                CancelResponse response = stubs.get(i).cancel(request);
                if ("NA".equals(response.getMeta().getErrCode())) {
                    LOGGER.info("replicas cancel success");
                } else {
                    LOGGER.warn("replicas cancel failed,remote answer:{}-{}", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warn("replicas cancel failed", e.getCause());
            }
        }
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
        RenewRequest request = RenewRequest.newBuilder().setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId)
                .setLastDirtyTimestamp(lastDirtyTimestamp).build();
        for (int i = 0; i < stubs.size(); i++) {
            try {
                RenewResponse response = stubs.get(i).renew(request);
                if ("NA".equals(response.getMeta().getErrCode())) {
                    LOGGER.info("replicas renew success");
                } else if ("GA1002".equals(response.getMeta().getErrCode())) {//不存在
                    LOGGER.warn("replicas renew failed,remote answer:{}-{},register again", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                    Optional<InstanceLeaseInfo> leaseOptional = this.registry.findLeaseOptional(groupName, serviceName, instanceId);
                    if (leaseOptional.isPresent()) {
                        RegisterRequest registerRequest = RegisterRequest.newBuilder()
                                .setMeta(RequestMeta.getDefaultInstance())
                                .setLease(leaseOptional.get().replicaSnapshot()).build();
                        RegisterResponse registerResponse = stubs.get(i).register(registerRequest);
                        if ("NA".equals(registerResponse.getMeta().getErrCode())) {
                            LOGGER.info("replicas register success");
                        } else {
                            LOGGER.warn("replicas register failed,remote answer:{}-{}", registerResponse.getMeta().getErrCode()
                                    , response.getMeta().getDetails());
                        }
                    } else {
                        LOGGER.warn("local copy not found,ignore");
                    }
                } else if ("GA1003".equals(response.getMeta().getErrCode())) {//对方数据新
                    LOGGER.warn("replicas renew failed,remote answer:{}-{},need update local copy",
                            response.getMeta().getErrCode());
                    RegistryLeaseInfo remoteLease = response.getLease();
                    this.registry.replicaRegister(remoteLease);
                } else {
                    LOGGER.warn("replicas renew failed,remote answer:{}-{}", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warn("replicas renew failed", e.getCause());
            }
        }
    }

    /**
     * 注册
     *
     * @param info
     */
    public void register(RegistryLeaseInfo info) {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setLease(info).build();
        for (int i = 0; i < stubs.size(); i++) {
            try {
                RegisterResponse response = stubs.get(i).register(request);
                if ("NA".equals(response.getMeta().getErrCode())) {
                    LOGGER.info("replicas register success");
                } else {
                    LOGGER.warn("replicas register failed,remote answer:{}-{}", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warn("replicas register failed", e.getCause());
            }
        }
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
        ChangeRequest request = ChangeRequest.newBuilder().setMeta(RequestMeta.getDefaultInstance())
                .setGroupName(groupName).setServiceName(serviceName).setInstanceId(instanceId)
                .setStatus(status).putAllAttributes(attributesMap)
                .setLastDirtyTimestamp(lastDirtyTimestamp).build();
        for (int i = 0; i < stubs.size(); i++) {
            try {
                ChangeResponse response = stubs.get(i).change(request);
                if ("NA".equals(response.getMeta().getErrCode())) {
                    LOGGER.info("replicas change success");
                } else {
                    LOGGER.warn("replicas change failed,remote answer:{}-{}", response.getMeta().getErrCode()
                            , response.getMeta().getDetails());
                }
            } catch (StatusRuntimeException e) {
                LOGGER.warn("replicas change failed", e.getCause());
            }
        }
    }
}
