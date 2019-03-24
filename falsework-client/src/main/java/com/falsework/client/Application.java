package com.falsework.client;

import com.falsework.client.background.BackgroundModule;
import com.falsework.client.dependecy.DependencyModule;
import com.falsework.core.FalseWorkApplicationBuilder;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        FalseWorkApplicationBuilder.newBuilder()
                .withModule(new DependencyModule())
                .withModule(new BackgroundModule())
                .runSync();
    }
}
