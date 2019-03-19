package com.falsework.census;

import com.falsework.census.module.StubModule;
import com.falsework.census.module.ZipKinModule;
import com.falsework.core.FalseWorkApplicationBuilder;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        FalseWorkApplicationBuilder.newBuilder()
                .withModule(new ZipKinModule())
                .withModule(new StubModule())
                .runSync();
    }
}
