package com.falsework.census.service;

import com.falsework.census.composite.ErrorCode;
import com.falsework.census.generated.grpc.*;
import com.falsework.census.generated.jooq.Tables;
import com.falsework.census.generated.jooq.tables.records.MetricsViewsRecord;
import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.composite.ResponseMetaException;
import io.grpc.stub.StreamObserver;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Collectors;

public class MetricQueryService extends MetricQueryServiceGrpc.MetricQueryServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricQueryService.class);
    private final DSLContext context;

    @Inject
    public MetricQueryService(DSLContext context) {
        this.context = context;
    }

    @Override
    public void metrics(MetricsRequest request, StreamObserver<MetricsReplay> responseObserver) {
        MetricsReplay.Builder builder = MetricsReplay.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            Result<Record2<String, String>> metrics = this.context.selectDistinct(Tables.METRICS_VIEWS.SERVICE_NAME, Tables.METRICS_VIEWS.METRIC_ID)
                    .from(Tables.METRICS_VIEWS).fetch();
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                    .addAllInfoList(metrics.stream()
                            .map(m -> MetricsReplay.MetricInfo.newBuilder()
                                    .setServiceName(m.value1())
                                    .setMetricId(m.value2()).build())
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
    public void metric(MetricRequest request, StreamObserver<MetricReplay> responseObserver) {
        MetricReplay.Builder builder = MetricReplay.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            Optional<MetricsViewsRecord> record = this.context.fetchOptional(Tables.METRICS_VIEWS, Tables.METRICS_VIEWS.METRIC_ID.eq(request.getMetricId()));
            if (record.isPresent()) {
                builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META)
                        .addMetrics(CensusMetric.newBuilder()
                                .setMetricId(record.get().getMetricId())
                                .setServiceName(record.get().getServiceName())
                                .setStats(String.valueOf(record.get().getStats()))
                                .setTimestamp(record.get().getTimestamp())
                                .build());
            } else {
                builder.setMeta(ErrorCode.NOT_FOUND.toResponseMeta());
            }
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
}
