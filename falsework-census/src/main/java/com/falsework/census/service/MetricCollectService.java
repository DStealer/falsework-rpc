package com.falsework.census.service;

import com.falsework.census.composite.ErrorCode;
import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.composite.ResponseMetaException;
import com.falsework.core.generated.metric.FalseWorkMetricRequest;
import com.falsework.core.generated.metric.FalseWorkMetricResponse;
import com.falsework.core.generated.metric.FalseWorkMetricServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * 统计数据采集服务
 */
public class MetricCollectService extends FalseWorkMetricServiceGrpc.FalseWorkMetricServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricCollectService.class);
    private final DSLContext context;

    @Inject
    public MetricCollectService(DSLContext context) {
        this.context = context;
    }

    @Override
    public void collect(FalseWorkMetricRequest request, StreamObserver<FalseWorkMetricResponse> responseObserver) {
        FalseWorkMetricResponse.Builder builder = FalseWorkMetricResponse.newBuilder();
        try {
            if (!request.hasMeta()) {
                throw ErrorCode.INVALID_ARGUMENT.asException("meta invalid");
            }
            LOGGER.info("Receive metric from {}[{}:{}]", request.getServiceName(), request.getIp(), request.getPort());
            this.context.execute("REPLACE INTO metrics_views (metric_id, service_name, stats, TIMESTAMP) VALUES (?, ?, ?, ?)",
                    String.format("%s:%d", request.getIp(), request.getPort()), request.getServiceName(), request.getStats(), System.currentTimeMillis());
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_RESPONSE_META);
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
