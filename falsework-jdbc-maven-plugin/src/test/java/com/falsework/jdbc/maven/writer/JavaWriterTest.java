package com.falsework.jdbc.maven.writer;

import org.junit.Test;

import java.io.File;

public class JavaWriterTest {
    @Test
    public void tt01() throws Exception {
        JavaWriter writer = new JavaWriter("com.falsework.module.TaskRecord",
                new File("/tmp"), "UTF8");
        writer.tabLn("public class TaskRecord{")
                .tabIncLn("private int age;")
                .tabLn("private String name;")
                .tabLn("public getAge(){")
                .tabIncLn("return this.age;")
                .tabDecLn("}")
                .tabDecLn("}");
        writer.close();

    }
}