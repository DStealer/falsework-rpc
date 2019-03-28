package com.falsework.core.governance;

import com.falsework.core.server.ServerListener;

public class DiscoveryLifeListener implements ServerListener {
    private final DiscoveryClient client;

    public DiscoveryLifeListener(DiscoveryClient client) {
        this.client = client;
    }

    @Override
    public void afterStart() throws Exception {
        client.start();
    }

    @Override
    public void beforeStop() throws Exception {
        client.stopAll();
    }
}
