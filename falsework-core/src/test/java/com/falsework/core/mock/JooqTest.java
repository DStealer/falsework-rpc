package com.falsework.core.mock;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.jooq.conf.ExecuteWithoutWhere;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class JooqTest {
    private DSLContext context;

    @Before
    public void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/quant_docker_hub?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=GMT%2b8");
        config.setUsername("user");
        config.setPassword("user");
        config.setConnectionTestQuery("select 1");
        HikariDataSource datasource = new HikariDataSource(config);
        context = DSL.using(datasource, SQLDialect.MYSQL_8_0, SettingsTools.defaultSettings()
                .withRenderCatalog(false)
                .withRenderSchema(false)
                .withMapJPAAnnotations(false)
                .withDebugInfoOnStackTrace(false)
                .withReturnAllOnUpdatableRecord(true)
                .withUpdatablePrimaryKeys(false)
                .withExecuteDeleteWithoutWhere(ExecuteWithoutWhere.THROW)
                .withExecuteUpdateWithoutWhere(ExecuteWithoutWhere.THROW)
                .withExecuteLogging(false));
    }

    @Test
    public void tt01() {
        Record1<Timestamp> one = context.select(DSL.now()).fetchOne();
        System.out.println(one);
    }

    @Test
    public void tt02() throws InterruptedException {
        context.transactionAsync(configuration -> DSL.using(configuration).selectOne());
        TimeUnit.SECONDS.sleep(1000);
    }

    @Test
    public void tt03() {
        context.transaction(new TransactionalRunnable() {
            @Override
            public void run(Configuration configuration) throws Throwable {

            }
        });
    }
}
