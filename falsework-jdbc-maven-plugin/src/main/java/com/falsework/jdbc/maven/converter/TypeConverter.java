package com.falsework.jdbc.maven.converter;

public interface TypeConverter {
    Class convert(String jdbcType, int sqlType);
}
