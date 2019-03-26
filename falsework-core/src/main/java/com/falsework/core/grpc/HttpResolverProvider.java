package com.falsework.core.grpc;

import com.google.common.collect.Lists;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
public class HttpResolverProvider extends NameResolverProvider {
    public static final HttpResolverProvider SINGLTON = new HttpResolverProvider();
    private static final String SCHEMA = "http";

    private HttpResolverProvider() {
    }

    /**
     * 解析地址 例如127.0.0.1:8080
     *
     * @param authority
     * @return
     */
    private static InetSocketAddress parse(String authority) {
        String[] strings = authority.split(":", 2);
        try {
            return new InetSocketAddress(strings[0], Integer.parseInt(strings[1]));
        } catch (NumberFormatException e) {
            return new InetSocketAddress(strings[0], 80);
        }
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (SCHEMA.equals(targetUri.getScheme())) {
            return new NameResolver() {
                @Override
                public String getServiceAuthority() {
                    return targetUri.getAuthority();
                }

                @Override
                public void start(Listener listener) {
                    String[] authorities = targetUri.getAuthority().split(";");
                    List<EquivalentAddressGroup> groups = new ArrayList<>(authorities.length);
                    for (String authority : authorities) {
                        groups.add(new EquivalentAddressGroup(
                                Lists.newArrayList(parse(authority))));
                    }
                    Collections.shuffle(groups);//打乱顺序，避免同时连接一个服务,grpc有选取第一个可用连接的负载策略
                    listener.onAddresses(groups, params);
                }

                @Override
                public void shutdown() {

                }
            };
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEMA;
    }
}
