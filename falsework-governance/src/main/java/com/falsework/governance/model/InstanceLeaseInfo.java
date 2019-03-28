package com.falsework.governance.model;

import com.falsework.core.generated.governance.InstanceInfo;
import com.falsework.governance.generated.RegistryLeaseInfo;

import javax.annotation.Nonnull;

public class InstanceLeaseInfo implements Comparable<InstanceLeaseInfo> {
    public static final long DEFAULT_DURATION_MS = 90_000L;
    private final InnerInstanceInfo instanceInfo;
    private final long duration; //lease 有效期
    private final long registrationTimestamp; //lease 注册时间
    private volatile long lastUpdateTimestamp; //lease 续约时间
    private volatile long evictionTimestamp; //lease 失效时间
    private volatile long lastDirtyTimestamp; //lease 更新时间
    private volatile String hash;//hash 值

    public InstanceLeaseInfo(InnerInstanceInfo instanceInfo, long durationMs) {
        this.instanceInfo = instanceInfo;
        this.duration = durationMs;
        this.registrationTimestamp = System.currentTimeMillis();
        this.lastUpdateTimestamp = this.registrationTimestamp;
        this.lastDirtyTimestamp = this.registrationTimestamp;
        this.evictionTimestamp = 0L;
        this.hash = "";
    }

    public InstanceLeaseInfo(RegistryLeaseInfo info) {
        this.instanceInfo = new InnerInstanceInfo(info.getInstance());
        this.duration = info.getDuration();
        this.registrationTimestamp = info.getRegistrationTimestamp();
        this.lastUpdateTimestamp = info.getLastUpdateTimestamp();
        this.evictionTimestamp = info.getEvictionTimestamp();
        this.lastDirtyTimestamp = info.getLastDirtyTimestamp();
        this.hash = info.getHash();
    }

    public void renew() {
        this.lastUpdateTimestamp = System.currentTimeMillis();
    }

    public void cancel() {
        if (this.evictionTimestamp == 0L) {
            this.evictionTimestamp = System.currentTimeMillis();
        }
    }

    //jvm延迟执行，需要计算
    public boolean isExpired(long additionalLeaseMs) {
        return (this.evictionTimestamp > 0 || System.currentTimeMillis() > (lastUpdateTimestamp + duration + additionalLeaseMs));
    }

    public long getEvictionTimestamp() {
        return evictionTimestamp;
    }

    public long getDuration() {
        return duration;
    }

    public long getLastRenewalTimestamp() {
        return lastUpdateTimestamp;
    }

    public long getLastDirtyTimestamp() {
        return lastDirtyTimestamp;
    }

    public void setLastDirtyTimestamp(long lastDirtyTimestamp) {
        this.lastDirtyTimestamp = lastDirtyTimestamp;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    public String getHash() {
        return hash;
    }

    public InstanceInfo snapshot() {
        return this.instanceInfo.snapshot();
    }

    public RegistryLeaseInfo replicaSnapshot() {
        return RegistryLeaseInfo.newBuilder()
                .setInstance(this.instanceInfo.replicaSnapshot())
                .setDuration(this.duration)
                .setRegistrationTimestamp(this.registrationTimestamp)
                .setLastUpdateTimestamp(this.lastUpdateTimestamp)
                .setEvictionTimestamp(this.evictionTimestamp)
                .setLastDirtyTimestamp(this.lastDirtyTimestamp)
                .setHash(this.reHash())
                .build();
    }

    public String reHash() {
        this.hash = String.format("%s|%d", this.instanceInfo.getInstanceId(), this.lastDirtyTimestamp);
        return this.hash;
    }

    @Override
    public int compareTo(@Nonnull InstanceLeaseInfo o) {
        return this.instanceInfo.compareTo(o.getInstanceInfo());
    }

    public InnerInstanceInfo getInstanceInfo() {
        return instanceInfo;
    }


}
