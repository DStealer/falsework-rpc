package com.falsework.core.datasource;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import io.opencensus.stats.*;
import io.opencensus.tags.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * open census 统计追踪器
 */
public class OpenCensusMetricsTracker implements IMetricsTracker {
    public static final OpenCensusMetricsTrackerFactory FACTORY = new OpenCensusMetricsTrackerFactory();

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCensusMetricsTracker.class);

    private static final Measure.MeasureLong M_CONNECTION_TIMEOUT = Measure.MeasureLong.create("hikari/timeouts", "Hikari timeout number", "1");
    private static final Measure.MeasureLong M_USAGE_MILLIS = Measure.MeasureLong.create("hikari/usageMillis", "Hikari usage millis", "ms");

    private static final Measure.MeasureLong M_TOTAL_CONNECTION = Measure.MeasureLong.create("hikari/connection/total", "Hikari total connections", "1");
    private static final Measure.MeasureLong M_IDLE_CONNECTION = Measure.MeasureLong.create("hikari/connection/idle", "Hikari idle connections", "1");
    private static final Measure.MeasureLong M_ACTIVE_CONNECTION = Measure.MeasureLong.create("hikari/connection/active", "Hikari active connections", "1");
    private static final Measure.MeasureLong M_PENDING_THREAD = Measure.MeasureLong.create("hikari/thread/pending", "Hikari pending threads", "1");
    private static final Measure.MeasureLong M_MAX_CONNECTION = Measure.MeasureLong.create("hikari/connection/max", "Hikari max connections", "1");
    private static final Measure.MeasureLong M_MIN_CONNECTION = Measure.MeasureLong.create("hikari/connection/min", "Hikari min connections", "1");

    private static final TagKey POOL_NAME_TAG = TagKey.create("pool_name");
    private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();

    private static final Tagger TAGGER = Tags.getTagger();
    private final String poolName;
    private final PoolStats poolStats;
    private final TagContext startCtx;


    OpenCensusMetricsTracker(String poolName, PoolStats poolStats) {
        Preconditions.checkNotNull(poolName, "pool name");
        Preconditions.checkNotNull(poolStats, "pool stats");
        this.poolStats = poolStats;
        this.poolName = poolName;
        this.startCtx = TAGGER.emptyBuilder()
                .put(POOL_NAME_TAG, TagValue.create(poolName))
                .build();
    }

    /**
     * 注册视图
     */
    public static void registerViews() {
        LOGGER.info("Create data pool metrics tracker views");
        ViewManager viewManager = Stats.getViewManager();
        List<TagKey> tagKeys = Collections.singletonList(POOL_NAME_TAG);

        Aggregation.Sum sum = Aggregation.Sum.create();

        View connectionTimeoutView = View.create(View.Name.create("datasource/hikari/timeout"),
                "Hikari timeout number", M_CONNECTION_TIMEOUT, sum, tagKeys);
        viewManager.registerView(connectionTimeoutView);
        View usageMillisView = View.create(View.Name.create("datasource/hikari/usage"),
                "Hikari usage millis", M_USAGE_MILLIS, sum, tagKeys);
        viewManager.registerView(usageMillisView);

        Aggregation.LastValue lastValue = Aggregation.LastValue.create();

        View totalConnectionView = View.create(View.Name.create("datasource/hikari/total"),
                "Hikari total connections", M_TOTAL_CONNECTION, lastValue, tagKeys);
        viewManager.registerView(totalConnectionView);

        View idleConnectionView = View.create(View.Name.create("datasource/hikari/idle"),
                "Hikari idle connections", M_IDLE_CONNECTION, lastValue, tagKeys);
        viewManager.registerView(idleConnectionView);

        View activeConnectionView = View.create(View.Name.create("datasource/hikari/active"),
                "Hikari active connections", M_ACTIVE_CONNECTION, lastValue, tagKeys);
        viewManager.registerView(activeConnectionView);

        View pendingThreadView = View.create(View.Name.create("datasource/hikari/pendingThread"),
                "Hikari pending thread", M_PENDING_THREAD, lastValue, tagKeys);
        viewManager.registerView(pendingThreadView);

        View maxConnectionView = View.create(View.Name.create("datasource/hikari/max"),
                "Hikari max connections", M_MAX_CONNECTION, lastValue, tagKeys);
        viewManager.registerView(maxConnectionView);

        View minConnectionView = View.create(View.Name.create("datasource/hikari/min"),
                "Hikari min connections", M_MIN_CONNECTION, lastValue, tagKeys);
        viewManager.registerView(minConnectionView);
    }

    @Override
    public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
        STATS_RECORDER.newMeasureMap()
                .put(M_USAGE_MILLIS, elapsedBorrowedMillis)
                .put(M_TOTAL_CONNECTION, this.poolStats.getTotalConnections())
                .put(M_IDLE_CONNECTION, this.poolStats.getIdleConnections())
                .put(M_ACTIVE_CONNECTION, this.poolStats.getActiveConnections())
                .put(M_PENDING_THREAD, this.poolStats.getPendingThreads())
                .put(M_MAX_CONNECTION, this.poolStats.getMaxConnections())
                .put(M_MIN_CONNECTION, this.poolStats.getMinConnections())
                .record(this.startCtx);

    }

    @Override
    public void recordConnectionTimeout() {
        STATS_RECORDER.newMeasureMap().put(M_CONNECTION_TIMEOUT, 1)
                .put(M_PENDING_THREAD, this.poolStats.getPendingThreads())
                .record(this.startCtx);
    }

    @Override
    public void close() {
        LOGGER.info("data pool [{}] metrics tracker close", this.poolName);
    }

    /**
     * 统计工厂
     */
    private static class OpenCensusMetricsTrackerFactory implements MetricsTrackerFactory {
        @Override
        public OpenCensusMetricsTracker create(String poolName, PoolStats poolStats) {
            LOGGER.info("data pool [{}] metrics tracker starting", poolName);
            return new OpenCensusMetricsTracker(poolName, poolStats);
        }
    }
}
