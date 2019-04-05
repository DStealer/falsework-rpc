package com.falsework.jdbc.maven.plugin;

import com.falsework.jdbc.maven.config.Jdbc;
import com.falsework.jdbc.maven.config.Target;
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
    @Parameter(property = "target", required = true)
    private Target target;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("run generate");

        if (!new File(target.getDirectory()).isAbsolute())
            target.setDirectory(project.getBasedir() + File.separator + target.getDirectory());

        Generator generator = new Generator(jdbc, target);
        generator.run();
        project.addCompileSourceRoot(target.getDirectory());
        getLog().info("run completely!");
    }
}
