package com.falsework.jdbc.maven.generator;

import com.falsework.jdbc.maven.composite.Utils;
import com.falsework.jdbc.maven.config.Config;
import com.falsework.jdbc.maven.config.Jdbc;
import com.falsework.jdbc.maven.converter.TypeConverter;
import com.falsework.jdbc.maven.model.ColumnDefinition;
import com.falsework.jdbc.maven.model.DatabaseDefinition;
import com.falsework.jdbc.maven.model.TableDefinition;
import com.falsework.jdbc.maven.writer.JavaWriter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Generator {
    private final Jdbc jdbc;
    private final Config config;

    public Generator(Jdbc jdbc, Config config) {
        this.jdbc = jdbc;
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public void run() throws Exception {
        TypeConverter converter = (TypeConverter) Utils.loadClass(this.config.getTypeConverterClass())
                .getDeclaredConstructor().newInstance();

        Class<? extends Driver> driver = (Class<? extends Driver>) Utils.loadClass(this.jdbc.getDriver());
        Properties properties = this.jdbc.getProperties() == null ? new Properties() : this.jdbc.getProperties();
        if (!Objects.isNull(this.jdbc.getUsername())) {
            properties.put("user", this.jdbc.getUsername());
        }
        if (!Objects.isNull(this.jdbc.getPassword())) {
            properties.put("password", this.jdbc.getPassword());
        }

        Connection connection = driver.getDeclaredConstructor().newInstance().connect(this.jdbc.getUrl(), properties);

        DatabaseMetaData databaseMetaData = connection.getMetaData();

        DatabaseDefinition databaseDefinition = new DatabaseDefinition(this.jdbc.getCatalog(), this.jdbc.getSchema());

        ResultSet tableResultSet = databaseMetaData.getTables(databaseDefinition.getCatalog(), databaseDefinition
                .getSchema(), null, null);

        while (tableResultSet.next()) {
            TableDefinition tableDefinition = new TableDefinition(tableResultSet.getString(3),
                    tableResultSet.getString(4), tableResultSet.getString(5));
            databaseDefinition.addTable(tableDefinition);

            ResultSet columnResultSet = databaseMetaData.getColumns(databaseDefinition.getCatalog(),
                    databaseDefinition.getSchema(), tableDefinition.getName(), null);
            while (columnResultSet.next()) {
                int sqlType = columnResultSet.getInt(5);
                String jdbcType = columnResultSet.getString(6);
                Class javaType = converter.convert(jdbcType, sqlType);
                ColumnDefinition columnDefinition = new ColumnDefinition(columnResultSet.getString(4),
                        sqlType, jdbcType, javaType, columnResultSet.getInt(7)
                        , columnResultSet.getInt(17), columnResultSet.getString(13),
                        columnResultSet.getInt(11), columnResultSet.getString(12));
                tableDefinition.addColumn(columnDefinition);
            }
        }
        generate(databaseDefinition, this.config);
    }

    private void generate(DatabaseDefinition databaseDefinition, Config config) throws Exception {
        File targetDir = config.getDirectory();
        if (targetDir.exists()) {
            FileUtils.cleanDirectory(targetDir);
        } else {
            targetDir.mkdirs();
        }
        for (TableDefinition tableDefinition : databaseDefinition.getTableDefinitionList()) {
            generate(tableDefinition, targetDir, config);
        }
    }

    private void generate(TableDefinition tableDefinition, File targetDir, Config config) throws Exception {

        String clazzName = snakeToCamelHumps(tableDefinition.getName()) + "Record";

        String qualifiedType = config.getPackageName() + "." + clazzName;

        JavaWriter writer = new JavaWriter(qualifiedType, targetDir, config.getEncoding());
        writer.println();
        writer.printImport();
        writer.println("public class %s{", clazzName);

        List<ColumnDefinition> columnDefinitions = tableDefinition.getColumnDefinitionList();
        columnDefinitions.sort(Comparator.comparing(ColumnDefinition::getPosition));

        writer.tabInc();
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            writer.ref(columnDefinition.getJavaType());
            writer.tabLn("private %s %s;", columnDefinition.getJavaType().getSimpleName(),
                    escapeKeyWord(snakeToCamelHumpsTitle(columnDefinition.getName())));
        }
        writer.println();

        for (ColumnDefinition columnDefinition : columnDefinitions) {
            String className = columnDefinition.getJavaType().getSimpleName();
            String sth = escapeKeyWord(snakeToCamelHumps(columnDefinition.getName()));
            String stht = escapeKeyWord(snakeToCamelHumpsTitle(columnDefinition.getName()));

            writer.tabLn("public %s get%s(){", className, sth);
            writer.tabIncLn("return this.%1$s;", stht);
            writer.tabDecLn("}");
            writer.tabLn("public void set%s(%s %s){", sth, className, stht);
            writer.tabIncLn("this.%s=%s;", stht, stht);
            writer.tabDecLn("}");
            writer.println();
        }
        writer.tabDec()
                .tabLn("}");
        writer.close();
    }


    private String snakeToCamelHumps(String string) {
        String[] parts = string.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return sb.toString();
    }

    private String escapeKeyWord(String word) {
        if (!Utils.isKeyword(word)) {
            return word;
        } else {
            return word + 0;
        }
    }

    private String snakeToCamelHumpsTitle(String string) {
        StringBuilder sb = new StringBuilder();
        String[] parts = string.split("_");
        sb.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
