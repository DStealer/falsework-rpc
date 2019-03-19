package com.falsework.core.datasource;

import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.common.Props;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.*;
import org.jooq.tools.jdbc.JDBCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 提供JOOQ运行环境
 */
public class DSLContextModule extends EnvAwareModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(DSLContextModule.class);

    static {
        System.setProperty("org.jooq.no-logo", "true");
    }

    @Override
    protected void configure() {
        LOGGER.info("Try to config jooq content");
        Map<String, Props> namedProps = getProps().subNamedProps("jdbc");
        namedProps.forEach((name, prop) -> {
            HikariDataSource dataSource = DataSourceBuilder.newBuilder()
                    .withName(name)
                    .withProps(prop)
                    .withMetricsTrackerFactory(OpenCensusMetricsTracker.FACTORY)
                    .build();

            DefaultConfiguration configuration = new DefaultConfiguration();
            configuration.set(SettingsTools.defaultSettings().withRenderSchema(false));
            configuration.setSQLDialect(JDBCUtils.dialect(dataSource.getJdbcUrl()));
            configuration.setExecuteListenerProvider(DefaultExecuteListenerProvider
                    .providers(new OpenCensusExecuteListener(dataSource.getPoolName(), true)));

            configuration.setDataSource(dataSource);
            configuration.setTransactionProvider(new ThreadLocalTransactionProvider(
                    new DataSourceConnectionProvider(dataSource)));
            DSLContext context = DSL.using(configuration);
            LOGGER.info("bind DSLContext <> Name:{}", name);
            bind(DSLContext.class).annotatedWith(Names.named(name)).toInstance(context);
        });
        LOGGER.info("config jooq content complete");
    }
}
