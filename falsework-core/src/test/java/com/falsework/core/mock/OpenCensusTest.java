package com.falsework.core.mock;

import com.google.gson.Gson;
import io.opencensus.common.Scope;
import io.opencensus.metrics.Metrics;
import io.opencensus.metrics.export.*;
import io.opencensus.stats.*;
import io.opencensus.tags.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OpenCensusTest {
    private static final Measure.MeasureDouble M_LATENCY_MS = Measure.MeasureDouble.create("repl/latency", "The latency in milliseconds per REPL loop", "ms");
    private static final TagKey KEY_METHOD = TagKey.create("method");
    private static final Tagger tagger = Tags.getTagger();
    private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();
    private static final Gson gson = new Gson();

    @Test
    public void tt01() throws IOException, InterruptedException {
        Aggregation latencyDistribution = Aggregation.Distribution.create(BucketBoundaries.create(
                Arrays.asList(
                        0.0, 25.0, 50.0, 75.0, 100.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 2000.0, 4000.0, 6000.0)
        ));

        Aggregation.Count count = Aggregation.Count.create();

        Aggregation.Sum sum = Aggregation.Sum.create();

        Aggregation.LastValue lastValue = Aggregation.LastValue.create();


        View view_1 = View.create(View.Name.create("ocjavametrics/latency"), "The distribution of latencies", M_LATENCY_MS,
                latencyDistribution, Collections.unmodifiableList(Collections.singletonList(KEY_METHOD)));

        View view_2 = View.create(View.Name.create("ocjavametrics/count"), "count", M_LATENCY_MS, count,
                Collections.unmodifiableList(Collections.singletonList(KEY_METHOD)));

        View view_3 = View.create(View.Name.create("ocjavametrics/sum"), "sum", M_LATENCY_MS, sum,
                Collections.unmodifiableList(Collections.singletonList(KEY_METHOD)));

        View view_4 = View.create(View.Name.create("ocjavametrics/last"), "last", M_LATENCY_MS, lastValue,
                Collections.unmodifiableList(Collections.singletonList(KEY_METHOD)));


        ViewManager vmgr = Stats.getViewManager();
        vmgr.registerView(view_1);
        vmgr.registerView(view_2);
        vmgr.registerView(view_3);
        vmgr.registerView(view_4);

        MetricProducerManager metricProducerManager = Metrics.getExportComponent().getMetricProducerManager();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (MetricProducer metricProducer : metricProducerManager.getAllMetricProducer()) {
                    for (Metric metric : metricProducer.getMetrics()) {
                        System.out.println(gson.toJson(metric.getMetricDescriptor()));
                        for (TimeSeries series : metric.getTimeSeriesList()) {
                            System.out.println(series);
                        }
                    }
                    System.out.println("********************");
                }
            }
        }, 0, 10, TimeUnit.SECONDS);


        for (; ; ) {
            TagContext tagContext = tagger.emptyBuilder()
                    .put(KEY_METHOD, TagValue.create("method-abc")).build();

            try (Scope ss = tagger.withTagContext(tagContext)) {
                statsRecorder.newMeasureMap().put(M_LATENCY_MS, 100).record();
            }
            TimeUnit.SECONDS.sleep(2);

            TagContext tagContext_1 = tagger.emptyBuilder()
                    .put(KEY_METHOD, TagValue.create("method-abcd")).build();

            try (Scope ss = tagger.withTagContext(tagContext_1)) {
                statsRecorder.newMeasureMap().put(M_LATENCY_MS, 100).record();
            }
        }

    }
}
