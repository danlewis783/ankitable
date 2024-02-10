/*
 * This file is part of the AnkiTable distribution (https://github.com/danlewis783/ankitable).
 * Copyright © 2024 Dan Lewis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ankitable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.MINIMAL)
            .setEscape('\\').setCommentMarker('#').setQuote('\"').setTrim(true).build();

    private static final String STYLE = "<style>\n" +
            "table.fred {\n" +
            "  margin: auto;\n" +
            "  border-collapse: collapse;\n" +
            "}\n" +
            "table.fred,\n" +
            "table.fred th,\n" +
            "table.fred td {\n" +
            "  border: 1px solid white;\n" +
            "}\n" +
            "table.fred th,\n" +
            "table.fred td {\n" +
            "  padding: 10px;\n" +
            "  text-align: left;\n" +
            "}\n" +
            "</style>\n";


    public static void main(String... args) throws IOException {
        if (args.length == 3) {
            if (args[0].equals("-batch")) {
                batch(args[1], args[2]);
            } else if (args[0].equals("-file")) {
                oneFile(args[1], args[2]);
            } else {
                printUsage(args);
            }
        } else if (args.length == 0) {
            batch();
        } else {
            printUsage(args);
        }
    }

    private static void printUsage(String[] args) {
        System.out.println("received args: " + Arrays.toString(args));
        System.err.println(
                "usage: " +
                "       AnkiTable\n" +
                "                --OR--\n" +
                "       AnkiTable -batch directory/containing/csvfiles -output directory/to/save/html/files" +
                "                --OR--\n" +
                "       AnkiTable -file path/to/file.csv -file path/to/outputFile.html");
    }

    private static void batch() throws IOException {
        // Use $home/anki-flashcards as the default directory to read .csv files and generate .html files.
        // Create the $home/anki-flashcards directory if it does not exist.
        String userHomeProperty = System.getProperty("user.home");
        String defaultAnkiFlashcardsDirectory = "anki-flashcards";
        Path defaultWorkingDir = Paths.get(userHomeProperty, defaultAnkiFlashcardsDirectory);
        Path defaultWorkingDirCreated = Files.createDirectories(defaultWorkingDir);
        batch(defaultWorkingDirCreated, defaultWorkingDirCreated);
    }

    private static void batch(String csvFilesDirStr, String outputFilesDirStr) throws IOException {
        batch(Paths.get(csvFilesDirStr), Paths.get(outputFilesDirStr));
    }

    private static void batch(Path csvFilesDir, Path outputFilesDir) throws IOException {
        if (! Files.exists(csvFilesDir)) {
            throw new RuntimeException("csv files dir does not exist");
        }
        if (! Files.isDirectory(csvFilesDir)) {
            throw new RuntimeException("csv files dir is not a directory");
        }
        Files.createDirectories(outputFilesDir);
        if (! Files.exists(outputFilesDir)) {
            throw new RuntimeException("output files dir does not exist");
        }
        if (! Files.isDirectory(outputFilesDir)) {
            throw new RuntimeException("output files dir is not a directory");
        }
        Set<Path> csvFiles;
        try (Stream<Path> stream = Files.list(csvFilesDir)) {
            csvFiles = stream.filter(file -> !Files.isDirectory(file))
                    .filter(file -> file.getFileName().toString().endsWith(".csv"))
                    .collect(Collectors.toSet());
        }
        for (Path file : csvFiles) {
            Path outputFile = deriveOutputFileFrom(file, outputFilesDir);
            oneFile(file, outputFile);
        }
    }

    private static @NotNull Path deriveOutputFileFrom(Path file) {
        return deriveOutputFileFrom(file, file.getParent());
    }

    private static @NotNull Path deriveOutputFileFrom(@NotNull Path file, @NotNull Path dir) {
        String outputFileName = file.getFileName().toString().replace(".csv", ".html");
        return dir.resolve(outputFileName);
    }

    private static void oneFile(String csvFileName, String outputFileName) throws IOException {
        Path csvFile = Paths.get(csvFileName);
        Path outputFile = Paths.get(outputFileName);
        oneFile(csvFile, outputFile);
    }

    private static void oneFile(Path csvFile, Path outputFile) throws IOException {
        String title = deriveTitle(outputFile);
        oneFile(csvFile, outputFile, title);
    }

    private static String deriveTitle(Path outputFile) {
        return fileNameWithoutExtension(outputFile);
    }

    private static String fileNameWithoutExtension(Path outputFile) {
        String fileName = outputFile.getFileName().toString();
        String noExtension = fileName.substring(0, fileName.indexOf(".html"));
        return noExtension;
    }

    private static void oneFile(Path csvFile, Path outputFile, String title) throws IOException {
        System.out.println("csvFile = " + csvFile + ", outputFile = " + outputFile + ", title = " + title);
        List<CSVRecord> records;
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            CSVParser parser = CSV_FORMAT.parse(reader);
            records = parser.getRecords();
        }

        int numColumns = numColumns(records).orElseThrow(() -> new IllegalArgumentException("inconsistent number of columns for file " + csvFile));

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile);
             PrintWriter printWriter = new PrintWriter(writer)) {

            printWriter.println(STYLE);
            printWriter.println("<div>" + title + "</div>\n");
            printWriter.println("<table class=\"fred\">\n");
            printWriter.println("<tr>\n");
            CSVRecord heading = records.get(0);
            for (int i = 0; i < numColumns; i++) {
                printWriter.println("  <th>" + escapeHtmlAndClozeDeletion(heading.get(i)) + "</th>\n");
            }

            int clozeNum = 1;

            for (int i = 1; i < records.size(); i++) {
                printWriter.println("<tr>\n");
                for (int j = 0; j < numColumns; j++) {
                    printWriter.println("  <td>{{c" + clozeNum++ + "::" + escapeHtmlAndClozeDeletion(records.get(i).get(j)) + "}}</td>\n");
                }
                printWriter.println("</tr>\n");
            }
            printWriter.println("</table>\n");
        }
        System.out.println("wrote file: " + outputFile.toAbsolutePath());
    }

    private static String escapeHtmlAndClozeDeletion(String s) {
        return escapeHtml(escapeClozeDeletion(s));
    }

    private static String escapeHtml(String s) {
        return s.replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeClozeDeletion(String s) {
        return s.replace("::", ":\u200B:");
    }

    private static OptionalInt numColumns(List<CSVRecord> records) {
        int toReturn = -1;
        for (CSVRecord csvRecord : records) {
            int currentRecordSize = csvRecord.size();
            if (toReturn == -1) {
                toReturn = currentRecordSize;
                continue;
            }
            if (currentRecordSize != toReturn) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(toReturn);
    }
}