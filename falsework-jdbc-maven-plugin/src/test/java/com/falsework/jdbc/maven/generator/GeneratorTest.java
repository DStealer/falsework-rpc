package com.falsework.jdbc.maven.generator;

import com.falsework.jdbc.maven.config.Config;
import com.falsework.jdbc.maven.config.Jdbc;
import org.junit.Test;

import java.io.File;

public class GeneratorTest {

    @Test
    public void run() throws Exception {
        Jdbc jdbc = new Jdbc();
        jdbc.setDriver("com.mysql.cj.jdbc.Driver");
        jdbc.setUrl("jdbc:mysql://127.0.0.1:3306?useSSL=false");
        jdbc.setCatalog("testdb");
        jdbc.setUsername("user");
        jdbc.setPassword("user");

        Config config = new Config();
        config.setDirectory(new File("/tmp/generated"));
        Generator generator = new Generator(jdbc, config);
        generator.run();
    }
}