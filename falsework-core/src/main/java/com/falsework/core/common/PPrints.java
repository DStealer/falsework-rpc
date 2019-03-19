package com.falsework.core.common;


import com.falsework.core.generated.etcd.ServiceDefinition;

public abstract class PPrints {
    /**
     * 格式化输出
     *
     * @param definition
     * @return
     */
    public static String toString(ServiceDefinition definition) {
        return String.format("%s<=>%s:%d", definition.getName(), definition.getHost(), definition.getPort());
    }
}
