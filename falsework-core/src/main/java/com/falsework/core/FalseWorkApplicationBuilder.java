package com.falsework.core;

import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.aop.common.FixObjectModule;
import com.falsework.core.aop.common.InternalHelper;
import com.falsework.core.common.Builder;
import com.falsework.core.common.Holder;
import com.falsework.core.common.Props;
import com.falsework.core.server.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.InternalNettyServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.util.NettyRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * 应用构建器
 */
public class FalseWorkApplicationBuilder implements Builder<FalseWorkApplication> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FalseWorkApplicationBuilder.class);
    private final Props props;
    private final Set<AbstractModule> modules = new LinkedHashSet<>();
    private final Set<ServerInterceptor> interceptors = new LinkedHashSet<>();
    private final Set<ServerLifecycleListener> listeners = new LinkedHashSet<>();
    private final Holder<Stage> stageHolder = new Holder<>(Stage.PRODUCTION);
    private final Map<String, Object> global = new ConcurrentHashMap<>();

    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private FalseWorkApplicationBuilder(Props props) {
        this.props = props;
    }

    public static FalseWorkApplicationBuilder newBuilder() throws IOException {
        LOGGER.info("**********************************************************************************");
        LOGGER.info("************************FalseWorkApplication config.....*************************");
        return new FalseWorkApplicationBuilder(Props.loadFromClassPath("bootstrap.properties"));
    }

    public static FalseWorkApplicationBuilder newBuilder(String path) throws IOException {
        LOGGER.info("**********************************************************************************");
        LOGGER.info("************************FalseWorkApplication config.....*************************");
        return new FalseWorkApplicationBuilder(Props.loadFromPath(path));
    }

    public FalseWorkApplicationBuilder withModule(AbstractModule module) {
        this.modules.add(module);
        return this;
    }

    public FalseWorkApplicationBuilder withStage(Stage stage) {
        this.stageHolder.set(stage);
        return this;
    }

    public FalseWorkApplicationBuilder withInterceptor(ServerInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public FalseWorkApplicationBuilder withListener(ServerLifecycleListener listener) {
        this.listeners.add(listener);
        return this;
    }

    public FalseWorkApplicationBuilder withGlobal(String key, Object value) {
        this.global.put(key, value);
        return this;
    }

    public void runSync() throws Exception {
        build().run().sync();
    }

    public void runAsync() throws Exception {
        build().run().async();
    }


    private void validate(String ip, int port) throws RuntimeException {
        try {
            InetSocketAddress address = new InetSocketAddress(ip, port);
            NetworkInterface anInterface = NetworkInterface.getByInetAddress(address.getAddress());
            if (anInterface == null) {
                LOGGER.error("Can't binding {}:{} to any local network interface", ip, port);
                throw new RuntimeException("not valid ip or port");
            }
            if (anInterface.isLoopback()) {
                LOGGER.warn("Binging loop back interface only available in development");
            }
            LOGGER.info("Bing service on interface: {} with {}:{}", anInterface.getName(), ip, port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FalseWorkApplication build() {
        //暴露配置文件到环境中
        this.modules.add(new FixObjectModule<>(Props.class, this.props));

        //环境感知
        this.modules.stream().filter(EnvAwareModule.class::isInstance)
                .map(EnvAwareModule.class::cast)
                .forEach(e -> {
                    InternalHelper.setGlobal(e, this.global);
                    InternalHelper.setProps(e, this.props);
                    InternalHelper.setSeal(e);
                });

        Injector injector = Guice.createInjector(this.stageHolder.get(), this.modules);

        //server 相关
        String serverName = this.props.getProperty("server.name");
        String serverIp = this.props.getProperty("server.ip");
        int serverPort = this.props.getInt("server.port");
        LOGGER.info("building service:{}[{}:{}]", serverName, serverIp, serverPort);

        this.validate(serverIp, serverPort);

        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(serverIp, serverPort));

        //注册服务
        injector.getAllBindings().entrySet().stream()
                .filter(e -> BindableService.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (BindableService) e.getValue().getProvider().get()).forEach(builder::addService);

        injector.getAllBindings().entrySet().stream()
                .filter(e -> ServerServiceDefinition.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (ServerServiceDefinition) e.getValue().getProvider().get()).forEach(builder::addService);

        //服务注册
        CompositeServerRegister register = new CompositeServerRegister();
        injector.getAllBindings().entrySet().stream()
                .filter(e -> ServerRegister.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (ServerRegister) e.getValue().getProvider().get()).forEach(register::addServerRegister);


        int threadServerNumber = props.getInt("thread.server.number", NettyRuntime.availableProcessors() * 8);
        builder.executor(Executors.newFixedThreadPool(threadServerNumber, new ThreadFactoryBuilder()
                .setNameFormat("Server-executor-%d").build()));

        LOGGER.info("Server core thread:{}", threadServerNumber);

        builder.intercept(TimedInterceptor.getInstance());

        for (ServerInterceptor interceptor : this.interceptors) {
            builder.intercept(interceptor);
        }

        builder.fallbackHandlerRegistry(new FallbackHandlerRegistry());
        Server server = builder.build();
        LifecycleServer lifecycleServer = new LifecycleServer(server);
        for (ServerLifecycleListener listener : this.listeners) {
            lifecycleServer.addLifecycleListener(listener);
        }
        InternalNettyServerBuilder.setStatsRecordStartedRpcs(builder, false);
        return new FalseWorkApplication(lifecycleServer, injector, register);
    }
}
