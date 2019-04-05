package com.falsework.jdbc.maven.generator;

import com.falsework.jdbc.maven.config.Jdbc;
import com.falsework.jdbc.maven.config.Target;

public class Generator {
    private final Jdbc jdbc;
    private final Target target;

    public Generator(Jdbc jdbc, Target target) {
        this.jdbc = jdbc;
        this.target = target;
    }

    public void run() {

    }
}
