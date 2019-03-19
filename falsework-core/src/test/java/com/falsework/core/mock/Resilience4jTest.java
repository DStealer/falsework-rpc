package com.falsework.core.mock;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import org.junit.Test;

import java.time.Duration;
import java.util.function.Supplier;

public class Resilience4jTest {
    public void work() {
        System.out.println(System.currentTimeMillis());
    }

    public String print() {
        return String.valueOf(System.currentTimeMillis());
    }

    public String thrEx() {
        throw new RuntimeException("ex");
    }

    @Test
    public void tt01() throws Throwable {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(100))
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .build();
        RateLimiter rateLimiter = RateLimiter.of("backendName", config);
        CheckedRunnable work = RateLimiter.decorateCheckedRunnable(rateLimiter, this::work);
        CheckedFunction0<String> print = RateLimiter.decorateCheckedSupplier(rateLimiter, this::print);
        for (int i = 0; i < 1000; i++) {
            work.run();
            print.apply();
        }
    }

    @Test
    public void tt02() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, this::thrEx);
        for (int i = 0; i < 1000; i++) {
            try {
                supplier.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void tt03() {
        Bulkhead bulkhead = Bulkhead.ofDefaults("backendName");

        Supplier<String> supplier = Bulkhead.decorateSupplier(bulkhead, this::print);
    }
}
