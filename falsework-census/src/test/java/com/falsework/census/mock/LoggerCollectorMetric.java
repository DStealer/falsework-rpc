package com.falsework.census.mock;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.collector.CollectorMetrics;

public class LoggerCollectorMetric implements CollectorMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerCollectorMetric.class);

    LoggerCollectorMetric() {
    }

    @Override
    public CollectorMetrics forTransport(String transportType) {
        Preconditions.checkArgument("grpc".equals(transportType), transportType);
        return new LoggerCollectorMetric();
    }

    @Override
    public void incrementMessages() {
        LOGGER.info("incrementMessages");
    }

    @Override
    public void incrementMessagesDropped() {
        LOGGER.info("incrementMessagesDropped");
    }

    @Override
    public void incrementSpans(int quantity) {
        LOGGER.info("incrementSpans:{}", quantity);
    }

    @Override
    public void incrementBytes(int quantity) {
        LOGGER.info("incrementBytes:{}", quantity);
    }

    @Override
    public void incrementSpansDropped(int quantity) {
        LOGGER.info("incrementSpansDropped:{}", quantity);
    }
}
