package com.falsework.census.module;

import com.falsework.census.composite.ZipkinMySQLStorageTimedListener;
import com.falsework.core.aop.common.EnvAwareModule;
import com.falsework.core.common.Props;
import com.falsework.core.datasource.DataSourceBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.util.NettyRuntime;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ExecuteWithoutWhere;
import org.jooq.conf.Settings;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListenerProvider;
import zipkin2.collector.Collector;
import zipkin2.collector.CollectorMetrics;
import zipkin2.collector.CollectorSampler;
import zipkin2.storage.mysql.v1.MySQLStorage;

import java.util.concurrent.Executors;

public class ZipKinModule extends EnvAwareModule {
    @Override
    protected void configure() {
        Props tracingProps = getProps()
                .subProps("jdbc.census");

        HikariDataSource tracingDatasource = DataSourceBuilder.newBuilder()
                .withName("census")
                .withProps(tracingProps)
                .build();
        Settings settings = SettingsTools.defaultSettings()
                .withRenderCatalog(false)
                .withRenderSchema(false)
                .withMapJPAAnnotations(false)
                .withDebugInfoOnStackTrace(false)
                .withReturnAllOnUpdatableRecord(false)
                .withUpdatablePrimaryKeys(false)
                .withExecuteDeleteWithoutWhere(ExecuteWithoutWhere.THROW)
                .withExecuteUpdateWithoutWhere(ExecuteWithoutWhere.THROW)
                .withExecuteLogging(false);
        DSLContext context = DSL.using(tracingDatasource, SQLDialect.MYSQL_8_0, settings);

        bind(DSLContext.class).toInstance(context);

        //zipkin存储配置

        MySQLStorage storage = MySQLStorage.newBuilder()
                .strictTraceId(true)
                .searchEnabled(true)
                .datasource(tracingDatasource)
                .settings(settings)
                .listenerProvider(new DefaultExecuteListenerProvider(new ZipkinMySQLStorageTimedListener()))
                .executor(Executors.newFixedThreadPool(NettyRuntime.availableProcessors() * 4,
                        new ThreadFactoryBuilder().setNameFormat("ZipkinMySQLStorage-%d").setDaemon(true).build()))
                .build();

        bind(MySQLStorage.class).toInstance(storage);

        Collector collector = Collector.newBuilder(getClass())
                .storage(storage)
                .metrics(CollectorMetrics.NOOP_METRICS)
                .sampler(CollectorSampler.ALWAYS_SAMPLE)
                .build();

        bind(Collector.class).toInstance(collector);
    }
}
