package com.falsework.core.census;

import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.composite.NoopStreamObserver;
import com.falsework.core.generated.tracing.FalseWorkEndpoint;
import com.falsework.core.generated.tracing.FalseWorkTracingRequest;
import com.falsework.core.generated.tracing.FalseWorkTracingServiceGrpc;
import com.google.common.base.Preconditions;
import io.opencensus.trace.export.SpanData;
import io.opencensus.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class TracingExporterHandler extends SpanExporter.Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingExporterHandler.class);

    private final FalseWorkTracingServiceGrpc.FalseWorkTracingServiceStub serviceStub;
    private final FalseWorkEndpoint localEndpoint;
    private final String serviceName;
    private final String ip;
    private final int port;

    public TracingExporterHandler(FalseWorkTracingServiceGrpc.FalseWorkTracingServiceStub serviceStub, String serviceName, String ip, int port) {
        Preconditions.checkNotNull(serviceName, "service name invalid");
        this.serviceStub = serviceStub;
        this.serviceName = serviceName;
        this.ip = ip;
        this.port = port;
        this.localEndpoint = FalseWorkEndpoint.newBuilder()
                .setServiceName(this.serviceName)
                .setIp(this.ip)
                .setPort(this.port).build();
    }

    @Override
    public void export(Collection<SpanData> spanDataList) {
        LOGGER.info("exporting span data...");
        FalseWorkTracingRequest.Builder builder = FalseWorkTracingRequest.newBuilder();
        builder.setMeta(FalseWorkMetaUtil.DEFAULT_REQUEST_META)
                .setServiceName(this.serviceName).setIp(this.ip).setPort(this.port);
        for (SpanData data : spanDataList) {
            builder.addSpans(SpanConverter.spanDataToFalseWorkSpan(data, localEndpoint));
        }
        serviceStub.collect(builder.build(), new NoopStreamObserver<>(LOGGER));
    }
}
