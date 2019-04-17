package com.falsework.jdbc.maven.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaWriter.class);
    private static final String SERIAL_STATEMENT = "__SERIAL_STATEMENT__";
    private static final String IMPORT_STATEMENT = "__IMPORT_STATEMENT__";
    private final File targetDir;
    private final String encoding;
    private final String qualifiedType;
    private final StringBuilder sb;
    private final Set<Class> refClasses = new HashSet<>();
    private int indentTabs;


    public JavaWriter(String qualifiedType, File targetDir, String encoding) {
        this.qualifiedType = qualifiedType;
        this.encoding = encoding;
        this.targetDir = targetDir;
        this.sb = new StringBuilder();
        int lastIndexOf = this.qualifiedType.lastIndexOf(".");

        if (lastIndexOf > -1) {
            String packageName = this.qualifiedType.substring(0, lastIndexOf);
            this.sb.append(String.format("package %s;", packageName)).append("\r\n");
        }
    }

    public JavaWriter tab(int tabs) {
        this.indentTabs = tabs;
        return this;
    }

    public JavaWriter tabInc() {
        this.indentTabs++;
        return this;
    }

    public JavaWriter tabDec() {
        this.indentTabs--;
        return this;
    }

    public JavaWriter println() {
        this.sb.append("\r\n");
        return this;
    }

    public JavaWriter tabLn(String string, Object... args) {
        for (int i = 0; i < this.indentTabs; i++) {
            this.sb.append("\t");
        }
        this.sb.append(String.format(string, args)).append("\r\n");
        return this;
    }

    public JavaWriter tabSingleLineDoc(String string, Object... args) {
        for (int i = 0; i < this.indentTabs; i++) {
            this.sb.append("\t");
        }
        this.sb.append("//").append(String.format(string, args)).append("\r\n");
        return this;
    }

    public JavaWriter tabIncLn(String string, Object... args) {
        this.indentTabs++;
        for (int i = 0; i < this.indentTabs; i++) {
            this.sb.append("\t");
        }
        this.sb.append(String.format(string, args)).append("\r\n");
        return this;
    }

    public JavaWriter tabDecLn(String string, Object... args) {
        this.indentTabs--;
        for (int i = 0; i < this.indentTabs; i++) {
            this.sb.append("\t");
        }
        this.sb.append(String.format(string, args)).append("\r\n");
        return this;
    }

    public JavaWriter println(String string, Object... args) {
        this.sb.append(String.format(string, args))
                .append("\r\n");
        return this;
    }

    public JavaWriter print(String string, Object... args) {
        this.sb.append(String.format(string, args));
        return this;
    }


    public JavaWriter printSerial() {
        this.sb.append(String.format("private static final long serialVersionUID = %s;", SERIAL_STATEMENT))
                .append("\\r\\n");
        return this;
    }

    public JavaWriter printImport() {
        this.sb.append(IMPORT_STATEMENT).append("\r\n");
        return this;
    }

    public JavaWriter ref(Class clazz) {
        if (!clazz.getPackage().getName().startsWith("java.lang.")) {
            this.refClasses.add(clazz);
        }
        return this;
    }

    private String escapeJavadoc(String string) {
        return string
                .replace("/*", "/ *")
                .replace("*/", "* /")
                .replace("\\u002a/", "\\u002a /")
                .replace("*\\u002f", "* \\u002f")
                .replace("\\u002a\\u002f", "\\u002a \\u002f");
    }

    public void close() throws Exception {
        String subName = this.qualifiedType.replace('.', File.separatorChar) + ".java";
        File targetFile = new File(this.targetDir, subName);
        LOGGER.info("write file:{}", targetFile.getCanonicalPath());
        if (targetFile.exists()) {
            targetFile.delete();
        } else {
            targetFile.getParentFile().mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), this.encoding))) {
            String source = this.sb.toString();
            if (this.refClasses.size() > 0) {
                StringBuilder importSb = new StringBuilder();
                for (Class clazz : this.refClasses) {
                    importSb.append("import ").append(clazz.getCanonicalName()).append("\r\n");
                }
                source = source.replace(IMPORT_STATEMENT, importSb.toString());
            } else {
                source = source.replace(IMPORT_STATEMENT, "");
            }

            Matcher m = Pattern.compile("(?s:^.*?[\\r\\n]+package\\s+(.*?);?[\\r\\n]+.*?$)").matcher(source);
            if (m.find()) {
                String pkg = m.group(1);
                source = source.replaceAll(pkg + "\\.[^\\.]+", "");
            }
            source = source.replace(SERIAL_STATEMENT, String.valueOf(source.hashCode()));
            writer.write(source);
        }
    }
}
