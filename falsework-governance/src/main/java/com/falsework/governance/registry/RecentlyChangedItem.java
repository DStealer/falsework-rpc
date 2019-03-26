package com.falsework.governance.registry;

import com.falsework.governance.model.LeaseInfo;

public class RecentlyChangedItem {
    private final long lastUpdateTime;
    private final LeaseInfo leaseInfo;

    public RecentlyChangedItem(long lastUpdateTime, LeaseInfo leaseInfo) {
        this.lastUpdateTime = lastUpdateTime;
        this.leaseInfo = leaseInfo;
    }

    public LeaseInfo getLeaseInfo() {
        return leaseInfo;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
}
