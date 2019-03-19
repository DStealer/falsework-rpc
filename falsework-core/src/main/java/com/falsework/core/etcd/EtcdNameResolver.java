package com.falsework.core.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import com.falsework.core.common.PPrints;
import com.falsework.core.generated.etcd.ServiceDefinition;
import com.falsework.core.generated.etcd.ServiceInformation;
import com.google.common.collect.Lists;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EtcdNameResolver extends NameResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdNameResolver.class);
    private final Client client;
    private final URI targetUri;
    private final ConcurrentHashMap<ServiceDefinition, ServiceInformation> lastAvailableService = new ConcurrentHashMap<>();
    private final Attributes params;
    private ExecutorService executor;

    public EtcdNameResolver(Client client, URI targetUri, Attributes params) {
        this.executor = Executors.newSingleThreadExecutor(
                GrpcUtil.getThreadFactory("Name-resolver-" + targetUri, true));
        this.params = params;
        this.client = client;
        this.targetUri = targetUri;
    }

    @Override
    public String getServiceAuthority() {
        return targetUri.getAuthority();
    }

    @Override
    public synchronized void start(Listener listener) {
        LOGGER.info("NameResolver for:{} staring", targetUri);
        try {
            ByteSequence serviceToLocate = ByteSequence.fromByteString(ServiceDefinition.newBuilder()
                    .setScheme(targetUri.getScheme()).setName(targetUri.getHost()).build().toByteString());
            GetResponse response = this.client.getKVClient().get(serviceToLocate,
                    GetOption.newBuilder().withPrefix(serviceToLocate).build()).get();
            for (KeyValue kv : response.getKvs()) {
                ServiceDefinition serviceDefinition = ServiceDefinition.parseFrom(kv.getKey().getByteString());
                ServiceInformation serviceInformation = ServiceInformation.parseFrom(kv.getValue().getByteString());
                lastAvailableService.putIfAbsent(serviceDefinition, serviceInformation);
            }
            this.onUpdate(listener, this.lastAvailableService, this.params);
            Watch.Watcher watch = this.client.getWatchClient().watch(serviceToLocate,
                    WatchOption.newBuilder().withPrefix(serviceToLocate).build());
            this.executor.submit(() -> {
                for (; ; ) {
                    try {
                        WatchResponse listen = watch.listen();
                        for (WatchEvent event : listen.getEvents()) {
                            switch (event.getEventType()) {
                                case PUT: {
                                    ServiceDefinition serviceDefinition = ServiceDefinition.parseFrom(event.getKeyValue().getKey().getByteString());
                                    LOGGER.info("service:{} online.", PPrints.toString(serviceDefinition));
                                    ServiceInformation serviceInformation = ServiceInformation.parseFrom(event.getKeyValue().getValue().getByteString());
                                    lastAvailableService.putIfAbsent(serviceDefinition, serviceInformation);
                                }
                                break;
                                case DELETE: {
                                    ServiceDefinition serviceDefinition = ServiceDefinition.parseFrom(event.getKeyValue().getKey().getByteString());
                                    LOGGER.info("service:{} offline", PPrints.toString(serviceDefinition));
                                    lastAvailableService.remove(serviceDefinition);
                                }
                                break;
                                default: {
                                    LOGGER.warn("unrecognized event:{}", event);
                                }
                            }
                            this.onUpdate(listener, this.lastAvailableService, this.params);
                        }
                    } catch (Exception e) {
                        LOGGER.error("etcd name watch error", e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 更新任务节点列表
     *
     * @param listener
     * @param service
     * @param param
     */
    private synchronized void onUpdate(Listener listener, ConcurrentHashMap<ServiceDefinition, ServiceInformation> service, Attributes param) {
        if (!service.isEmpty()) {
            listener.onAddresses(service.entrySet().stream()
                    .map(e -> {
                        Attributes.Builder builder = Attributes.newBuilder();
                        for (Map.Entry<String, String> me : e.getValue().getAttributesMap().entrySet()) {
                            builder.set(Attributes.Key.create(me.getKey()), me.getValue());
                        }
                        return new EquivalentAddressGroup(
                                Lists.newArrayList(new InetSocketAddress(e.getKey().getHost(), e.getKey().getPort())),
                                builder.build());
                    })
                    .collect(Collectors.toList()), param);
        } else {
            listener.onError(Status.Code.UNAVAILABLE.toStatus());
        }
    }

    @Override
    public synchronized void shutdown() {
        LOGGER.info("NameResolver for:{} stop", targetUri);
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }
}
