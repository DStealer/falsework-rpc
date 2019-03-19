package com.falsework.core.aop.common;

import com.falsework.core.common.Props;

import java.util.Map;

public class InternalHelper {
    /**
     * 保护性设置属性
     *
     * @param module
     * @param props
     */
    public static void setProps(EnvAwareModule module, Props props) {
        module.setProps(props);
    }

    /**
     * 保护性设置属性
     *
     * @param module
     * @param global
     */
    public static void setGlobal(EnvAwareModule module, Map<String, Object> global) {
        module.setGlobal(global);
    }

    /**
     * 保护性设置属性
     *
     * @param module
     */
    public static void setSeal(EnvAwareModule module) {
        module.sealed();
    }
}
