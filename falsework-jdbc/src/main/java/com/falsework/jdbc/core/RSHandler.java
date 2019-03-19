package com.falsework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by lishiwu on 2016/7/14.
 */
public interface RSHandler<T> {
    /**
     * 解析结果集到bean
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    T parseFrom(ResultSet rs) throws SQLException;
}
