package com.falsework.account;

import com.falsework.account.service.ServiceModule;
import com.falsework.core.FalseWorkApplicationBuilder;
import com.falsework.core.jooq.DSLContextModule;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        FalseWorkApplicationBuilder.newBuilder()
                .withModule(new DSLContextModule())
                .withModule(new ServiceModule())
                .runSync();
    }
}
