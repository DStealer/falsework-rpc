package com.falsework.census.mock;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.conf.ExecuteWithoutWhere;
import org.jooq.conf.SettingsTools;
import org.jooq.tools.LoggerListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Callback;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.collector.Collector;
import zipkin2.collector.CollectorSampler;
import zipkin2.storage.mysql.v1.MySQLStorage;

import java.io.IOException;
import java.util.List;

public class ZipkinTest {
    public static final Endpoint FRONTEND =
            Endpoint.newBuilder().serviceName("frontend").ip("127.0.0.1").build();
    public static final Endpoint BACKEND =
            Endpoint.newBuilder().serviceName("backend").ip("192.168.99.101").port(9000).build();
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinTest.class);
    Span base = Span.newBuilder().traceId("1").id("1").localEndpoint(FRONTEND).build();
    Span oneOfEach = Span.newBuilder()
            .traceId("7180c278b62e8f6a216a2aea45d08fc9")
            .parentId("1")
            .id("2")
            .name("get")
            .kind(Span.Kind.SERVER)
            .localEndpoint(BACKEND)
            .remoteEndpoint(FRONTEND)
            .timestamp(1)
            .duration(3)
            .addAnnotation(2, "foo")
            .putTag("http.path", "/api")
            .shared(true)
            .debug(true)
            .build();
    private Gson gson;
    private MySQLStorage storage;
    private HikariDataSource datasource;

    @Before
    public void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/statistics?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=GMT%2b8");
        config.setUsername("user");
        config.setPassword("user");
        config.setConnectionTestQuery("select 1");
        this.datasource = new HikariDataSource(config);
        this.storage = MySQLStorage.newBuilder()
                .strictTraceId(true)
                .searchEnabled(true)
                .datasource(datasource)
                .settings(SettingsTools.defaultSettings()
                        .withRenderCatalog(false)
                        .withRenderSchema(false)
                        .withMapJPAAnnotations(false)
                        .withDebugInfoOnStackTrace(false)
                        .withReturnAllOnUpdatableRecord(true)
                        .withUpdatablePrimaryKeys(false)
                        .withExecuteDeleteWithoutWhere(ExecuteWithoutWhere.THROW)
                        .withExecuteUpdateWithoutWhere(ExecuteWithoutWhere.THROW)
                        .withExecuteLogging(false))
                .executor(MoreExecutors.directExecutor())
                .listenerProvider(LoggerListener::new)
                .build();
        this.gson = new GsonBuilder().create();
    }

    @After
    public void tearDown() throws Exception {
        storage.close();
    }

    @Test
    public void tt01() throws IOException {
        List<String> execute = storage.spanStore().getServiceNames().execute();
        System.out.println(execute);
    }

    @Test
    public void tt02() {
        Collector collector = Collector.newBuilder(getClass())
                .storage(storage)
                .metrics(new LoggerCollectorMetric())
                .sampler(CollectorSampler.create(1.0f))
                .build();

        collector.accept(Lists.newArrayList(oneOfEach), new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                LOGGER.info("success:", value);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.info("error:", t);
            }
        });
    }
}
