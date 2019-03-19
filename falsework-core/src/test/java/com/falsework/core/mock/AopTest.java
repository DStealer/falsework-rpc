package com.falsework.core.mock;

import com.google.inject.*;
import org.junit.Assert;
import org.junit.Test;

public class AopTest {

    @Test
    public void tt01() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                bind(AopObject.class).asEagerSingleton();
                bind(Object.class).to(AopObject.class);
            }
        });
        Binding<AopObject> binding = injector.getBinding(AopObject.class);
        AopObject aopObject = binding.getProvider().get();
        aopObject.run();
        Binding<Object> object = injector.getBinding(Object.class);
        Assert.assertNotNull(object.getProvider());
    }

    @Test
    public void tt02(){
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                bind(AopObject.class).asEagerSingleton();
            }
        });
        Binding<AopObject> binding = injector.getBinding(AopObject.class);
    }

}
