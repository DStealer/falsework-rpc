package com.falsework.core;

import com.falsework.core.server.LifecycleServer;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class FalseWorkApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FalseWorkApplication.class);
    private final LifecycleServer server;
    private final Injector injector;

    FalseWorkApplication(LifecycleServer server, Injector injector) {
        this.server = server;
        this.injector = injector;
    }

    public FalseWorkApplication run() throws Exception {
        LOGGER.info("************************FalseWorkApplication starting....*************************");
        injector.getAllBindings().forEach((key, binding) -> LOGGER.info("binging:{}",binding));
        this.server.start();
        LOGGER.info("************************FalseWorkApplication started....***************************");
        LOGGER.info("**********************************************************************************");
        return this;
    }

    public FalseWorkApplication stop() throws Exception {
        LOGGER.info("**********************************************************************************");
        LOGGER.info("FalseWorkApplication stopping....");
        this.server.stop();
        LOGGER.info("FalseWorkApplication stopped....");
        LOGGER.info("**********************************************************************************");
        return this;
    }

    public void sync() throws Exception {
        this.server.sync();
    }

    public void async() throws Exception {
        this.server.async();
    }

}
