package com.falsework.jdbc.maven.model;

public class ColumnDefinition {
    private final String name;
    private final int sqlType;
    private final String jdbcType;
    private final Class javaType;
    private final int length;
    private final int position;
    private final String defVal;
    private final int nullAble;
    private final String comment;

    public ColumnDefinition(String name, int sqlType, String jdbcType, Class javaType, int length, int position,
                            String defVal, int nullAble, String comment) {
        this.name = name;
        this.sqlType = sqlType;
        this.jdbcType = jdbcType;
        this.javaType = javaType;
        this.length = length;
        this.position = position;
        this.defVal = defVal;
        this.nullAble = nullAble;
        this.comment = comment;
    }

    public Class getJavaType() {
        return javaType;
    }

    public String getName() {
        return name;
    }

    public int getSqlType() {
        return sqlType;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public int getLength() {
        return length;
    }

    public int getPosition() {
        return position;
    }

    public String getDefVal() {
        return defVal;
    }

    public int getNullAble() {
        return nullAble;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "ColumnDefinition{" +
                "name='" + name + '\'' +
                ", sqlType=" + sqlType +
                ", jdbcType='" + jdbcType + '\'' +
                ", javaType=" + javaType +
                ", length=" + length +
                ", position=" + position +
                ", defVal='" + defVal + '\'' +
                ", nullAble=" + nullAble +
                ", comment='" + comment + '\'' +
                '}';
    }
}
