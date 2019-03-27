package com.falsework.governance.registry;

import com.falsework.core.config.Props;
import com.falsework.core.generated.governance.GroupInfo;
import com.falsework.core.generated.governance.InstanceInfo;
import com.falsework.core.generated.governance.InstanceStatus;
import com.falsework.core.generated.governance.ServiceInfo;
import com.falsework.governance.composite.ErrorCode;
import com.falsework.governance.config.PropsVars;
import com.falsework.governance.model.InnerGroupInfo;
import com.falsework.governance.model.InnerInstanceInfo;
import com.falsework.governance.model.InnerServiceInfo;
import com.falsework.governance.model.LeaseInfo;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class InstanceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceRegistry.class);
    private final Props props;
    private final ConcurrentHashMap<String, InnerGroupInfo> multiGroupInfo = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();//需要同步的读写锁
    private final long evictionIntervalMs;
    private Timer evictionTimer = new Timer("governance-eviction-timer", true); //失效移除任务

    @Inject
    public InstanceRegistry(Props props) {
        this.props = props;
        this.evictionIntervalMs = this.props.getLong(PropsVars.REGISTER_EVICTION_INTERVAL);
    }


    /**
     * 服务注册
     *
     * @param instanceInfo
     * @param durationInSecs
     */
    public void register(final InstanceInfo instanceInfo, final long durationInSecs) {
        LOGGER.info("register:{}", instanceInfo.getInstanceId());
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

            LeaseInfo instanceInfoLease = serviceInfo.getInstanceMap().computeIfAbsent(instanceInfo.getInstanceId(), k -> {
                LOGGER.info("lease:{} can't find build it", k);
                return new LeaseInfo(new InnerInstanceInfo(instanceInfo), durationInSecs);
            });
            instanceInfoLease.renew();
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
        LOGGER.info("cancel group:{},service:{},instance:{}", groupName, serviceName, instanceId);
        try {
            this.readWriteLock.readLock().lock();
            InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
            if (groupInfo != null) {
                InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
                if (serviceInfo != null) {
                    serviceInfo.getInstanceMap().remove(instanceId);
                }
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
    public void heartbeat(String groupName, String serviceName, String instanceId) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not find");
        }
        LeaseInfo instanceInfoLease = serviceInfo.getInstanceMap().get(instanceId);
        if (instanceInfoLease == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not find");
        }
        instanceInfoLease.renew();
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
    public void change(String groupName, String serviceName, String instanceId, InstanceStatus status) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not find");
        }
        LeaseInfo instanceInfoLease = serviceInfo.getInstanceMap().get(instanceId);
        if (instanceInfoLease == null) {
            throw ErrorCode.NOT_FOUND.asException("instance not find");
        }
        instanceInfoLease.getHolder().setStatus(status);
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
        List<LeaseInfo> expiredLeases = new ArrayList<>();
        int registrySize = 0;
        for (Map.Entry<String, InnerGroupInfo> groupInfoEntry : this.multiGroupInfo.entrySet()) {
            for (Map.Entry<String, InnerServiceInfo> serviceInfoEntry : groupInfoEntry.getValue().getServiceMap().entrySet()) {
                for (Map.Entry<String, LeaseInfo> leaseMap : serviceInfoEntry.getValue().getInstanceMap().entrySet()) {
                    registrySize++;
                    LeaseInfo lease = leaseMap.getValue();
                    if (lease.isExpired(additionalLeaseMs)) {
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
                LeaseInfo lease = expiredLeases.get(i);
                String groupName = lease.getHolder().getGroupName();
                String serviceName = lease.getHolder().getServiceName();
                String instanceId = lease.getHolder().getInstanceId();
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
                    serviceInfo.getInstanceMap().remove(instanceId);
                } finally {
                    this.readWriteLock.readLock().unlock();
                }
            }
        }

    }

    private boolean isSelfPreservationModeEnabled() {
        return this.props.getBoolean(PropsVars.REGISTER_SELF_PRESERVATION_MODE_ENABLED);
    }

    private boolean isLeaseExpirationEnabled() {
        if (!isSelfPreservationModeEnabled()) {
            return true;
        }
        return true; //需要计算
    }

    /**
     * 依赖服务启动
     *
     * @throws Exception
     */
    public void start() throws Exception {
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
    }

    /**
     * 依赖服务关闭
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        this.evictionTimer.cancel();
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
    public Optional<GroupInfo> group(String groupName) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            return Optional.empty();
        }
        return Optional.of(groupInfo.snapshot());
    }

    /**
     * 查询服务
     *
     * @param groupName
     * @param serviceName
     * @return
     */
    public Optional<ServiceInfo> service(String groupName, String serviceName) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            return Optional.empty();
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            return Optional.empty();
        }
        return Optional.of(serviceInfo.snapshot());
    }

    /**
     * 查询实例
     *
     * @param groupName
     * @param serviceName
     * @return
     */
    public Optional<InstanceInfo> instance(String groupName, String serviceName, String instanceId) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            return Optional.empty();
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            return Optional.empty();
        }
        LeaseInfo instanceInfoLease = serviceInfo.getInstanceMap().get(instanceId);
        return Optional.of(instanceInfoLease.getHolder().snapshot());
    }


    /**
     * 检测组变更
     *
     * @param groupHashInfoMap
     * @return
     */
    public List<GroupInfo> deltaGroup(Map<String, String> groupHashInfoMap) {
        List<GroupInfo> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : groupHashInfoMap.entrySet()) {
            InnerGroupInfo info = this.multiGroupInfo.get(entry.getKey());
            if (info == null) {
                list.add(GroupInfo.newBuilder()
                        .setGroupName(entry.getKey())
                        .setHash("group not found")
                        .build());
                continue;
            }
            if (!info.reHash().equals(entry.getValue())) {
                list.add(info.snapshot());
            }
        }

        return list;
    }

    /**
     * 检测服务变更
     *
     * @param groupName
     * @param serviceHashInfoMap
     * @return
     */
    public List<ServiceInfo> deltaService(String groupName, Map<String, String> serviceHashInfoMap) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not find");
        }
        List<ServiceInfo> list = new ArrayList<>();
        ConcurrentHashMap<String, InnerServiceInfo> serviceMap = groupInfo.getServiceMap();
        for (Map.Entry<String, String> entry : serviceHashInfoMap.entrySet()) {
            InnerServiceInfo serviceInfo = serviceMap.get(entry.getKey());
            if (serviceInfo == null) {
                list.add(ServiceInfo.newBuilder()
                        .setGroupName(groupName)
                        .setServiceName(entry.getKey())
                        .setHash("service not fund")
                        .build());
            }
            if (!serviceInfo.reHash().equals(entry.getValue())) {
                list.add(serviceInfo.snapshot());
            }
        }

        return list;
    }

    /**
     * 检测实例变更
     *
     * @param groupName
     * @param serviceName
     * @param instanceHashInfoMap
     * @return
     */
    public List<InstanceInfo> deltaInstance(String groupName, String serviceName, Map<String, String> instanceHashInfoMap) {
        InnerGroupInfo groupInfo = this.multiGroupInfo.get(groupName);
        if (groupInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("group not found");
        }
        InnerServiceInfo serviceInfo = groupInfo.getServiceMap().get(serviceName);
        if (serviceInfo == null) {
            throw ErrorCode.NOT_FOUND.asException("service not found");
        }
        ConcurrentHashMap<String, LeaseInfo> instanceMap = serviceInfo.getInstanceMap();
        List<InstanceInfo> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : instanceHashInfoMap.entrySet()) {
            LeaseInfo info = instanceMap.get(entry.getKey());
            if (info == null) {
                list.add(InstanceInfo.newBuilder()
                        .setGroupName(groupName)
                        .setServiceName(serviceName)
                        .setInstanceId(entry.getKey())
                        .setHash("instance not fund")
                        .build());
                continue;
            }
            if (!info.getHolder().reHash().equals(entry.getValue())) {
                list.add(info.getHolder().snapshot());
            }
        }
        return list;
    }
}
