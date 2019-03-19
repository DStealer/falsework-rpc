package com.falsework.core.server;

import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class LifecycleServer implements ServerLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleServer.class);
    private final Server server;
    private final Set<ServerLifecycleListener> listeners;
    private boolean running;

    public LifecycleServer(Server server) {
        this.server = server;
        this.listeners = new LinkedHashSet<>();
        this.running = false;
    }

    @Override
    public void addLifecycleListener(ServerLifecycleListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public Set<ServerLifecycleListener> findLifecycleListeners() {
        return Collections.unmodifiableSet(this.listeners);
    }

    @Override
    public void removeLifecycleListener(ServerLifecycleListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void start() throws Exception {
        if (this.running) {
            throw new IllegalStateException("server is running!");
        } else if (this.server.isShutdown() || this.server.isTerminated()) {
            throw new IllegalStateException("server state shutdown or terminated");
        } else {
            this.running = true;
        }
        for (ServerLifecycleListener listener : this.listeners) {
            listener.beforeStart();
        }
        this.server.start();

        for (ServerLifecycleListener listener : this.listeners) {
            listener.afterStart();
        }
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(() -> {
                            System.err.println("*** shutting down gRPC server since JVM is shutting down");
                            try {
                                LifecycleServer.this.stop();
                                System.err.println("*** server shut down");
                            } catch (Exception e) {
                                System.err.println("*** server shut down err:" + e.toString());
                            }
                        }));
    }

    @Override
    public void stop() throws Exception {

        if (!this.running) {
            throw new IllegalStateException("server not running");
        } else {
            this.running = false;
        }

        for (ServerLifecycleListener listener : this.listeners) {
            listener.beforeStop();
        }

        this.server.shutdown();

        for (ServerLifecycleListener listener : this.listeners) {
            listener.afterStop();
        }
    }

    /**
     * 同步模式
     *
     * @throws Exception
     */
    public void sync() throws Exception {
        if (!this.running) {
            throw new IllegalStateException("server not running");
        }
        this.server.awaitTermination();
    }

    /**
     * 异步模式
     *
     * @throws Exception
     */
    public void async() throws Exception {
        new Thread("main-server-thread") {
            @Override
            public void run() {
                if (!LifecycleServer.this.running) {
                    throw new IllegalStateException("server not running");
                }
                try {
                    LifecycleServer.this.server.awaitTermination();
                } catch (Exception e) {
                    LOGGER.error("server start failed", e);
                }
            }
        }.start();
    }
}
