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


    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "usage: AnkiTable path/to/file.csv path/to/outputFile.html\n" +
                    "                --OR--\n" +
                    "       AnkiTable -batch directory/containing/csvfiles");
            return;
        }
        System.out.println(Arrays.toString(args));
        if (args[0].equals("-batch")) {
            batch(args[1]);
        } else {
            extracted(args[0], args[1]);
        }
    }


    private static void batch(String csvFilesDirStr) throws IOException {
        Path csvFilesDir = Paths.get(csvFilesDirStr);
        batch(csvFilesDir);
    }

    private static void batch(Path csvFilesDir) throws IOException {
        if (! Files.exists(csvFilesDir)) {
            throw new RuntimeException("does not exist");
        }
        if (! Files.isDirectory(csvFilesDir)) {
            throw new RuntimeException("not a directory");
        }
        Set<Path> csvFiles;
        try (Stream<Path> stream = Files.list(csvFilesDir)) {
            csvFiles = stream.filter(file -> !Files.isDirectory(file)).filter(file -> file.getFileName().toString().endsWith(".csv")).collect(Collectors.toSet());
        }
        for (Path file : csvFiles) {
            Path outputFile = deriveOutputFileFrom(file);
            extracted(file, outputFile);
        }
    }

    private static Path deriveOutputFileFrom(Path file) {
        String outputFileName = file.getFileName().toString().replace(".csv", ".html");
        Path outputDir = file.getParent();
        return outputDir.resolve(outputFileName);
    }

    private static void extracted(String csvFileName, String outputFileName) throws IOException {
        Path csvFile = Paths.get(csvFileName);
        Path outputFile = Paths.get(outputFileName);
        extracted(csvFile, outputFile);
    }

    private static void extracted(Path csvFile, Path outputFile) throws IOException {
        String title = deriveTitle(outputFile);
        extracted(csvFile, outputFile, title);
    }

    private static String deriveTitle(Path outputFile) {
        return fileNameWithoutExtension(outputFile);
    }

    private static String fileNameWithoutExtension(Path outputFile) {
        String fileName = outputFile.getFileName().toString();
        String noExtension = fileName.substring(0, fileName.indexOf(".html"));
        return noExtension;
    }

    private static void extracted(Path csvFile, Path outputFile, String title) throws IOException {
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
                printWriter.println("  <th>" + heading.get(i) + "</th>\n");
            }

            int clozeNum = 1;

            for (int i = 1; i < records.size(); i++) {
                printWriter.println("<tr>\n");
                for (int j = 0; j < numColumns; j++) {
                    printWriter.println("  <td>{{c" + clozeNum++ + "::" + escapeClozeDeletion(records.get(i).get(j)) + "}}</td>\n");
                }
                printWriter.println("</tr>\n");
            }
            printWriter.println("</table>\n");
        }
        System.out.println("wrote file: " + outputFile.toAbsolutePath());
    }

    private static String escapeClozeDeletion(String s) {
        String s2 = s.replace("<", "&lt;");
        String s3 = s2.replace(">", "&gt;");
        String s4 = s3.replace("::", ":\u200B:");
        return s4;
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