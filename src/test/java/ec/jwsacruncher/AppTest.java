/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package ec.jwsacruncher;

import _test.DependencyResolver;
import _test.MavenCoordinates;
import nbbrd.io.text.TextParser;
import nbbrd.io.xml.bind.Jaxb;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void testRuntimeDependencies() throws IOException {
        assertThat(getRuntimeDependencies())
                .describedAs("Check runtime dependencies")
                .satisfies(AppTest::checkDemetra)
                .satisfies(AppTest::checkJavaSqlUtil)
                .satisfies(AppTest::checkJavaIoUtil)
                .satisfies(AppTest::checkSpreadsheet4j)
                .satisfies(AppTest::checkSlf4j)
                .satisfies(AppTest::checkLog4j)
                .satisfies(AppTest::checkGuava);
    }

    private static void checkGuava(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check guava dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "com.google.guava")
                .isNotEmpty()
                .extracting(MavenCoordinates::getArtifactId)
                .contains("guava")
                .hasSize(3);
    }

    private static void checkLog4j(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check log4j dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "org.apache.logging.log4j")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .containsExactlyInAnyOrder("log4j-api", "log4j-to-slf4j");
    }

    private static void checkSlf4j(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check slf4j dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "org.slf4j")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .containsExactlyInAnyOrder("slf4j-api", "slf4j-jdk14");
    }

    private static void checkSpreadsheet4j(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check spreadsheet4j dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "com.github.nbbrd.spreadsheet4j")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .containsExactlyInAnyOrder(
                        "spreadsheet-api",
                        "spreadsheet-html",
                        "spreadsheet-od",
                        "spreadsheet-poi",
                        "spreadsheet-util",
                        "spreadsheet-xl",
                        "spreadsheet-xmlss");
    }

    private static void checkJavaIoUtil(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check java-io-util dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "com.github.nbbrd.java-io-util")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .contains("java-io-win")
                .hasSize(4);
    }

    private static void checkJavaSqlUtil(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check java-sql-util dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "com.github.nbbrd.java-sql-util")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .contains("java-sql-lhod")
                .hasSize(3);
    }

    private static void checkDemetra(List<? extends MavenCoordinates> coordinates) {
        assertThat(coordinates)
                .describedAs("Check demetra dependencies")
                .filteredOn(MavenCoordinates::getGroupId, "eu.europa.ec.joinup.sat")
                .isNotEmpty()
                .has(sameVersion())
                .extracting(MavenCoordinates::getArtifactId)
                .containsExactlyInAnyOrder(
                        "demetra-common",
                        "demetra-jdbc",
                        "demetra-odbc",
                        "demetra-sdmx",
                        "demetra-spreadsheet",
                        "demetra-tss",
                        "demetra-tstoolkit",
                        "demetra-utils",
                        "demetra-workspace"
                );
    }

    @Test
    public void testGenerateDefaultConfigFile(@TempDir Path temp) throws IOException {
        App.generateDefaultConfigFile(temp.toFile());

        assertThat(temp.resolve(WsaConfig.DEFAULT_FILE_NAME))
                .hasContent(writeConfigToString(WsaConfig.generateDefault()));
    }

    private static String writeConfigToString(WsaConfig config) throws IOException {
        return Jaxb.Formatter.of(WsaConfig.class).withFormatted(true).formatToString(config);
    }


    private static Condition<List<? extends MavenCoordinates>> sameVersion() {
        return new Condition<>(AppTest::sameVersion, "same version");
    }

    private static boolean sameVersion(List<? extends MavenCoordinates> list) {
        return list
                .stream()
                .map(MavenCoordinates::getVersion)
                .distinct()
                .count() == 1;
    }

    private static List<MavenCoordinates> getRuntimeDependencies() throws IOException {
        return TextParser.onParsingReader(DependencyResolver::parseCoordinates)
                .parseResource(AppTest.class, "/runtime-dependencies.txt", UTF_8);
    }
}
