package com.falsework.jdbc.maven.config;

import java.util.Properties;

public class Jdbc {
    private String driver;
    private String url;
    private String schema;
    private String catalog;
    private String username;
    private String password;
    private Properties properties;

    public String getDriver() {
        return driver;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Jdbc{" +
                "driver='" + driver + '\'' +
                ", url='" + url + '\'' +
                ", schema='" + schema + '\'' +
                ", catalog='" + catalog + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", properties=" + properties +
                '}';
    }
}
