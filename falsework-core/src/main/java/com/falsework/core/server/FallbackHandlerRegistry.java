package com.falsework.core.server;

import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class FallbackHandlerRegistry extends HandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackHandlerRegistry.class);

    @Override
    public List<ServerServiceDefinition> getServices() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public ServerMethodDefinition<?, ?> lookupMethod(
            String methodName, @Nullable String authority) {
        LOGGER.error("Can't find server method definition for:{} auth:{}", methodName, authority);
        return null;
    }

}
