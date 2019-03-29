package com.falsework.governance.registry;

import com.falsework.core.config.Props;
import com.falsework.core.generated.governance.*;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.config.PropsVars;
import com.falsework.governance.generated.RegistryGroupInfo;
import com.falsework.governance.generated.RegistryInstanceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;
import com.falsework.governance.model.InnerGroupInfo;
import com.falsework.governance.model.InnerInstanceInfo;
import com.falsework.governance.model.InnerServiceInfo;
import com.falsework.governance.model.InstanceLeaseInfo;
import com.falsework.governance.peers.ReplicationPeers;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Singleton
public class InstanceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceRegistry.class);
    private final Props props;
    private final ConcurrentHashMap<String, InnerGroupInfo> multiGroupInfo = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();//需要同步的读写锁
    private final long evictionIntervalMs;
    private final boolean preservationModeEnabled;
    private final ReplicationPeers replicationPeers;
    private Timer evictionTimer = new Timer("governance-eviction-timer", true); //失效移除任务

    @Inject
    public InstanceRegistry(Props props) {
        this.props = props;
        this.evictionIntervalMs = this.props.getLong(PropsVars.REGISTER_EVICTION_INTERVAL);
        this.preservationModeEnabled = this.props.getBoolean(PropsVars.REGISTER_PRESERVATION_MODE_ENABLED);
        if (this.props.existProps(PropsVars.REGISTER_PEER_ADDRESS)) {
            this.replicationPeers = new ReplicationPeers(this, props);
        } else {
            this.replicationPeers = null;
        }
    }


    /**
     * 服务注册
     *
     * @param instanceInfo
     * @param durationInMs
     */
    public void register(final InstanceInfo instanceInfo, final long durationInMs) {
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.computeIfAbsent(instanceInfo.getGroupName(), k -> {
                LOGGER.info("group:{} can't find build it", k);
                return new InnerGroupInfo(k);
            });

            InnerServiceInfo serviceInfo = groupInfo.getServiceMap().computeIfAbsent(instanceInfo.getServiceName(), k -> {
                LOGGER.info("service:{} can't find build it", k);
                return new InnerServiceInfo(instanceInfo.getGroupName(), instanceInfo.getServiceName());
            });

            InstanceLeaseInfo instanceInfoLease = serviceInfo.getLeaseMap().computeIfAbsent(instanceInfo.getInstanceId(), k -> {
                LOGGER.info("lease:{} can't find build it", k);
                return new InstanceLeaseInfo(new InnerInstanceInfo(instanceInfo), durationInMs);
            });

            instanceInfoLease.renew();
            serviceInfo.markDirty();//记录变更
            groupInfo.markDirty();//记录变更

            if (this.replicationPeers != null) {
                this.replicationPeers.register(instanceInfoLease.replicaSnapshot());
            }
            LOGGER.info("register success");
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * 服务注销
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     */
    public void cancel(String groupName, String serviceName, String instanceId) {
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
            if (groupInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("group not find");
            }
            InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
            if (serviceInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("service not find");
            }
            InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
            if (leaseInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("instance not find");
            }
            serviceInfo.getLeaseMap().remove(instanceId);
            serviceInfo.markDirty();//记录变更
            groupInfo.markDirty();//记录变更
            if (this.replicationPeers != null) {
                this.replicationPeers.cancel(groupName, serviceName, instanceId);
            }
            LOGGER.info("cancel success");
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }


    /**
     * 服务续约
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @return
     */
    public void renew(String groupName, String serviceName, String instanceId) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not find");
        }
        InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
        if (leaseInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not find");
        }
        leaseInfo.renew();
        if (this.replicationPeers != null) {
            this.replicationPeers.renew(groupName, serviceName, instanceId,
                    leaseInfo.getInstanceInfo().getLastDirtyTimestamp());
        }
    }

    /**
     * 状态改变
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @param status
     * @return
     */
    public void change(String groupName, String serviceName, String instanceId, InstanceStatus status,
                       Map<String, String> attributesMap) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not find");
        }
        InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
        if (leaseInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not find");
        }
        InnerInstanceInfo instanceInfo = leaseInfo.getInstanceInfo();
        instanceInfo.setStatus(status);
        instanceInfo.getAttributes().clear();
        instanceInfo.getAttributes().putAll(attributesMap);
        instanceInfo.markDirty();
        serviceInfo.markDirty();//记录变更
        groupInfo.markDirty();//记录变更
        if (this.replicationPeers != null) {
            this.replicationPeers.change(groupName, serviceName, instanceId, status,
                    attributesMap, instanceInfo.getLastDirtyTimestamp());
        }
    }

    /**
     * 依赖服务启动
     *
     * @throws Exception
     */
    public void start() throws Exception {
        LOGGER.info("start instance registry...");
        if (this.replicationPeers != null) {
            this.replicationPeers.start();
            this.syncUp();
        } else {
            LOGGER.info("replication function disable");
        }

        LOGGER.info("schedule evict timer task with {}ms", this.evictionIntervalMs);
        this.evictionTimer.schedule(new TimerTask() {
                                        private final AtomicLong lastExecutionNanosRef = new AtomicLong(0L);

                                        @Override
                                        public void run() {
                                            evict(this.getCompensationTimeMs());
                                        }

                                        //jvm 可能会导致执行延时,所以需要计算，时间误差
                                        private long getCompensationTimeMs() {
                                            long currNanos = System.nanoTime();
                                            long lastNanos = lastExecutionNanosRef.getAndSet(currNanos);
                                            if (lastNanos == 0L) {
                                                return 0L;
                                            }

                                            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(currNanos - lastNanos);
                                            long compensationTime = elapsedMs - evictionIntervalMs;
                                            return compensationTime <= 0L ? 0L : compensationTime;
                                        }
                                    },
                this.evictionIntervalMs,
                this.evictionIntervalMs);
        LOGGER.info("instance registry! start Ok...");
    }

    /**
     * 从其他节点同步库
     */
    private void syncUp() {
        LOGGER.info("sync up enabled...");
        Collection<RegistryGroupInfo> registryGroupInfos = this.replicationPeers.tryFetchRegistry();

        if (registryGroupInfos.size() > 0) {
            LOGGER.info("fetch registry from remote peer registry:\n{}", registryGroupInfos);
            try {
                this.readWriteLock.writeLock().lock();
                for (RegistryGroupInfo info : registryGroupInfos) {
                    this.multiGroupInfo.put(info.getGroupName(), new InnerGroupInfo(info));
                }
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
            LOGGER.info("sync success ,Ok...");
        } else {
            LOGGER.info("sync zero instance...!!!");
        }
    }

    /**
     * 驱逐失效节点
     *
     * @param additionalLeaseMs
     */
    private void evict(long additionalLeaseMs) {
        LOGGER.info("Running the evict task...");
        if (!isLeaseExpirationEnabled()) {
            LOGGER.info("lease expiration is currently disabled.");
            return;
        }
        //先收集失效lease
        List<InstanceLeaseInfo> expiredLeases = new ArrayList<>();
        int registrySize = 0;
        for (Map.Entry<String, InnerGroupInfo> groupInfoEntry : this.multiGroupInfo.entrySet()) {
            for (Map.Entry<String, InnerServiceInfo> serviceInfoEntry : groupInfoEntry.getValue().getServiceMap().entrySet()) {
                for (Map.Entry<String, InstanceLeaseInfo> leaseMap : serviceInfoEntry.getValue().getLeaseMap().entrySet()) {
                    registrySize++;
                    InstanceLeaseInfo lease = leaseMap.getValue();
                    if (lease.isExpired(additionalLeaseMs)) {
                        lease.getInstanceInfo().setStatus(InstanceStatus.UNKNOWN);
                        expiredLeases.add(lease);
                    }
                }
            }
        }
        if (expiredLeases.size() == 0) {
            LOGGER.info("find zero lease expiration");
            return;
        }
        int registrySizeThreshold = (int) (registrySize * 0.85);
        int evictionLimit = registrySize - registrySizeThreshold;
        int toEvict = Math.min(expiredLeases.size(), evictionLimit);
        if (toEvict > 0) {
            LOGGER.info("Evicting {} items (expired={}, evictionLimit={})", toEvict, expiredLeases.size(), evictionLimit);
            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < toEvict; i++) {
                // 随机选取进行移除
                int next = i + random.nextInt(expiredLeases.size() - i);
                Collections.swap(expiredLeases, i, next);
                InstanceLeaseInfo lease = expiredLeases.get(i);
                String groupName = lease.getInstanceInfo().getGroupName();
                String serviceName = lease.getInstanceInfo().getServiceName();
                String instanceId = lease.getInstanceInfo().getInstanceId();
                LOGGER.info("Registry expired lease for {}|{}|{}", groupName, serviceName, instanceId);
                try {
                    this.readWriteLock.readLock().lock();
                    InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
                    if (groupInfo == null) {
                        continue;
                    }
                    InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
                    if (serviceInfo == null) {
                        continue;
                    }
                    serviceInfo.getLeaseMap().remove(instanceId);
                    serviceInfo.markDirty();
                    groupInfo.markDirty();
                } finally {
                    this.readWriteLock.readLock().unlock();
                }
            }
        }

        //避免无效对象过多,移除
        if (this.readWriteLock.writeLock().tryLock()) {
            try {
                List<String> invalidGroups = new LinkedList<>();
                for (Map.Entry<String, InnerGroupInfo> groupInfoEntry : this.multiGroupInfo.entrySet()) {
                    List<String> invalidServices = new LinkedList<>();
                    for (Map.Entry<String, InnerServiceInfo> serviceInfoEntry : groupInfoEntry.getValue().getServiceMap().entrySet()) {
                        if (serviceInfoEntry.getValue().getLeaseMap().size() == 0) {
                            invalidServices.add(serviceInfoEntry.getKey());
                        }
                    }
                    for (String service : invalidServices) {
                        groupInfoEntry.getValue().getServiceMap().remove(service);
                        LOGGER.info("remove invalid service:{}", service);
                    }
                    if (groupInfoEntry.getValue().getServiceMap().size() == 0) {
                        invalidGroups.add(groupInfoEntry.getKey());
                    }
                }
                for (String group : invalidGroups) {
                    this.multiGroupInfo.remove(group);
                    LOGGER.info("remove invalid service:{}", group);
                }
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

    }

    private boolean isLeaseExpirationEnabled() {
        if (!this.preservationModeEnabled) {
            return true;
        }
        return true; //需要计算
    }

    /**
     * 服务关闭
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        LOGGER.info("instance registry! stop...");
        this.replicationPeers.stop();
        this.evictionTimer.cancel();
        this.multiGroupInfo.clear();
        LOGGER.info("stop finished...");
    }

    /**
     * 查询所有组名
     *
     * @return
     */
    public Collection<String> groupName() {
        return this.multiGroupInfo.keySet();
    }

    /**
     * 查询所有服务名
     *
     * @param groupName
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<String> serviceName(String groupName) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            return Collections.EMPTY_LIST;
        }
        return groupInfo.getServiceMap().keySet();
    }

    /**
     * 查询组
     *
     * @param groupName
     * @return
     */
    public GroupInfo group(String groupName) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not found");
        }
        return groupInfo.snapshot();
    }

    /**
     * 查询服务
     *
     * @param groupName
     * @param serviceName
     * @return
     */
    public ServiceInfo service(String groupName, String serviceName) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not found");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not found");
        }
        return serviceInfo.snapshot();
    }

    /**
     * 查询实例
     *
     * @param groupName
     * @param serviceName
     * @return
     */
    public InstanceInfo instance(String groupName, String serviceName, String instanceId) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not found");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not found");
        }
        InstanceLeaseInfo instanceInfoLease = serviceInfo.getLeaseMap().get(instanceId);
        if (instanceInfoLease == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not found");
        }
        return instanceInfoLease.getInstanceInfo().snapshot();
    }

    /**
     * 查找lease
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @return
     */
    public Optional<InstanceLeaseInfo> findLeaseOptional(String groupName, String serviceName, String instanceId) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            return Optional.empty();
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            return Optional.empty();
        }
        InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
        return Optional.ofNullable(leaseInfo);
    }

    /**
     * 检测组变更
     *
     * @param groupHashInfoMap
     * @return
     */
    public Collection<DeltaGroupInfo> groupDelta(Map<String, String> groupHashInfoMap) {
        List<DeltaGroupInfo> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : groupHashInfoMap.entrySet()) {
            InnerGroupInfo info = this.multiGroupInfo.get(entry.getKey());
            if (info == null) {
                list.add(DeltaGroupInfo.newBuilder()
                        .setAction(Action.DELETED)
                        .setGroupInfo(GroupInfo.newBuilder()
                                .setGroupName(entry.getKey())
                                .build())
                        .build());
            } else if (!info.getHash().equals(entry.getValue())) {
                list.add(DeltaGroupInfo.newBuilder()
                        .setGroupInfo(info.snapshot())
                        .setAction(Action.MODIFY)
                        .build());
            }
        }

        return list;
    }

    /**
     * 检测服务变更
     *
     * @param groupName
     * @param hashInfos
     * @return
     */
    public Collection<DeltaServiceInfo> serviceDelta(String groupName, Map<String, String> hashInfos) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        List<DeltaServiceInfo> list = new ArrayList<>();
        ConcurrentHashMap<String, InnerServiceInfo> serviceMap = groupInfo.getServiceMap();
        for (Map.Entry<String, String> entry : hashInfos.entrySet()) {
            InnerServiceInfo serviceInfo = serviceMap.get(entry.getKey());
            if (serviceInfo == null) {
                list.add(DeltaServiceInfo.newBuilder()
                        .setServiceInfo(ServiceInfo.newBuilder()
                                .setGroupName(groupName)
                                .setServiceName(entry.getKey())
                                .build())
                        .setAction(Action.DELETED)
                        .build());
            } else if (!serviceInfo.getHash().equals(entry.getValue())) {
                list.add(DeltaServiceInfo.newBuilder()
                        .setServiceInfo(serviceInfo.snapshot())
                        .setAction(Action.MODIFY)
                        .build());
            }
        }

        return list;
    }

    /**
     * 检测实例变更
     *
     * @param groupName
     * @param serviceName
     * @param hashInfos
     * @return
     */
    public Collection<DeltaInstanceInfo> instanceDelta(String groupName, String serviceName, Map<String, String> hashInfos) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not found");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not found");
        }
        ConcurrentHashMap<String, InstanceLeaseInfo> instanceMap = serviceInfo.getLeaseMap();
        List<DeltaInstanceInfo> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashInfos.entrySet()) {
            InstanceLeaseInfo leaseInfo = instanceMap.get(entry.getKey());
            if (leaseInfo == null) {
                list.add(DeltaInstanceInfo.newBuilder()
                        .setInstanceInfo(InstanceInfo.newBuilder()
                                .setGroupName(groupName)
                                .setServiceName(serviceName)
                                .setInstanceId(entry.getKey())
                                .build())
                        .setAction(Action.DELETED)
                        .build());
            } else if (!leaseInfo.getInstanceInfo().getHash().equals(entry.getValue())) {
                list.add(DeltaInstanceInfo.newBuilder()
                        .setInstanceInfo(leaseInfo.getInstanceInfo().snapshot())
                        .setAction(Action.MODIFY)
                        .build());
            }
        }
        return list;
    }

    /**
     * 导出整个库,需要读锁,防止在在库同步的时候导出{@link #syncUp()}
     *
     * @return
     */
    public Collection<RegistryGroupInfo> fetchRegistry() {
        try {
            this.readWriteLock.readLock().lock();
            return this.multiGroupInfo.values().stream()
                    .map(InnerGroupInfo::replicaSnapshot)
                    .collect(Collectors.toList());
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * 复制请求注册
     *
     * @param lease
     */
    public void replicaRegister(RegistryLeaseInfo lease) {
        RegistryInstanceInfo instance = lease.getInstance();
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.computeIfAbsent(instance.getGroupName(), k -> {
                LOGGER.info("group:{} can't find build it", k);
                return new InnerGroupInfo(k);
            });

            InnerServiceInfo serviceInfo = groupInfo.getServiceMap().computeIfAbsent(instance.getServiceName(), k -> {
                LOGGER.info("service:{} can't find build it", k);
                return new InnerServiceInfo(instance.getGroupName(), instance.getServiceName());
            });

            InstanceLeaseInfo existLease = serviceInfo.getLeaseMap().get(instance.getInstanceId());
            if (existLease == null) {
                serviceInfo.getLeaseMap().put(instance.getInstanceId(), new InstanceLeaseInfo(lease));
                serviceInfo.markDirty();
                groupInfo.markDirty();
                LOGGER.info("register success");
            } else {
                if (existLease.getInstanceInfo().getLastDirtyTimestamp() <= lease.getInstance().getLastDirtyTimestamp()) {
                    LOGGER.info("replace exist lease:{} <= {}", existLease.getInstanceInfo().getLastDirtyTimestamp(),
                            lease.getInstance().getLastDirtyTimestamp());
                    serviceInfo.getLeaseMap().put(instance.getInstanceId(), new InstanceLeaseInfo(lease));
                    serviceInfo.markDirty();
                    groupInfo.markDirty();
                } else {
                    LOGGER.warn("oop!,this should not happen:{} > {}", existLease.getInstanceInfo().getLastDirtyTimestamp(),
                            lease.getInstance().getLastDirtyTimestamp());
                }
            }
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * 复制删除请求
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     */
    public void replicaCancel(String groupName, String serviceName, String instanceId) {
        LOGGER.info("replica cancel group:{},service:{},instance:{}", groupName, serviceName, instanceId);
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
            if (groupInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("group not find");
            }
            InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
            if (serviceInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("service not find");
            }
            InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
            if (leaseInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("instance not find");
            }
            serviceInfo.getLeaseMap().remove(instanceId);
            serviceInfo.markDirty();
            groupInfo.markDirty();
            LOGGER.info("cancel success");
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * 复制状态更改
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @param status
     * @param attributesMap
     * @param lastDirtyTimestamp
     */
    public void replicaChange(String groupName, String serviceName, String instanceId, InstanceStatus status,
                              Map<String, String> attributesMap, long lastDirtyTimestamp) {
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
            if (groupInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("group not find");
            }
            InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
            if (serviceInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("service not find");
            }
            InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
            if (leaseInfo == null) {
                throw ErrorCode.NOT_FOUND.asException("instance not find");
            }
            if (leaseInfo.getInstanceInfo().getLastDirtyTimestamp() <= lastDirtyTimestamp) {
                InnerInstanceInfo instanceInfo = leaseInfo.getInstanceInfo();
                instanceInfo.setStatus(status);
                instanceInfo.getAttributes().clear();
                instanceInfo.getAttributes().putAll(attributesMap);
                leaseInfo.getInstanceInfo().markDirty();
                serviceInfo.markDirty();
                groupInfo.markDirty();
                LOGGER.info("replica change success");
            } else {
                LOGGER.warn("oop!,this should not happen:{} > {}", leaseInfo.getInstanceInfo().getLastDirtyTimestamp(),
                        lastDirtyTimestamp);
            }
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    /**
     * 复制续约，如果
     *
     * @param groupName
     * @param serviceName
     * @param instanceId
     * @param lastDirtyTimestamp
     * @return
     */
    public RegistryLeaseInfo replicaRenew(String groupName, String serviceName, String instanceId, long lastDirtyTimestamp) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not find");
        }
        InstanceLeaseInfo leaseInfo = serviceInfo.getLeaseMap().get(instanceId);
        if (leaseInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not find");
        }
        if (leaseInfo.getInstanceInfo().getLastDirtyTimestamp() < lastDirtyTimestamp) {
            LOGGER.warn("my instance is dirty...{}|{}|{}", groupName, serviceName, instanceId);
            throw ErrorCode.NOT_FOUND.asException("instance is dirty");
        } else if (leaseInfo.getInstanceInfo().getLastDirtyTimestamp() > lastDirtyTimestamp) {
            LOGGER.warn("bs:my instance is newly...{}|{}|{}", groupName, serviceName, instanceId);
            return leaseInfo.replicaSnapshot();
        } else {
            leaseInfo.renew();
            return null;
        }
    }
}
