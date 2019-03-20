package com.falsework.eureka.mock;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.DynamicPropertyFactory;
import org.junit.Test;

public class EurekaMock {
    @Test
    public void test01() {//server
        MyDataCenterInstanceConfig instanceConfig = new MyDataCenterInstanceConfig();

        DynamicPropertyFactory configInstance = DynamicPropertyFactory.getInstance();

        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);

        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);
    }

    @Test
    public void test02() {

    }
}
