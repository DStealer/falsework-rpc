package com.falsework.core.governance;

import com.falsework.core.generated.common.RequestMeta;
import com.falsework.governance.generated.LookupRequest;
import com.falsework.governance.generated.LookupResponse;
import com.falsework.governance.generated.LookupServiceGrpc;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务监听处理
 */
public class ResolverGovernor implements EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverGovernor.class);
    private final ConcurrentHashMap<URI, NameResolver.Listener> dependencies = new ConcurrentHashMap<>();
    private final LookupServiceGrpc.LookupServiceStub stub;

    public ResolverGovernor(LookupServiceGrpc.LookupServiceStub stub) {
        Preconditions.checkNotNull(stub);
        this.stub = stub;
    }

    /**
     * 注册
     *
     * @param uri
     * @param listener
     */
    public void register(URI uri, NameResolver.Listener listener) {
        if (this.dependencies.containsKey(uri)) {
            throw new UnsupportedOperationException("duplicates key...");
        }
        this.dependencies.putIfAbsent(uri, listener);
    }

    /**
     * 注销
     *
     * @param uri
     */
    public void deregister(URI uri) {
        this.dependencies.remove(uri);
    }

    /**
     * 事件发生
     *
     * @param object
     */
    public void onEvent(EventObject object) {

    }

    /**
     * 更新在此注册 listener
     *
     * @param uri
     */
    public void refresh(URI uri) {
        LOGGER.debug("refresh for:{}", uri);
        NameResolver.Listener listener = this.dependencies.get(uri);
        LookupRequest request = LookupRequest.newBuilder()
                .setMeta(RequestMeta.getDefaultInstance())
                .setServiceName(uri.getAuthority())
                .build();
        this.stub.lookup(request, new StreamObserver<LookupResponse>() {
            @Override
            public void onNext(LookupResponse lookupResponse) {
                if (lookupResponse.getServiceInfoListList().size() > 0) {
                    List<EquivalentAddressGroup> groups = lookupResponse.getServiceInfoListList()
                            .stream()
                            .map(e -> {
                                Attributes.Builder builder = Attributes.newBuilder();
                                for (Map.Entry<String, String> me : e.getAttributesMap().entrySet()) {
                                    builder.set(Attributes.Key.create(me.getKey()), me.getValue());
                                }
                                return new EquivalentAddressGroup(Lists.newArrayList(new InetSocketAddress(e.getHostname(), e.getPort())),
                                        builder.build());
                            }).collect(Collectors.toList());
                    listener.onAddresses(groups, Attributes.EMPTY);
                } else {
                    LOGGER.warn("refresh service with 0 instance:{}", uri);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("can't refresh dependencies:{}", uri, throwable);
                listener.onError(Status.UNKNOWN.withCause(throwable));
            }

            @Override
            public void onCompleted() {
                LOGGER.info("refresh completed!");
            }
        });
    }
}
