package com.falsework.core.census;

import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.core.datasource.OpenCensusMetricsTracker;
import com.falsework.core.generated.metric.FalseWorkMetricServiceGrpc;
import com.falsework.core.generated.tracing.FalseWorkTracingServiceGrpc;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;

public class CensusStubModule extends EnvAwareModule {
    @Override
    protected void configure() {
        try {
            //建立采集数据客户端
            ChannelManager manager = ChannelManagerBuilder.newBuilder()
                    .name("etcd://census-v1")
                    .build();
            manager.start();
            //采集参数配置
            TraceConfig traceConfig = Tracing.getTraceConfig();
            traceConfig.updateActiveTraceParams(TraceParams.DEFAULT);
            //注册视图
            RpcViews.registerAllGrpcViews();

            OpenCensusMetricsTracker.registerViews();

            //发送采集数据
            String serverName = getProps().getProperty("server.name");
            String ip = getProps().getProperty("server.ip");
            int port = getProps().getInt("server.port");
            FalseWorkTracingServiceGrpc.FalseWorkTracingServiceStub tracingServiceStub = manager.newStub(FalseWorkTracingServiceGrpc::newStub);
            TracingExporterHandler tracingExporterHandler = new TracingExporterHandler(tracingServiceStub, serverName, ip, port);
            Tracing.getExportComponent().getSpanExporter().registerHandler("TracingExporterHandler", tracingExporterHandler);

            FalseWorkMetricServiceGrpc.FalseWorkMetricServiceStub metricServiceStub = manager.newStub(FalseWorkMetricServiceGrpc::newStub);
            MetricExportHandler metricExportHandler = new MetricExportHandler(metricServiceStub, serverName, ip, port);
            metricExportHandler.schedule();
        } catch (Exception e) {
            addError(e);
        }
    }
}
