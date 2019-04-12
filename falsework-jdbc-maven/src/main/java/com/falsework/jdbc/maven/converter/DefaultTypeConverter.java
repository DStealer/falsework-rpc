package com.falsework.jdbc.maven.converter;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;

public class DefaultTypeConverter implements TypeConverter {
    @Override
    public Class convert(String jdbcType, int sqlType) {
        switch (sqlType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGNVARCHAR:
                return String.class;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return BigDecimal.class;
            case Types.BIT:
            case Types.BOOLEAN:
                return Boolean.class;
            case Types.TINYINT:
                return Byte.class;
            case Types.SMALLINT:
                return Short.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.BIGINT:
                return Long.class;
            case Types.REAL:
                return Float.class;
            case Types.FLOAT:
            case Types.DOUBLE:
                return Double.class;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return byte[].class;
            case Types.DATE:
                return Date.class;
            case Types.TIME:
                return Time.class;
            case Types.TIMESTAMP:
                return Timestamp.class;
            case Types.CLOB:
                return Clob.class;
            case Types.BLOB:
                return Blob.class;
            case Types.ARRAY:
                return Array.class;
            case Types.STRUCT:
                return Struct.class;
            case Types.REF:
                return Ref.class;
            case Types.DATALINK:
                return URL.class;
            default:
                return Object.class;
        }
    }
}
