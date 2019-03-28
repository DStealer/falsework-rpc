package com.falsework.governance.registry;

import com.falsework.core.server.ServerListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RegistryLifeListener implements ServerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryLifeListener.class);
    private final InstanceRegistry registry;

    @Inject
    public RegistryLifeListener(InstanceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterStart() throws Exception {
        LOGGER.info("registry starting....");
        this.registry.start();
    }

    @Override
    public void beforeStop() throws Exception {
        LOGGER.info("registry stopping....");
        this.registry.stop();
    }
}
