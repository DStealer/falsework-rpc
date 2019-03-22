package com.falsework.core;

import com.falsework.core.composite.FixInstanceModule;
import com.falsework.core.common.Builder;
import com.falsework.core.common.Holder;
import com.falsework.core.composite.SystemUtil;
import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;
import com.falsework.core.config.PropsVars;
import com.falsework.core.grpc.CompositeResolverFactoryManager;
import com.falsework.core.grpc.HttpResolverProvider;
import com.falsework.core.grpc.LoadBalancerProviderManager;
import com.falsework.core.grpc.SharedExecutorManager;
import com.falsework.core.server.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import io.grpc.*;
import io.grpc.netty.InternalNettyServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.util.NettyRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    private final String propsFileName;
    private final Set<AbstractModule> modules = new LinkedHashSet<>();
    private final Set<ServerInterceptor> interceptors = new LinkedHashSet<>();
    private final Set<ServerLifecycleListener> listeners = new LinkedHashSet<>();
    private final Holder<Stage> stageHolder = new Holder<>(Stage.PRODUCTION);
    private final Map<String, Object> global = new ConcurrentHashMap<>();

    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private FalseWorkApplicationBuilder(String propsFileName) {
        this.propsFileName = propsFileName;
    }

    public static FalseWorkApplicationBuilder newBuilder() throws IOException {
        LOGGER.info("**********************************************************************************");
        LOGGER.info("************************FalseWorkApplication config.....*************************");
        return new FalseWorkApplicationBuilder("bootstrap.properties");
    }

    public static FalseWorkApplicationBuilder newBuilder(String path) throws IOException {
        LOGGER.info("**********************************************************************************");
        LOGGER.info("************************FalseWorkApplication config.....*************************");
        return new FalseWorkApplicationBuilder(path);
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


    @Override
    public FalseWorkApplication build() {
        Props props = PropsManager.initConfig(this.propsFileName);
        //使用负载均衡策略
        LoadBalancerProviderManager.set(LoadBalancerRegistry.getDefaultRegistry().getProvider("round_robin"));
        //使用共享线程池
        SharedExecutorManager.setShared(null);
        //命名解析
        CompositeResolverFactoryManager.addFactory(HttpResolverProvider.SINGLTON);


        this.modules.add(new FixInstanceModule<>(Map.class, this.global));
        this.modules.add(new FixInstanceModule<>(Props.class, props));

        Injector injector = Guice.createInjector(this.stageHolder.get(), this.modules);

        //server 相关
        String serverName = props.getProperty(PropsVars.SERVER_NAME);
        String serverIp = props.getProperty(PropsVars.SERVER_IP);
        int serverPort = props.getInt(PropsVars.SERVER_PORT);
        LOGGER.info("building service:{}[{}:{}]", serverName, serverIp, serverPort);

        SystemUtil.validate(serverIp, serverPort);

        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(serverIp, serverPort));

        //注册服务
        injector.getAllBindings().entrySet().stream()
                .filter(e -> BindableService.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (BindableService) e.getValue().getProvider().get()).forEach(builder::addService);

        injector.getAllBindings().entrySet().stream()
                .filter(e -> ServerServiceDefinition.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (ServerServiceDefinition) e.getValue().getProvider().get()).forEach(builder::addService);

        int threadServerNumber = props.getInt(PropsVars.SERVER_THREAD_POOL_SIZE, NettyRuntime.availableProcessors() * 8);
        builder.executor(Executors.newFixedThreadPool(threadServerNumber, new ThreadFactoryBuilder()
                .setNameFormat("server-executor-%d").build()));

        LOGGER.info("server core thread:{}", threadServerNumber);


        builder.intercept(TimedInterceptor.getInstance());

        for (ServerInterceptor interceptor : this.interceptors) {
            builder.intercept(interceptor);
        }

        builder.fallbackHandlerRegistry(new FallbackHandlerRegistry());
        Server server = builder.build();

        // 启动listener
        injector.getAllBindings().entrySet().stream()
                .filter(e -> ServerLifecycleListener.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (ServerLifecycleListener) e.getValue().getProvider().get()).forEach(this.listeners::add);

        LifecycleServer lifecycleServer = new LifecycleServer(server);
        for (ServerLifecycleListener listener : this.listeners) {
            lifecycleServer.addLifecycleListener(listener);
        }

        InternalNettyServerBuilder.setStatsRecordStartedRpcs(builder, false);
        return new FalseWorkApplication(lifecycleServer, injector, ServerRegister.NO_OP);
    }
}
