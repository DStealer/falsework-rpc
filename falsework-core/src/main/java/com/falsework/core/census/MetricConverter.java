package com.falsework.core.census;

import com.google.gson.*;
import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.LabelValue;
import io.opencensus.metrics.export.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class MetricConverter {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * 转换时间戳到毫秒数
     *
     * @param timestamp
     * @return
     */
    private static long convert(Timestamp timestamp) {
        return TimeUnit.SECONDS.toMillis(timestamp.getSeconds())
                + TimeUnit.NANOSECONDS.toMillis(timestamp.getNanos());
    }

    /**
     * 转换成json格式的数据
     *
     * @param metricProducerManager
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getJsonMetrics(MetricProducerManager metricProducerManager) {
        JsonArray jsonArray = new JsonArray();
        for (MetricProducer producer : metricProducerManager.getAllMetricProducer()) {
            for (Metric metric : producer.getMetrics()) {
                MetricDescriptor descriptor = metric.getMetricDescriptor();
                JsonObject metricJson = new JsonObject();
                metricJson.addProperty("name", descriptor.getName());
                metricJson.addProperty("description", descriptor.getDescription());
                metricJson.addProperty("unit", descriptor.getUnit());
                metricJson.addProperty("type", descriptor.getType().name());
                metricJson.add("labelKeys", GSON.toJsonTree(descriptor.getLabelKeys()
                        .stream().map(LabelKey::getKey).collect(Collectors.toList())));
                JsonArray seriesArray = new JsonArray();
                for (TimeSeries series : metric.getTimeSeriesList()) {
                    JsonObject seriesJson = new JsonObject();
                    seriesJson.add("labelValues", GSON.toJsonTree(series.getLabelValues()
                            .stream().map(LabelValue::getValue).collect(Collectors.toList())));
                    if (series.getStartTimestamp() != null) {
                        seriesJson.addProperty("startTimestamp", convert(series.getStartTimestamp()));
                    }
                    JsonArray pointArray = new JsonArray();
                    for (Point point : series.getPoints()) {
                        JsonObject pointJson = new JsonObject();
                        if (point.getTimestamp() != null) {
                            pointJson.addProperty("timestamp", convert(point.getTimestamp()));
                        }
                        JsonElement valueJson = point.getValue()
                                .match(GSON::toJsonTree,
                                        GSON::toJsonTree,
                                        arg -> {
                                            Distribution.BucketOptions options = arg.getBucketOptions();
                                            List<Double> bucketBoundaries = options.match(Distribution.BucketOptions.ExplicitOptions::getBucketBoundaries,
                                                    ag -> Collections.EMPTY_LIST);
                                            List<Long> buckets = arg.getBuckets().stream().map(Distribution.Bucket::getCount).collect(Collectors.toList());
                                            JsonObject bucketJson = new JsonObject();
                                            bucketJson.addProperty("sum", arg.getSum());
                                            bucketJson.addProperty("count", arg.getCount());
                                            bucketJson.add("bucketBoundaries", GSON.toJsonTree(bucketBoundaries));
                                            bucketJson.add("buckets", GSON.toJsonTree(buckets));
                                            return bucketJson;
                                        },
                                        arg -> {
                                            JsonObject summaryJson = new JsonObject();
                                            summaryJson.addProperty("sum", arg.getSum());
                                            summaryJson.addProperty("count", arg.getCount());
                                            return summaryJson;
                                        },
                                        arg -> JsonNull.INSTANCE);

                        pointJson.add("value", valueJson);
                        pointArray.add(pointJson);
                    }
                    seriesJson.add("points", pointArray);
                    seriesArray.add(seriesJson);
                }
                metricJson.add("timeSeries", seriesArray);
                jsonArray.add(metricJson);
            }

        }
        return jsonArray.toString();
    }
}
