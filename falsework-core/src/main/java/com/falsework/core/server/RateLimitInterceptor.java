package com.falsework.core.server;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class RateLimitInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };
    private static final Metadata EMPTY_META_DATA = new Metadata();
    private static final ConcurrentHashMap<String, RateLimiter> RATE_LIMITER_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private RateLimitInterceptor() {

    }

    public static RateLimitInterceptor getInstance() {
        return new RateLimitInterceptor();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();

        call.close(Status.ABORTED.withDescription("Rate limit exceeded"), EMPTY_META_DATA);
        return NOOP_LISTENER;
    }
}
