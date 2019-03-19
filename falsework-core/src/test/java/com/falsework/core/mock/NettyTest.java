package com.falsework.core.mock;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import org.junit.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class NettyTest {

    @Test
    public void tt01() throws Exception {
        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
        KeyStore jks = KeyStore.getInstance("JKS");
        try (InputStream inputStream = NettyTest.class.getResourceAsStream("sChat.jks")) {
            jks.load(inputStream, "sNetty".toCharArray());
            factory.init(jks, "sNetty".toCharArray());
        }
        SSLContext instance = SSLContext.getInstance("SSL");
        instance.init(factory.getKeyManagers(), null, null);
        SSLEngine engine = instance.createSSLEngine();
        engine.setNeedClientAuth(false);

    }

    @Test
    public void tt02() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        KeyStore jks = KeyStore.getInstance("JKS");
        try (InputStream inputStream = NettyTest.class.getResourceAsStream("cChat.jks")) {
            jks.load(inputStream, "cNetty".toCharArray());
            factory.init(jks);
        }
        SSLContext instance = SSLContext.getInstance("SSL");
        instance.init(null, factory.getTrustManagers(), null);
        SSLEngine engine = instance.createSSLEngine();
        engine.setNeedClientAuth(true);
    }

    @Test
    public void tt03() throws Exception {
        //-Djavax.net.debug=ssl,handshake

        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
        KeyStore jks = KeyStore.getInstance("JKS");
        try (InputStream inputStream = NettyTest.class.getResourceAsStream("sChat.jks")) {
            jks.load(inputStream, "sNetty".toCharArray());
            factory.init(jks, "sNetty".toCharArray());
        }
        SSLContext instance = SSLContext.getInstance("TLSv1.2");
        instance.init(factory.getKeyManagers(), null, null);
        SSLEngine engine = instance.createSSLEngine();
        engine.setUseClientMode(false);

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addFirst("ssl", new SslHandler(engine))
                            .addLast();
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(8080).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @Test
    public void tt04() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        KeyStore jks = KeyStore.getInstance("JKS");
        try (InputStream inputStream = NettyTest.class.getResourceAsStream("cChat.jks")) {
            jks.load(inputStream, "cNetty".toCharArray());
            factory.init(jks);
        }
        SSLContext instance = SSLContext.getInstance("TLSv1.2");
        instance.init(null, factory.getTrustManagers(), null);
        SSLEngine engine = instance.createSSLEngine();
        engine.setUseClientMode(true);

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // (1)
            b.group(workerGroup); // (2)
            b.channel(NioSocketChannel.class); // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                            .addFirst("ssl", new SslHandler(engine));
                }
            });

            // Start the client.
            ChannelFuture f = b.connect("127.0.0.1", 8080).sync(); // (5)

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
