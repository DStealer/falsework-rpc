package com.falsework.core.mock;

import com.falsework.core.FalseWorkApplicationBuilder;
import com.falsework.core.common.Props;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class PropsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropsTest.class);

    @Test
    public void tt01() throws IOException {
        Props propsUtil = Props.loadFromClassPath("bootstrap.properties");
        System.out.println(propsUtil);
        System.out.println(propsUtil.subProps("server"));
        System.out.println(propsUtil.subNamedProps("jdbc"));
        System.out.println(Arrays.toString(propsUtil.getStringArray("etcd.endpoints")));
    }

    @Test
    public void tt02() throws Exception {
        FalseWorkApplicationBuilder.newBuilder().build().run();
    }
}
