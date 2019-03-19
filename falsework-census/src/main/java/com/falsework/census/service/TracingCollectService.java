package com.falsework.census.service;

import com.falsework.census.composite.ErrorCode;
import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.generated.tracing.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Callback;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.collector.Collector;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跟踪数据采集
 */
public class TracingCollectService extends FalseWorkTracingServiceGrpc.FalseWorkTracingServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingCollectService.class);
    private final Collector collector;

    @Inject
    public TracingCollectService(Collector collector) {
        this.collector = collector;
    }

    @Override
    public void collect(FalseWorkTracingRequest request, StreamObserver<FalseWorkTracingResponse> responseObserver) {
        LOGGER.info("Receive tracing from {}[{}:{}]", request.getServiceName(), request.getIp(), request.getPort());
        List<Span> spans = request.getSpansList().stream()
                .map(this::convert)
                .collect(Collectors.toList());
        if (!request.hasMeta()) {
            responseObserver.onNext(FalseWorkTracingResponse.newBuilder()
                    .setMeta(ErrorCode.INVALID_ARGUMENT.toResponseMeta("meta invalid")).build());
            responseObserver.onCompleted();
            return;
        }
        this.collector.accept(spans, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                responseObserver.onNext(FalseWorkTracingResponse.newBuilder()
                        .setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META).build());
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("request error", t);
                responseObserver.onNext(FalseWorkTracingResponse.newBuilder()
                        .setMeta(ErrorCode.INTERNAL.toResponseMeta()).build());
                responseObserver.onCompleted();
            }
        });

    }

    private Span convert(FalseWorkSpan span) {
        Span.Builder builder = Span.newBuilder();
        builder.traceId(span.getTraceId());
        if (span.hasField(FalseWorkSpan.getDescriptor().findFieldByNumber(FalseWorkSpan.PARENTID_FIELD_NUMBER))) {
            builder.parentId(span.getParentId());
        }
        builder.id(span.getId())
                .kind(this.convert(span.getKind()))
                .name(span.getName())
                .timestamp(span.getTimestamp())
                .duration(span.getDuration());
        if (span.hasLocalEndpoint()) {
            builder.localEndpoint(this.convert(span.getLocalEndpoint()));
        }
        if (span.hasRemoteEndpoint()) {
            builder.remoteEndpoint(this.convert(span.getRemoteEndpoint()));
        }
        for (FalseWorkAnnotation annotation : span.getAnnotationsList()) {
            builder.addAnnotation(annotation.getTimestamp(), annotation.getValue());
        }
        for (Map.Entry<String, String> entry : span.getTagsMap().entrySet()) {
            builder.putTag(entry.getKey(), entry.getValue());
        }
        builder.debug(span.getDebug());
        builder.shared(span.getShared());
        return builder.build();
    }

    private Span.Kind convert(FalseWorkSpan.Kind kind) {
        switch (kind) {
            case CLIENT:
                return Span.Kind.CLIENT;
            case SERVER:
                return Span.Kind.SERVER;
            case PRODUCER:
                return Span.Kind.PRODUCER;
            case CONSUMER:
                return Span.Kind.CONSUMER;
            default:
                return null;
        }
    }

    private Endpoint convert(FalseWorkEndpoint endpoint) {
        return Endpoint.newBuilder()
                .serviceName(endpoint.getServiceName())
                .port(endpoint.getPort())
                .ip(endpoint.getIp())
                .build();
    }
}
