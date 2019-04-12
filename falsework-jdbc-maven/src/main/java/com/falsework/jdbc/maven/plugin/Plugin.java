package com.falsework.jdbc.maven.plugin;

import com.falsework.jdbc.maven.config.Config;
import com.falsework.jdbc.maven.config.Jdbc;
import com.falsework.jdbc.maven.generator.Generator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class Plugin extends AbstractMojo {
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;
    @Parameter(property = "jdbc", required = true)
    private Jdbc jdbc;
    @Parameter(property = "config", required = true)
    private Config config;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("run generate");
        getLog().info("jdbc:" + this.jdbc);
        getLog().info("config:" + this.config);

        if (!config.getDirectory().isAbsolute())
            config.setDirectory(new File(project.getBasedir(), config.getDirectory().getPath()));

        Generator generator = new Generator(jdbc, config);
        try {
            generator.run();
            project.addCompileSourceRoot(config.getDirectory().getAbsolutePath());
            getLog().info("run completely!");
        } catch (Exception e) {
            throw new MojoExecutionException("generate code failed", e);
        }
    }
}
