/*
 * Copyright 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.mojo;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import com.facebook.swift.generator.SwiftGenerator;
import com.facebook.swift.generator.SwiftGeneratorConfig;
import com.google.common.base.Throwables;
import com.pyx4j.log4j.MavenLogAppender;

/**
 * Process one or more idl files and write java code.
 *
 * @requiresProject true
 * @goal generate
 * @phase generate-sources
 * @requiresProject true
 */
public class SwiftMojo extends AbstractMojo
{
    private static final Logger LOG = Logger.getLogger(SwiftMojo.class);

    /**
     * Skip the plugin execution.
     *
     * @parameter default-value="false"
     */
    private boolean skip = false;

    /**
     * Target java package for the generated classes. If null, the java
     * namespace from the idls is used.
     *
     * @parameter
     */
    private String packageName = null;

    /**
     * IDL files to process.
     *
     * @parameter
     * @required
     */
    private FileSet idlFiles;

    /**
     * Output folder
     *
     * @parameter default-value="${project.build.directory}/generated-sources/swift"
     * @required
     */
    private File outputFolder = null;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (!skip) {

                final File inputFolder = new File(idlFiles.getDirectory());

                @SuppressWarnings("unchecked")
                List<File> files = FileUtils.getFiles(inputFolder,
                                                      StringUtils.join(idlFiles.getIncludes(), ','),
                                                      StringUtils.join(idlFiles.getExcludes(), ','));
                final SwiftGeneratorConfig config = new SwiftGeneratorConfig(inputFolder, files.toArray(new File [files.size()]), outputFolder, packageName);
                final SwiftGenerator generator = new SwiftGenerator(config);
                generator.parse();

                project.addCompileSourceRoot(outputFolder.getPath());
            }
        }
        catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, MojoExecutionException.class);
            Throwables.propagateIfInstanceOf(e, MojoFailureException.class);

            LOG.error(String.format("While executing Mojo %s", this.getClass().getSimpleName()), e);
            throw new MojoExecutionException("Failure:" ,e);
        }
        finally {
            MavenLogAppender.endPluginLog(this);
        }
    }
}
