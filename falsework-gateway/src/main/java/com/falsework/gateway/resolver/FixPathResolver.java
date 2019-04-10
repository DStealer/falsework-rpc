package com.falsework.gateway.resolver;

import com.falsework.core.config.Props;
import com.falsework.gateway.config.PropsVars;
import com.falsework.gateway.module.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FixPathResolver implements PathResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixPathResolver.class);
    private final Map<String, ServiceInfo> groupAndServiceMap;

    public FixPathResolver(Props props) {
        Map<String, ServiceInfo> hashMap = new HashMap<>();
        Map<String, Props> routeProps = props.subNamedProps(PropsVars.GATEWAY_ROUTES_PREFIX);
        for (Map.Entry<String, Props> entry : routeProps.entrySet()) {
            hashMap.put(entry.getValue().getProperty("path"),
                    new ServiceInfo(entry.getValue().getProperty("group"),
                            entry.getValue().getProperty("service")));
        }
        this.groupAndServiceMap = Collections.unmodifiableMap(hashMap);

    }

    @Override
    public Optional<ServiceInfo> resolve(String path) {
        for (Map.Entry<String, ServiceInfo> entry : this.groupAndServiceMap.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        LOGGER.warn("can't resolve path:{}", path);
        return Optional.empty();
    }
    public Collection<ServiceInfo> getServiceInfo(){
        return this.groupAndServiceMap.values();
    }

}
