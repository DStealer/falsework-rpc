package com.falsework.core.census;

import com.falsework.core.composite.FalseWorkMetaUtil;
import com.falsework.core.composite.NoopStreamObserver;
import com.falsework.core.generated.metric.FalseWorkMetricRequest;
import com.falsework.core.generated.metric.FalseWorkMetricServiceGrpc;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opencensus.metrics.Metrics;
import io.opencensus.metrics.export.MetricProducerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class MetricExportHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(MetricExportHandler.class);
    private final String serviceName;
    private final String ip;
    private final int port;
    private final FalseWorkMetricServiceGrpc.FalseWorkMetricServiceStub serviceStub;
    private final ScheduledExecutorService service;
    private final MetricProducerManager metricProducerManager;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public MetricExportHandler(FalseWorkMetricServiceGrpc.FalseWorkMetricServiceStub serviceStub, String serviceName, String ip, int port) {
        this.serviceName = serviceName;
        this.ip = ip;
        this.port = port;
        this.serviceStub = serviceStub;
        this.metricProducerManager = Metrics.getExportComponent().getMetricProducerManager();
        this.service = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("ExportComponent.MetricExporterThread-%d").build());
    }

    /**
     * 开始在后台执行
     */
    public void schedule() {
        if (this.started.compareAndSet(false, true)) {
            LOGGER.info("Schedule export metric");
            this.service.scheduleAtFixedRate(MetricExportHandler.this::export, 10, 10, TimeUnit.SECONDS);
        } else {
            LOGGER.warn("Exporting metric has scheduled,ignore");
        }
    }

    /**
     * 导出数据
     */
    private void export() {
        try {
            LOGGER.info("exporting metric data...");
            FalseWorkMetricRequest.Builder builder = FalseWorkMetricRequest.newBuilder();
            builder.setMeta(FalseWorkMetaUtil.DEFAULT_REQUEST_META)
                    .setServiceName(this.serviceName).setIp(this.ip).setPort(this.port)
                    .setStats(MetricConverter.getJsonMetrics(this.metricProducerManager));
            this.serviceStub.collect(builder.build(), new NoopStreamObserver<>(LOGGER));
        } catch (Exception e) {
            LOGGER.error("export metric error", e);
        }
    }
}
