package com.falsework.core;

import com.falsework.core.common.Builder;
import com.falsework.core.common.Holder;
import com.falsework.core.common.MoreExecuters;
import com.falsework.core.composite.FixInstanceModule;
import com.falsework.core.composite.SystemUtil;
import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;
import com.falsework.core.config.PropsVars;
import com.falsework.core.governance.DiscoveryClient;
import com.falsework.core.governance.DiscoveryLifeListener;
import com.falsework.core.governance.DiscoveryNameResolverProvider;
import com.falsework.core.grpc.ChannelConfigurer;
import com.falsework.core.grpc.ChannelConfigurerManager;
import com.falsework.core.grpc.HttpResolverProvider;
import com.falsework.core.server.FallbackHandlerRegistry;
import com.falsework.core.server.LifecycleServer;
import com.falsework.core.server.ServerListener;
import com.falsework.core.server.TimedInterceptor;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 应用构建器
 */
public class FalseWorkApplicationBuilder implements Builder<FalseWorkApplication> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FalseWorkApplicationBuilder.class);
    private final String propsFileName;
    private final Set<AbstractModule> modules = new LinkedHashSet<>();
    private final Set<ServerInterceptor> interceptors = new LinkedHashSet<>();
    private final Set<ServerListener> listeners = new LinkedHashSet<>();
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

    public FalseWorkApplicationBuilder withListener(ServerListener listener) {
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

    @Override
    public FalseWorkApplication build() {
        Props props = PropsManager.initConfig(this.propsFileName);

        ChannelConfigurer configurer = ChannelConfigurerManager.getConfigurer();
        configurer.setLoadBalancerPolicy("round_robin");

        int threadChannelNumber = props.getInt(PropsVars.CHANNEL_THREAD_POOL_SIZE, NettyRuntime.availableProcessors() * 2);
        //使用共享线程池
        Executor channelExecutor = MoreExecuters.newStretchThreadPool(threadChannelNumber, new ThreadFactoryBuilder()
                .setNameFormat("channel-executor-%d").build());
        configurer.setDefaultChannelExecutor(channelExecutor);
        LOGGER.info("channel thread pool size:{}", threadChannelNumber);

        configurer.addResolverFactory(HttpResolverProvider.SINGLTON);
        boolean discovery = props.existSubProps(PropsVars.DISCOVERY_PREFIX);
        if (discovery) {
            DiscoveryClient client = new DiscoveryClient(props);
            if (client.isFetchRegistryEnable()) {
                configurer.addResolverFactory(new DiscoveryNameResolverProvider(client));
            }
            this.listeners.add(new DiscoveryLifeListener(client));
        }

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

        int threadServerNumber = props.getInt(PropsVars.SERVER_THREAD_POOL_SIZE, NettyRuntime.availableProcessors() * 4);
        builder.executor(MoreExecuters.newStretchThreadPool(threadServerNumber, new ThreadFactoryBuilder()
                .setNameFormat("server-executor-%d").build()));
        LOGGER.info("server thread pool size:{}", threadServerNumber);

        builder.intercept(TimedInterceptor.getInstance());

        for (ServerInterceptor interceptor : this.interceptors) {
            builder.intercept(interceptor);
        }

        builder.fallbackHandlerRegistry(new FallbackHandlerRegistry());
        Server server = builder.build();

        // 启动listener
        injector.getAllBindings().entrySet().stream()
                .filter(e -> ServerListener.class.isAssignableFrom(e.getKey().getTypeLiteral().getRawType()))
                .map(e -> (ServerListener) e.getValue().getProvider().get()).forEach(this.listeners::add);

        LifecycleServer lifecycleServer = new LifecycleServer(server);
        for (ServerListener listener : this.listeners) {
            lifecycleServer.addListener(listener);
        }

        InternalNettyServerBuilder.setStatsRecordStartedRpcs(builder, false);
        return new FalseWorkApplication(lifecycleServer, injector);
    }

    public void runAsync() throws Exception {
        build().run().async();
    }
}
