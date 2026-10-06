/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * nifi2md - documents an Apache NiFi flow as Markdown with Mermaid diagrams.
 */
package fr.giwi.nifi2md;

import fr.giwi.nifi2md.model.FlowParser;
import fr.giwi.nifi2md.render.MarkdownGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * nifi2md: parses a NiFi flow definition JSON file and writes Markdown documentation,
 * one file per process group, cross-linked from a main {@code index.md}.
 */
@Command(
        name = "nifi2md",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        descriptionHeading = "%nDescription:%n",
        optionListHeading = "%nOptions:%n",
        parameterListHeading = "%nParameters:%n",
        header = "nifi2md - document an Apache NiFi flow as Markdown with Mermaid diagrams.",
        description = {
                "Parses a NiFi flow definition (the JSON exported from the NiFi UI or stored in a",
                "versioned flow) and generates a documentation set: a main index file, plus one",
                "file per process group containing a Mermaid diagram, processors, connections,",
                "ports and controller services, annotated with Enterprise Integration Patterns."
        },
        version = "nifi2md 0.1.0")
public final class Main implements Callable<Integer> {

    static final int EXIT_OK = 0;
    static final int EXIT_ERROR = 1;

    @Parameters(index = "0", paramLabel = "<flow.json>",
            description = "Path to the NiFi flow definition JSON file.")
    private Path input;

    @Option(names = {"-o", "--output"}, paramLabel = "<dir>",
            description = "Output directory for the generated files. "
                    + "Defaults to <input name>-docs next to the input file.")
    private Path output;

    @Option(names = {"-t", "--title"}, paramLabel = "<title>",
            description = "Documentation title. Defaults to the root process group name.")
    private String title;

    @Option(names = {"-d", "--direction"}, paramLabel = "<dir>",
            defaultValue = "LR", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            description = "Mermaid layout direction: LR (default), RL, TB, BT.")
    private String direction;

    @Option(names = "--no-diagrams",
            description = "Skip the Mermaid diagrams.")
    private boolean noDiagrams;

    @Option(names = "--no-properties",
            description = "Skip component property tables.")
    private boolean noProperties;

    @Option(names = "--max-property-length", paramLabel = "<n>",
            defaultValue = "200", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            description = "Truncate property values to n characters (0 = no truncation).")
    private int maxPropertyLength;

    @Option(names = {"-q", "--quiet"}, description = "Do not print the summary of written files.")
    private boolean quiet;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() {
        try {
            return run();
        } catch (CommandLine.ParameterException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("nifi2md: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    private int run() throws IOException {
        if (!Files.isRegularFile(input)) {
            System.err.println("nifi2md: no such file: " + input);
            return EXIT_ERROR;
        }
        if (!directionMatches()) {
            System.err.println("nifi2md: unknown direction '" + direction
                    + "' (expected LR, RL, TB, TD or BT)");
            return EXIT_ERROR;
        }

        FlowParser.FlowData data = new FlowParser().parse(input);
        MarkdownGenerator.Options options = new MarkdownGenerator.Options(
                title,
                !noDiagrams,
                !noProperties,
                maxPropertyLength,
                direction);
        MarkdownGenerator.Document doc = new MarkdownGenerator(options)
                .generate(data, input.getFileName().toString());

        Path target = output != null ? output : defaultOutputDir();
        writeAll(target, doc);

        if (!quiet) {
            System.out.println("Wrote " + doc.fileCount() + " files to " + target.toAbsolutePath());
            doc.files().forEach((path, content) ->
                    System.out.println("  " + path + " (" + content.length() + " chars)"));
            System.out.println("Open: " + target.resolve(doc.mainFileName()).toAbsolutePath());
        }
        return EXIT_OK;
    }

    private boolean directionMatches() {
        String d = direction == null ? "" : direction.trim().toUpperCase(Locale.ROOT);
        return d.equals("LR") || d.equals("RL") || d.equals("TB") || d.equals("TD") || d.equals("BT");
    }

    private Path defaultOutputDir() {
        String fileName = input.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        Path parent = input.toAbsolutePath().getParent();
        return (parent == null ? Path.of(".") : parent).resolve(stem + "-docs");
    }

    private void writeAll(Path target, MarkdownGenerator.Document doc) throws IOException {
        Files.createDirectories(target);
        Map<String, Path> written = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : doc.files().entrySet()) {
            Path file = target.resolve(e.getKey());
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, e.getValue(), StandardCharsets.UTF_8);
            written.put(e.getKey(), file);
        }
    }
}
