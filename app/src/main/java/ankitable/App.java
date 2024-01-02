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
import java.util.List;
import java.util.OptionalInt;

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
        if (args.length < 1) {
            System.err.println("usage: AnkiTable path/to/file.csv path/to/outputFile.html");
            return;
        }
        Path csvFile = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);
        List<CSVRecord> records;
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            CSVParser parser = CSV_FORMAT.parse(reader);
            records = parser.getRecords();
        }

        int numColumns = numColumns(records).orElseThrow(() -> new IllegalArgumentException("inconsistent number of columns"));

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile);
             PrintWriter printWriter = new PrintWriter(writer)) {

            printWriter.println(STYLE);
            printWriter.println("<div>title</div>\n");
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