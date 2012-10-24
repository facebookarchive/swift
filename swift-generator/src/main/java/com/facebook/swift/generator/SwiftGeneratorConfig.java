package com.facebook.swift.generator;

import java.io.File;

public class SwiftGeneratorConfig
{
    private final File inputFolder;
    private final File [] inputFiles;
    private final File outputFolder;
    private final String defaultPackage;

    public SwiftGeneratorConfig(final File inputFolder,
                                final File [] inputFiles,
                                final File outputFolder,
                                final String defaultPackage)
    {
        this.inputFolder = inputFolder;
        this.inputFiles = inputFiles;
        this.outputFolder = outputFolder;
        this.defaultPackage = defaultPackage;
    }

    public File getInputFolder()
    {
        return inputFolder;
    }

    public File[] getInputFiles()
    {
        return inputFiles;
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

    public String getDefaultPackage()
    {
        return defaultPackage;
    }
}
