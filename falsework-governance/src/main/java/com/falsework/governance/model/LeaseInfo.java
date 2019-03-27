package com.falsework.governance.model;

import com.falsework.governance.generated.RegistryLeaseInfo;

public class LeaseInfo {
    public static final long DEFAULT_DURATION_MS = 60_000L;
    private final InnerInstanceInfo holder;
    private final long duration;
    private final long registrationTimestamp;
    private volatile long lastUpdateTimestamp;
    private volatile long evictionTimestamp;

    public LeaseInfo(InnerInstanceInfo holder, long durationMs) {
        this.holder = holder;
        this.duration = durationMs;
        this.registrationTimestamp = System.currentTimeMillis();
        this.lastUpdateTimestamp = this.registrationTimestamp;
        this.evictionTimestamp = 0L;
    }

    public LeaseInfo(RegistryLeaseInfo info) {
        this.holder = new InnerInstanceInfo(info.getInstance());
        this.duration = info.getDuration();
        this.registrationTimestamp = info.getRegistrationTimestamp();
        this.lastUpdateTimestamp = info.getLastUpdateTimestamp();
        this.evictionTimestamp = info.getEvictionTimestamp();
    }

    public void renew() {
        this.lastUpdateTimestamp = System.currentTimeMillis();
    }

    public void cancel() {
        if (this.evictionTimestamp <= 0) {
            this.evictionTimestamp = System.currentTimeMillis();
        }
    }

    //jvm延迟执行，需要计算
    public boolean isExpired(long additionalLeaseMs) {
        return (this.evictionTimestamp > 0
                || System.currentTimeMillis() > (lastUpdateTimestamp + duration + additionalLeaseMs));
    }

    public InnerInstanceInfo getHolder() {
        return holder;
    }

    public long getEvictionTimestamp() {
        return evictionTimestamp;
    }

    public long getLastRenewalTimestamp() {
        return lastUpdateTimestamp;
    }

    public long getRegistrationTimestamp() {
        return registrationTimestamp;
    }

    enum Action {
        Register, Cancel, Renew
    }

}
