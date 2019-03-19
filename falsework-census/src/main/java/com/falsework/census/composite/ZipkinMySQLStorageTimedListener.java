package com.falsework.census.composite;

import com.google.common.base.Stopwatch;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ZipkinMySQLStorageTimedListener extends DefaultExecuteListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinMySQLStorageTimedListener.class);
    private static final ThreadLocal<Stopwatch> STOPWATCH_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void start(ExecuteContext ctx) {
        STOPWATCH_THREAD_LOCAL.set(Stopwatch.createStarted());
    }

    @Override
    public void end(ExecuteContext ctx) {
        try {
            Stopwatch stopwatch = STOPWATCH_THREAD_LOCAL.get();
            LOGGER.info("ZipkinMySQLStorage executed using :{} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        } finally {
            STOPWATCH_THREAD_LOCAL.remove();
        }
    }

    @Override
    public void exception(ExecuteContext ctx) {
        try {
            Stopwatch stopwatch = STOPWATCH_THREAD_LOCAL.get();
            LOGGER.error("ZipkinMySQLStorage error using :{} ms", stopwatch.stop().elapsed(TimeUnit.MILLISECONDS),
                    ctx.exception() != null ? ctx.exception() : ctx.sqlException());
        } finally {
            STOPWATCH_THREAD_LOCAL.remove();
        }
    }
}
