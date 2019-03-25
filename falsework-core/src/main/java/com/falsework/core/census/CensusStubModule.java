package com.falsework.core.census;

import com.falsework.core.client.ChannelManager;
import com.falsework.core.client.ChannelManagerBuilder;
import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;
import com.falsework.core.config.PropsVars;
import com.falsework.core.datasource.OpenCensusMetricsTracker;
import com.falsework.core.generated.metric.FalseWorkMetricServiceGrpc;
import com.falsework.core.generated.tracing.FalseWorkTracingServiceGrpc;
import com.google.inject.AbstractModule;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.config.TraceParams;

public class CensusStubModule extends AbstractModule {
    @Override
    protected void configure() {
        try {
            Props props = PropsManager.getProps();
            //建立采集数据客户端
            ChannelManager manager = ChannelManagerBuilder.newBuilder()
                    .name(props.getProperty(PropsVars.CENSUS_ADDRESS))
                    .build();
            manager.start();
            //采集参数配置
            TraceConfig traceConfig = Tracing.getTraceConfig();
            traceConfig.updateActiveTraceParams(TraceParams.DEFAULT);
            //注册视图
            RpcViews.registerAllGrpcViews();

            OpenCensusMetricsTracker.registerViews();
            //发送采集数据
            String name = props.getProperty(PropsVars.SERVER_NAME);
            String ip = props.getProperty(PropsVars.SERVER_IP);
            int port = props.getInt(PropsVars.SERVER_PORT);

            FalseWorkTracingServiceGrpc.FalseWorkTracingServiceStub tracingServiceStub = manager.newStub(FalseWorkTracingServiceGrpc::newStub);
            TracingExporterHandler tracingExporterHandler = new TracingExporterHandler(tracingServiceStub, name, ip, port);
            Tracing.getExportComponent().getSpanExporter().registerHandler("TracingExporterHandler", tracingExporterHandler);

            FalseWorkMetricServiceGrpc.FalseWorkMetricServiceStub metricServiceStub = manager.newStub(FalseWorkMetricServiceGrpc::newStub);
            MetricExportHandler metricExportHandler = new MetricExportHandler(metricServiceStub, name, ip, port);
            metricExportHandler.schedule();
        } catch (Exception e) {
            addError(e);
        }
    }
}
