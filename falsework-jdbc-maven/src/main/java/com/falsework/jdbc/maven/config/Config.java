package com.falsework.jdbc.maven.config;

import java.io.File;

public class Config {
    private String packageName = "com.falsework.jdbc.generated";
    private File directory = new File("target/generated-sources/jdbc");
    private String encoding = "UTF-8";
    private String typeConverterClass = "com.falsework.jdbc.maven.converter.DefaultTypeConverter";

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getTypeConverterClass() {
        return typeConverterClass;
    }

    public void setTypeConverterClass(String typeConverterClass) {
        this.typeConverterClass = typeConverterClass;
    }

    @Override
    public String toString() {
        return "Config{" +
                "packageName='" + packageName + '\'' +
                ", directory=" + directory +
                ", encoding='" + encoding + '\'' +
                ", typeConverterClass='" + typeConverterClass + '\'' +
                '}';
    }
}
