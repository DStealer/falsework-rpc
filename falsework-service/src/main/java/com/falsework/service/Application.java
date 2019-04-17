package com.falsework.service;

import com.falsework.core.FalseWorkApplicationBuilder;
import com.falsework.service.core.ServiceModule;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        FalseWorkApplicationBuilder.newBuilder()
                .withModule(new ServiceModule())
                .runSync();
    }
}
