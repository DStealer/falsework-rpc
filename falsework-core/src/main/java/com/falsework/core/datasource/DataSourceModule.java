package com.falsework.core.datasource;

import com.falsework.core.config.Props;
import com.falsework.core.config.PropsManager;
import com.falsework.core.config.PropsVars;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据源绑定模块
 */
public class DataSourceModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceModule.class);

    @Override
    protected void configure() {
        LOGGER.info("Try to config datasource content");
        Props props = PropsManager.getProps();
        Map<String, Props> namedProps = props.subNamedProps(PropsVars.JDBC_PREFIX);
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
