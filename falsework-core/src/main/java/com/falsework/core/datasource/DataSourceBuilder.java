package com.falsework.core.datasource;

import com.falsework.core.common.Builder;
import com.falsework.core.common.Props;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 构建数据源
 */
public class DataSourceBuilder implements Builder<HikariDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceBuilder.class);
    private String name;
    private Props props;
    private MetricsTrackerFactory metricsTrackerFactory;
    private ScheduledExecutorService executor;

    private DataSourceBuilder() {

    }

    public static DataSourceBuilder newBuilder() {
        return new DataSourceBuilder();
    }

    public DataSourceBuilder withName(String name) {
        Preconditions.checkNotNull(name, "name invalid");
        this.name = name;
        return this;
    }

    public DataSourceBuilder withProps(Props props) {
        Preconditions.checkNotNull(props, "props invalid");
        this.props = props;
        return this;
    }

    public DataSourceBuilder withMetricsTrackerFactory(MetricsTrackerFactory metricsTrackerFactory) {
        Preconditions.checkNotNull(metricsTrackerFactory, "metricsTrackerFactory invalid");
        this.metricsTrackerFactory = metricsTrackerFactory;
        return this;
    }

    public DataSourceBuilder withScheduledExecutor(ScheduledExecutorService executor) {
        Preconditions.checkNotNull(executor, "executor invalid");
        this.executor = executor;
        return this;
    }

    @Override
    public HikariDataSource build() {
        LOGGER.info("find config :{}", name);
        HikariConfig config = new HikariConfig(props);
        if (Strings.isNullOrEmpty(config.getPoolName())) {
            config.setPoolName(String.format("pool-%s", name));
        }
        if (this.metricsTrackerFactory != null) {
            config.setMetricsTrackerFactory(this.metricsTrackerFactory);
        }
        if (this.executor != null) {
            config.setScheduledExecutor(this.executor);
        }
        LOGGER.info("config datasource:{}", config.toString());
        return new HikariDataSource(config);
    }
}
