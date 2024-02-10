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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
    private static final char COMMENT_MARKER = '#';
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder().setQuoteMode(QuoteMode.MINIMAL)
            .setEscape('\\').setCommentMarker(COMMENT_MARKER).setQuote('\"').setTrim(true).build();

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
                "       AnkiTable -batch directory/containing/csvfiles directory/to/save/html/files" +
                "                --OR--\n" +
                "       AnkiTable -file path/to/file.csv path/to/outputFile.html");
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
        List<String> allLines = Files.readAllLines(csvFile);
        String title = null;
        if (!allLines.isEmpty()) {
            String firstLine = allLines.get(0);
            if (firstLine.startsWith(COMMENT_MARKER + "title=")) {
                title = firstLine.substring(7).trim();
            }
        }
        if (title == null) {
            title = deriveTitle(outputFile);
        }

        oneFile(csvFile, outputFile, title);
    }

    private static @NotNull String deriveTitle(Path outputFile) {
        return fileNameWithoutExtension(outputFile);
    }

    private static @NotNull String fileNameWithoutExtension(@NotNull Path outputFile) {
        String fileName = outputFile.getFileName().toString();
        return fileName.substring(0, fileName.indexOf(".html"));
    }

    private static void oneFile(Path csvFile, Path outputFile, String title) throws IOException {
        System.out.println("csvFile = " + csvFile + ", outputFile = " + outputFile + ", title = " + title);
        List<CSVRecord> records;
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            CSVParser parser = CSV_FORMAT.parse(reader);
            records = parser.getRecords();
        }

        int numColumns = numColumns(records).orElseThrow(() -> new IllegalArgumentException("inconsistent number of columns for file " + csvFile));

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
             PrintWriter printWriter = new PrintWriter(writer)) {

            printWriter.println(STYLE);
            printWriter.println("<div>" + title + "</div>\n");
            printWriter.println("<table class=\"fred\">\n");
            printWriter.println("<tr>\n");
            CSVRecord heading = records.get(0);
            for (int i = 0; i < numColumns; i++) {
                printWriter.println("  <th>" + escapeHtml(escapeClozeDeletion(unwrapDoubleQuotes(heading.get(i)))) + "</th>\n");
            }

            int clozeNum = 1;

            for (int i = 1; i < records.size(); i++) {
                printWriter.println("<tr>\n");
                for (int j = 0; j < numColumns; j++) {
                    String cellValue = records.get(i).get(j);
                    if (cellValue.startsWith("\u00BF")) {   // "¿"
//                        clozeNum++;
                        String cellValueAfterFirstChar = cellValue.substring(1);
                        printWriter.println("  <td>" + escapeHtml(cellValueAfterFirstChar) + "</td>\n");
                    } else {
                        String s = unwrapDoubleQuotes(cellValue);
                        String foo = interpretSpecialMarkupToBreakLines(escapeHtml(escapeClozeDeletion(unwrapDoubleQuotes(s))));
                        printWriter.println("  <td>{{c" + clozeNum++ + "::" + foo + "}}</td>\n");
                    }
                }
                printWriter.println("</tr>\n");
            }
            printWriter.println("</table>\n");
        }
        System.out.println("wrote file: " + outputFile.toAbsolutePath());
    }

    static @NotNull String interpretSpecialMarkupToBreakLines(@NotNull String str) {
        if (str.contains("\u00c2\u00a1")) {
            return str.replace("\u00c2\u00a1", "<br />");
        } else if (str.contains("\u00a1")) {
            return str.replace("\u00a1", "<br />");
        }
        return str;
    }

    static @NotNull String unwrapDoubleQuotes(@NotNull String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static @NotNull String escapeHtml(@NotNull String s) {
        return s.replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Contract(pure = true)
    private static @NotNull String escapeClozeDeletion(@NotNull String s) {
        return s.replace("::", ":\u200B:");
    }

    private static OptionalInt numColumns(@NotNull List<CSVRecord> records) {
        int toReturn = -1;
        int lineNum = 0;
        for (CSVRecord csvRecord : records) {
            lineNum++;
            int currentRecordSize = csvRecord.size();
            if (toReturn == -1) {
                toReturn = currentRecordSize;
                continue;
            }
            if (currentRecordSize != toReturn) {
                System.out.println("at line " + lineNum + ": currentRecordSize " + currentRecordSize + " does not match " + toReturn);
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(toReturn);
    }
}