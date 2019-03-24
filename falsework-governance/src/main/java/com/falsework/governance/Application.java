package com.falsework.governance;

import com.falsework.core.FalseWorkApplicationBuilder;
import com.falsework.governance.service.ServiceModule;
import com.falsework.governance.storage.StorageModule;

/**
 * Hello world!
 */
public class Application {
    public static void main(String[] args) throws Exception {
        FalseWorkApplicationBuilder.newBuilder()
                .withModule(new ServiceModule())
                .withModule(new StorageModule())
                .runSync();
    }
}
