package com.facebook.swift.swiftify;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.facebook.swift.parser.ThriftIdlParser;
import com.facebook.swift.parser.model.Document;
import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class swiftify
{
    @Parameter(description = "Location to write Swift-annotated Java source idlFiles",
               names = { "-o", "--out" })
    public String outputDir = ".";

    @Parameter(description = "Thrift IDL files to parse")
    private List<String> idlFiles = new ArrayList<String>();

    @Parameter(description = "Show this help message", names = { "-h", "--help" })
    private boolean help = false;

    public static void main(String[] args) throws Exception {
        swiftify app = new swiftify();
        try {
            JCommander parser = new JCommander(app, args);
            parser.setProgramName(swiftify.class.getSimpleName());

            if (app.help) {
                parser.usage();
            } else if (app.idlFiles.size() == 0) {
                parser.usage();
            } else {
                app.run();
            }
        } catch (ParameterException e) {
            new JCommander(app, args).usage();
        }
    }

    private void usage(JCommander parser) {
        parser.usage(swiftify.class.getSimpleName().toLowerCase());
    }

    private void run()
            throws IOException
    {
        File generatedOutputRoot = new File(outputDir);

        SwiftClassGenerator generator = new SwiftClassGenerator(generatedOutputRoot.toString());
        for (String idlFile : idlFiles) {
            InputSupplier<InputStreamReader> readerSupplier =
                    Resources.newReaderSupplier(new File(idlFile).toURL(), Charsets.UTF_8);
            Document doc = ThriftIdlParser.parseThriftIdl(readerSupplier);
            generator.generateFromDocument(doc);
        }
    }
}
