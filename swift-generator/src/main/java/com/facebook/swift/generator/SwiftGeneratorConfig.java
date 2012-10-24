package com.facebook.swift.generator;

public class SwiftGeneratorConfig
{
    private final String inputFolderName;
    private final String [] inputFiles;
    private final String outputFolderName;
    private final String defaultPackage;

    public SwiftGeneratorConfig(final String inputFolderName,
                final String [] inputFiles,
                final String outputFolderName,
                final String defaultPackage)
    {
        this.inputFolderName = inputFolderName;
        this.inputFiles = inputFiles;
        this.outputFolderName = outputFolderName;
        this.defaultPackage = defaultPackage;
    }

    public String getInputFolderName()
    {
        return inputFolderName;
    }

    public String[] getInputFiles()
    {
        return inputFiles;
    }

    public String getOutputFolderName()
    {
        return outputFolderName;
    }

    public String getDefaultPackage()
    {
        return defaultPackage;
    }
}
