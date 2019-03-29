package com.falsework.core.composite;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class RateLimiterTest {

    @Test
    public void acquire() {
        RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS);
        while (true) {
            if (rateLimiter.acquire(4, 2)) {
                System.out.println(new Date() + " .....ok");
            }
        }
    }
}