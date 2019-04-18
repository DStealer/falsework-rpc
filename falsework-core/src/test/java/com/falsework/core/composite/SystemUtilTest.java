package com.falsework.core.composite;

import org.junit.Assert;
import org.junit.Test;

public class SystemUtilTest {

    @Test
    public void equivalentAddress() {
        Assert.assertFalse(SystemUtil.equivalentAddress("127.0.0.1", 8080, "127.0.0.1", 8081));
        Assert.assertTrue(SystemUtil.equivalentAddress("::1", 8080, "127.0.0.1", 8080));
        Assert.assertTrue(SystemUtil.equivalentAddress("172.17.0.1", 8080, "127.0.0.1", 8080));
    }
}