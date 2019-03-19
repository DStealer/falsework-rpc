package com.falsework.jdbc.core;

import java.util.Date;

/**
 * 简单类型处理
 * Created by LiShiwu on 03/08/2017.
 */
public class SimpleResult {
    /**
     * 数字类型
     */
    public static final RSHandler<Number> NumberResult = rs -> rs.getBigDecimal(1);
    /**
     * 字符串类型
     */
    public static final RSHandler<String> StringResult = rs -> rs.getString(1);
    /**
     * 日期类型
     */
    public static final RSHandler<Date> DateResult = rs -> rs.getTimestamp(1);
}
