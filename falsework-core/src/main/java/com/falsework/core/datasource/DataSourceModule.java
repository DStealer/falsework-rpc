package com.falsework.core.datasource;

import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.common.Props;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据源绑定模块
 */
public class DataSourceModule extends EnvAwareModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceModule.class);

    @Override
    protected void configure() {
        LOGGER.info("Try to config datasource content");
        Map<String, Props> namedProps = getProps().subNamedProps("jdbc");
        namedProps.forEach((name, prop) -> {
            HikariDataSource dataSource = DataSourceBuilder.newBuilder()
                    .withName(name)
                    .withProps(prop)
                    .withMetricsTrackerFactory(OpenCensusMetricsTracker.FACTORY)
                    .build();
            LOGGER.info("bind datasource <> Names:{}", name);
            bind(DataSource.class).annotatedWith(Names.named(name)).toInstance(dataSource);
        });
        LOGGER.info("config datasource content complete");
    }
}
