package com.falsework.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 配置文件处理
 */
public enum PropsManager {
    ;
    private static final Logger LOGGER = LoggerFactory.getLogger(PropsManager.class);
    private static Props props;

    /**
     * 配置文件初始化
     *
     * @param filename
     */
    public static Props initConfig(String filename) {
        if (props == null) {
            try {
                props = Props.loadFromClassPath(filename);
            } catch (IOException e) {
                LOGGER.warn("can't find file in class path");
                try {
                    props = Props.loadFromPath(filename);
                } catch (IOException e1) {
                    throw new RuntimeException("file name not found");
                }
            }
            return props;
        } else {
            throw new IllegalStateException("Props has initialed...,don't do this again!");
        }
    }

    /**
     * 获取全局配置实例
     *
     * @return
     */
    public static Props getProps() {
        if (props != null) {
            return props;
        } else {
            throw new IllegalStateException("Init first please!");
        }
    }
}
