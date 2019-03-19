package com.falsework.core.datasource;

import com.google.common.base.Preconditions;
import io.opencensus.trace.*;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCensusExecuteListener extends DefaultExecuteListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCensusExecuteListener.class);
    private static final Tracer TRACER = Tracing.getTracer();
    private static final ThreadLocal<Span> SPAN_THREAD_LOCAL = new InheritableThreadLocal<>();
    private final String sourceName;
    private final boolean isSampledToLocalTracing;

    OpenCensusExecuteListener(String sourceName, boolean isSampledToLocalTracing) {
        Preconditions.checkNotNull(sourceName, "invalid source name");
        this.sourceName = sourceName;
        this.isSampledToLocalTracing = isSampledToLocalTracing;
    }

    @Override
    public void renderEnd(ExecuteContext ctx) {
        Span parentSpan = TRACER.getCurrentSpan();
        if (parentSpan != null) {
            Span span = TRACER.spanBuilderWithExplicitParent("sql.execute", parentSpan)
                    .setSpanKind(Span.Kind.CLIENT)
                    .startSpan();
            SPAN_THREAD_LOCAL.set(span);
            span.addAnnotation("pool=" + this.sourceName);
        }
    }

    @Override
    public void end(ExecuteContext ctx) {
        Span span = SPAN_THREAD_LOCAL.get();
        if (span != null) {
            span.end(EndSpanOptions.builder()
                    .setStatus(Status.OK)
                    .setSampleToLocalSpanStore(this.isSampledToLocalTracing)
                    .build());
            SPAN_THREAD_LOCAL.remove();
        }
    }


    @Override
    public void exception(ExecuteContext ctx) {
        Span span = SPAN_THREAD_LOCAL.get();
        if (span != null) {
            span.end(EndSpanOptions.builder()
                    .setStatus(Status.INTERNAL.withDescription(ctx.sql()))
                    .setSampleToLocalSpanStore(this.isSampledToLocalTracing)
                    .build());
            SPAN_THREAD_LOCAL.remove();
        }
    }
}
