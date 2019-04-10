package com.falsework.gateway.server;

import com.falsework.core.config.Props;
import com.falsework.gateway.config.PropsVars;
import com.falsework.gateway.discovery.GateWayDiscoveryClient;
import com.falsework.gateway.h2c.H2cFrontendHandler;
import com.falsework.gateway.h2c.LoggingHandler;
import com.falsework.gateway.h2c.RouteDetectHandler;
import com.falsework.gateway.module.ServiceInfo;
import com.falsework.gateway.resolver.FixPathResolver;
import com.falsework.gateway.resolver.RandomServiceResolver;
import com.falsework.gateway.resolver.ServiceResolver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.NettyRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class H2cProxyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(H2cProxyServer.class);

    /**
     * 运行
     *
     * @param props
     * @throws Exception
     */
    public void runSync(Props props) throws Exception {
        GateWayDiscoveryClient client = new GateWayDiscoveryClient(props);
        FixPathResolver pathResolver = new FixPathResolver(props);
        for (ServiceInfo service : pathResolver.getServiceInfo()) {
            client.registerDependency(service.getGroup(), service.getService());
        }
        client.start();

        ServiceResolver serviceResolver = new RandomServiceResolver(client, pathResolver);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() * 2);
        try {
            String ip = props.getProperty(PropsVars.PROXY_IP);
            int port = props.getInt(PropsVars.PROXY_PORT);
            LOGGER.info("proxy listen:{}:{}", ip, port);
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LoggingHandler())
                                    .addLast(new IdleStateHandler(45, 30, 0, TimeUnit.SECONDS))
                                    .addLast(new RouteDetectHandler(serviceResolver))
                                    .addLast(new H2cFrontendHandler());
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(ip, port)
                    .channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}