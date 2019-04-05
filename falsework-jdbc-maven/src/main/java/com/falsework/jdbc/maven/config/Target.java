package com.falsework.jdbc.maven.config;

public class Target {
    private String packageName = "com.falsework.jdbc.generated";
    private String directory = "target/generated-sources/jdbc";
    private String encoding = "UTF-8";

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
