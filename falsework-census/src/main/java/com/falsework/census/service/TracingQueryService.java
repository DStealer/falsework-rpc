package com.falsework.census.service;

import com.falsework.census.composite.ErrorCode;
import com.falsework.census.generated.grpc.*;
import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.composite.ResponseMetaException;
import com.google.common.base.Strings;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.*;
import zipkin2.storage.QueryRequest;
import zipkin2.storage.mysql.v1.MySQLStorage;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TracingQueryService extends TracingQueryServiceGrpc.TracingQueryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingQueryService.class);
    private final MySQLStorage storage;

    @Inject
    public TracingQueryService(MySQLStorage storage) {
        this.storage = storage;
    }

    @Override
    public void dependencies(DependenciesRequest request, StreamObserver<DependenciesReply> responseObserver) {

        Call<List<DependencyLink>> call =
                this.storage.spanStore().getDependencies(request.getEndTs(), Math.max(request.getLookback(), 86400000L));
        DependenciesReply.Builder builder = DependenciesReply.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllDependencies(call.execute()
                            .stream()
                            .map(d -> CensusDependencyLink.newBuilder()
                                    .setParent(d.parent())
                                    .setChild(d.child())
                                    .setCallCount(d.callCount())
                                    .setErrorCount(d.errorCount())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void services(ServicesRequest request, StreamObserver<ServicesReply> responseObserver) {
        ServicesReply.Builder builder = ServicesReply.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            List<String> serviceNames = storage.spanStore().getServiceNames().execute();
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllServices(serviceNames).build();

        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void spanNames(SpanNamesRequest request, StreamObserver<SpanNamesReply> responseObserver) {
        SpanNamesReply.Builder builder = SpanNamesReply.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            List<String> spanNames = storage.spanStore().getSpanNames(request.getServiceName()).execute();
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllSpans(spanNames).build();
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void traces(TracesRequest request, StreamObserver<TracesReply> responseObserver) {
        TracesReply.Builder builder = TracesReply.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            QueryRequest queryRequest =
                    QueryRequest.newBuilder()
                            .serviceName(request.getServiceName())
                            .spanName(request.getSpanName())
                            .parseAnnotationQuery(request.getAnnotationQuery())
                            .minDuration(request.getMinDuration())
                            .maxDuration(request.getMaxDuration())
                            .endTs(Math.max(request.getEndTs(), System.currentTimeMillis()))
                            .lookback(Math.max(request.getLookback(), 86400000L))
                            .limit(request.getLimit())
                            .build();
            List<List<Span>> traces = storage.spanStore().getTraces(queryRequest).execute();
            List<TracesReply.CensusSpanList> spanLists = traces.stream().map(t -> TracesReply.CensusSpanList
                    .newBuilder()
                    .addAllSpans(t.stream()
                            .map(this::convert)
                            .collect(Collectors.toList()))
                    .build())
                    .collect(Collectors.toList());
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllSpanList(spanLists);
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void trace(TraceRequest request, StreamObserver<TraceReply> responseObserver) {
        TraceReply.Builder builder = TraceReply.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            List<Span> trace = storage.spanStore().getTrace(request.getTraceIdHex()).execute();
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllSpans(trace.stream()
                            .map(this::convert).collect(Collectors.toList())).build();
        } catch (ResponseMetaException e) {
            LOGGER.error(e.getErrorCode(), e);
            builder.setMeta(e.toResponseMeta());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            builder.setMeta(ErrorCode.INTERNAL.toResponseMeta());
        } finally {
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 转换span
     *
     * @param span
     * @return
     */
    private CensusSpan convert(Span span) {
        return CensusSpan.newBuilder()
                .setTraceId(span.id())
                .setParentId(span.parentId())
                .setId(span.id())
                .setKind(convert(span.kind()))
                .setName(span.name())
                .setTimestamp(span.timestampAsLong())
                .setDuration(span.durationAsLong())
                .setLocalEndpoint(convert(span.localEndpoint()))
                .setRemoteEndpoint(convert(span.remoteEndpoint()))
                .addAllAnnotations(span.annotations().stream().map(this::convert).collect(Collectors.toList()))
                .putAllTags(span.tags())
                .setDebug(span.debug())
                .setShared(span.shared())
                .build();
    }

    /**
     * 注释转换
     *
     * @param annotation
     * @return
     */
    private CensusAnnotation convert(Annotation annotation) {
        return CensusAnnotation.newBuilder()
                .setTimestamp(annotation.timestamp())
                .setValue(annotation.value())
                .build();
    }

    /**
     * endpoint 转换
     *
     * @param endpoint
     * @return
     */
    private CensusEndpoint convert(Endpoint endpoint) {
        return CensusEndpoint.newBuilder()
                .setServiceName(endpoint.serviceName())
                .setIp(Strings.isNullOrEmpty(endpoint.ipv4()) ? endpoint.ipv6() : endpoint.ipv4())
                .setPort(endpoint.portAsInt())
                .build();
    }

    /**
     * kind转换
     *
     * @param kind
     * @return
     */
    private CensusSpan.Kind convert(Span.Kind kind) {
        switch (kind) {
            case CONSUMER:
                return CensusSpan.Kind.CONSUMER;
            case PRODUCER:
                return CensusSpan.Kind.PRODUCER;
            case SERVER:
                return CensusSpan.Kind.SERVER;
            case CLIENT:
                return CensusSpan.Kind.CLIENT;
            default:
                return CensusSpan.Kind.UNRECOGNIZED;
        }
    }
}
