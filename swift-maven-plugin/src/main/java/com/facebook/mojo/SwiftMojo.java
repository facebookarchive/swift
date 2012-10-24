package com.facebook.mojo;

import com.google.common.base.Throwables;
import com.pyx4j.log4j.MavenLogAppender;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Process one or more idl files and write java code.
 *
 * @requiresProject true
 * @goal generate
 * @phase generate-sources
 */
public class SwiftMojo extends AbstractMojo
{
    private static final Logger LOG = Logger.getLogger(SwiftMojo.class);

    /**
     * Skip the plugin execution.
     *
     * <pre>
     *   <configuration>
     *     <skip>true</skip>
     *   </configuration>
     * </pre>
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

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (!skip) {
                LOG.info("Hello, World!");

                @SuppressWarnings("unchecked")
                List<File> files = FileUtils.getFiles(new File(idlFiles.getDirectory()),
                                                      StringUtils.join(idlFiles.getIncludes(), ','),
                                                      StringUtils.join(idlFiles.getExcludes(), ','));

                LOG.info("Files: " + files);
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
