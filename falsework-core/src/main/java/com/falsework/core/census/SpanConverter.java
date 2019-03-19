package com.falsework.core.census;

import com.falsework.core.generated.tracing.FalseWorkAnnotation;
import com.falsework.core.generated.tracing.FalseWorkEndpoint;
import com.falsework.core.generated.tracing.FalseWorkSpan;
import io.opencensus.common.Function;
import io.opencensus.common.Functions;
import io.opencensus.common.Timestamp;
import io.opencensus.trace.*;
import io.opencensus.trace.export.SpanData;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * span 数据转换
 */
public abstract class SpanConverter {

    private static final String STATUS_CODE = "census.status_code";
    private static final String STATUS_DESCRIPTION = "census.status_description";
    private static final Function<Object, String> returnToString = Functions.returnToString();

    /**
     * span data to zipkin span
     *
     * @param spanData
     * @param localEndpoint
     * @return
     */
    public static FalseWorkSpan spanDataToFalseWorkSpan(SpanData spanData, FalseWorkEndpoint localEndpoint) {
        SpanContext context = spanData.getContext();
        long startTimestamp = toEpochMicros(spanData.getStartTimestamp());

        long endTimestamp = toEpochMicros(spanData.getEndTimestamp());

        FalseWorkSpan.Builder spanBuilder =
                FalseWorkSpan.newBuilder()
                        .setTraceId(context.getTraceId().toLowerBase16())
                        .setId(context.getSpanId().toLowerBase16())
                        .setKind(toSpanKind(spanData))
                        .setName(spanData.getName())
                        .setTimestamp(toEpochMicros(spanData.getStartTimestamp()))
                        .setDuration(endTimestamp - startTimestamp)
                        .setLocalEndpoint(localEndpoint);

        if (spanData.getParentSpanId() != null && spanData.getParentSpanId().isValid()) {
            spanBuilder.setParentId(spanData.getParentSpanId().toLowerBase16());
        }

        for (Map.Entry<String, AttributeValue> label : spanData.getAttributes().getAttributeMap().entrySet()) {
            spanBuilder.putTags(label.getKey(), attributeValueToString(label.getValue()));
        }
        Status status = spanData.getStatus();
        if (status != null) {
            spanBuilder.putTags(STATUS_CODE, status.getCanonicalCode().toString());
            if (status.getDescription() != null) {
                spanBuilder.putTags(STATUS_DESCRIPTION, status.getDescription());
            }
        }

        for (SpanData.TimedEvent<Annotation> annotation : spanData.getAnnotations().getEvents()) {
            spanBuilder.addAnnotations(FalseWorkAnnotation.newBuilder()
                    .setTimestamp(toEpochMicros(annotation.getTimestamp()))
                    .setValue(annotation.getEvent().getDescription())
                    .build());
        }

        for (SpanData.TimedEvent<MessageEvent> messageEvent :
                spanData.getMessageEvents().getEvents()) {
            spanBuilder.addAnnotations(FalseWorkAnnotation.newBuilder()
                    .setTimestamp(toEpochMicros(messageEvent.getTimestamp()))
                    .setValue(messageEvent.getEvent().getType().name())
                    .build());
        }

        return spanBuilder.build();
    }

    /**
     * 转换span kind
     *
     * @param spanData
     * @return
     */
    private static FalseWorkSpan.Kind toSpanKind(SpanData spanData) {
        if (spanData.getKind() == io.opencensus.trace.Span.Kind.SERVER
                || (spanData.getKind() == null && Boolean.TRUE.equals(spanData.getHasRemoteParent()))) {
            return FalseWorkSpan.Kind.SERVER;
        }
        if (spanData.getKind() == io.opencensus.trace.Span.Kind.CLIENT || spanData.getName().startsWith("Sent.")) {
            return FalseWorkSpan.Kind.CLIENT;
        }
        return null;
    }

    /**
     * 转换时间戳
     *
     * @param timestamp
     * @return
     */
    private static long toEpochMicros(Timestamp timestamp) {
        return TimeUnit.SECONDS.toMicros(timestamp.getSeconds()) + TimeUnit.NANOSECONDS.toMicros(timestamp.getNanos());
    }


    /**
     * 转换属性
     *
     * @param attributeValue
     * @return
     */
    private static String attributeValueToString(AttributeValue attributeValue) {
        return attributeValue.match(
                returnToString,
                returnToString,
                returnToString,
                returnToString,
                Functions.returnConstant(""));
    }
}
