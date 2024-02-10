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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;


final class AppTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS) //NOTE: if you want to see the output files even when test passes, change CleanupMode
    private static Path tempDir;

    private static Path testResourcesPath;
    private static Path actualOutputTempPath;

    @BeforeAll
    static void beforeAll() throws IOException {
        testResourcesPath = Paths.get(System.getProperty("test.resources"));
        actualOutputTempPath = Files.createTempFile(tempDir, "happy-output-actual", ".html");
    }

    @Test
    void happy() throws IOException {
        Path inputPath = testResourcesPath.resolve("happy-input.csv");

        String inputPathStr = inputPath.toAbsolutePath().toString();
        String outputPathStr = actualOutputTempPath.toAbsolutePath().toString();
        System.out.println(inputPathStr);
        System.out.println(outputPathStr);
        App.main("-file", inputPathStr, outputPathStr);

        Document actualDoc = Jsoup.parse(actualOutputTempPath.toFile(), "UTF-8");
        io.github.ulfs.assertj.jsoup.Assertions.assertThat(actualDoc).elementExists("tr", 4);
        Elements rows = actualDoc.select("tr");
        assertThat(rows.size()).isEqualTo(4);
        assertThat(rows.get(0).select("th").size()).isEqualTo(3);
        assertThat(rows.get(1).select("td").size()).isEqualTo(3);
        assertThat(rows.get(2).select("td").size()).isEqualTo(3);
        assertThat(rows.get(3).select("td").size()).isEqualTo(3);

        assertThat(rows.get(0).select("th").get(0).text()).isEqualTo("HeadingA");
        assertThat(rows.get(0).select("th").get(1).html()).isEqualTo("Heading&lt;B&gt;");
        assertThat(rows.get(0).select("th").get(2).text()).isEqualTo("HeadingC");

        assertThat(rows.get(1).select("td").get(0).text()).isEqualTo("{{c1::data1a}}");
        assertThat(rows.get(1).select("td").get(1).text()).isEqualTo("{{c2::data1b}}");
        assertThat(unFrack(rows.get(1).select("td").get(2).html())).isEqualTo("{{c3::data1c-1<br />data1c-2}}");

        assertThat(unFrack(rows.get(2).select("td").get(0).html())).isEqualTo("{{c4::data2a-1<br />data2a-2<br />data2a-3}}");
        assertThat(rows.get(2).select("td").get(1).text()).isEqualTo("{{c5::data2b}}");
        assertThat(rows.get(2).select("td").get(2).html()).isEqualTo("{{c6::data2c:\u200B:foo}}");

        assertThat(rows.get(3).select("td").get(0).text()).isEqualTo("{{c7::data3a}}");
        assertThat(rows.get(3).select("td").get(1).text()).isEqualTo("data3b");
        assertThat(rows.get(3).select("td").get(2).text()).isEqualTo("{{c9::data3c}}");
    }

    @Test
    void interpretSpecialMarkupToBreakLines() {
        assertThat(App.interpretSpecialMarkupToBreakLines("¡")).isEqualTo("<br />");
        assertThat(App.interpretSpecialMarkupToBreakLines("\u00A1")).isEqualTo("<br />");
        assertThat(App.interpretSpecialMarkupToBreakLines("")).isEqualTo("");
    }

    private static String unFrack(String str) {
        return str.replace("<br>\n", "<br />");
    }

    @Test
    void unwrapDoubleQuotes() {
        assertThat(App.unwrapDoubleQuotes("\"foo\"")).isEqualTo("foo");
        assertThat(App.unwrapDoubleQuotes("\"bar")).isEqualTo("\"bar");
        assertThat(App.unwrapDoubleQuotes("")).isEqualTo("");
        assertThatNullPointerException().isThrownBy(() -> App.unwrapDoubleQuotes(null));
    }
}
