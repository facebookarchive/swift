package com.facebook.swift.generator;

public class SwiftGeneratorConfig
{
    private final String inputFolderName;
    private final String [] inputFiles;
    private final String outputFolderName;

    public SwiftGeneratorConfig(final String inputFolderName,
                final String [] inputFiles,
                final String outputFolderName)
    {
        this.inputFolderName = inputFolderName;
        this.inputFiles = inputFiles;
        this.outputFolderName = outputFolderName;
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
}
