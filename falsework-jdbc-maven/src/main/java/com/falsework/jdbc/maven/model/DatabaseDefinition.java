package com.falsework.jdbc.maven.model;

import java.util.ArrayList;
import java.util.List;

public class DatabaseDefinition {
    private final String catalog;
    private final String schema;
    private final String comment;
    private final List<TableDefinition> tableDefinitionList;

    public DatabaseDefinition(String catalog, String schema, String comment) {
        this.catalog = catalog;
        this.schema = schema;
        this.comment = comment;
        this.tableDefinitionList = new ArrayList<>();
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    public String getComment() {
        return comment;
    }

    public void addTable(TableDefinition table) {
        this.tableDefinitionList.add(table);
    }

    public List<TableDefinition> getTableDefinitionList() {
        return tableDefinitionList;
    }

    @Override
    public String toString() {
        return "DatabaseDefinition{" +
                "catalog='" + catalog + '\'' +
                ", schema='" + schema + '\'' +
                ", comment='" + comment + '\'' +
                ", tableDefinitionList=" + tableDefinitionList +
                '}';
    }
}
