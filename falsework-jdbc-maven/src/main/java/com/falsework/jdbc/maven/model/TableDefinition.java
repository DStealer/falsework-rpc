package com.falsework.jdbc.maven.model;

import java.util.ArrayList;
import java.util.List;

public class TableDefinition {
    private final String name;
    private final String type;
    private final String comment;
    private final List<ColumnDefinition> columnDefinitionList;

    public TableDefinition(String name, String type, String comment) {
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.columnDefinitionList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getType() {
        return type;
    }

    public void addColumn(ColumnDefinition column) {
        this.columnDefinitionList.add(column);
    }

    public List<ColumnDefinition> getColumnDefinitionList() {
        return columnDefinitionList;
    }

    @Override
    public String toString() {
        return "TableDefinition{" +
                "name='" + name + '\'' +
                ", comment='" + comment + '\'' +
                ", columnDefinitionList=" + columnDefinitionList +
                '}';
    }
}
