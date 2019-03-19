package com.falsework.core.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.ByteSequence;
import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.generated.etcd.ServiceDefinition;
import com.falsework.core.generated.etcd.ServiceInformation;
import com.falsework.core.grpc.CompositeResolverFactory;
import com.falsework.core.grpc.HttpResolverProvider;
import com.falsework.core.grpc.HttpsResolverProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.DnsNameResolverProvider;

import java.util.Objects;

public class EtcdModule extends EnvAwareModule {
    @Override
    protected void configure() {
        String serverName = getProps().getProperty("server.name");
        String serverIp = getProps().getProperty("server.ip");
        int serverPort = getProps().getInt("server.port");
        String serverAuthority = getProps().getProperty("server.authority");
        //etcd 相关配置
        String[] endpoints = getProps().getStringArray("etcd.endpoints");
        String etcdUser = getProps().getProperty("etcd.user");
        String etcdPassword = getProps().getProperty("etcd.password");
        String etcdAuthority = getProps().getProperty("etcd.authority");
        Client client = Client.builder()
                .endpoints(endpoints)
                .user(ByteSequence.fromString(etcdUser))
                .password(ByteSequence.fromString(etcdPassword))
                .authority(etcdAuthority)
                .loadBalancerFactory(Objects.requireNonNull(LoadBalancerRegistry
                        .getDefaultRegistry().getProvider("round_robin")))
                .build();
        EtcdServerRegister register = new EtcdServerRegister(client);
        register.withServiceDefinition(
                ServiceDefinition.newBuilder()
                        .setScheme("etcd")
                        .setName(serverName)
                        .setHost(serverIp)
                        .setPort(serverPort)
                        .build())
                .withInformationDefinition(
                        ServiceInformation.newBuilder()
                                .putAttributes("service.started.millis", String.valueOf(System.currentTimeMillis()))
                                .putAttributes("service.security.authority", serverAuthority)
                                .build());
        bind(EtcdServerRegister.class).toInstance(register);

        CompositeResolverFactory factory = new CompositeResolverFactory()
                .addFactory(new DnsNameResolverProvider())
                .addFactory(new HttpsResolverProvider())
                .addFactory(new HttpResolverProvider())
                .addFactory(new EtcdNameResolverProvider(client));
        
        bind(CompositeResolverFactory.class).toInstance(factory);
    }
}
