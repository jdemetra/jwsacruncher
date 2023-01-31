package ec.jwsacruncher;

import _test.DependencyResolver;
import nbbrd.io.text.TextParser;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeDependenciesTest {

    @Test
    public void testRuntimeDependencies() throws IOException {
        assertThat(getRuntimeDependencies())
                .describedAs("Check runtime dependencies")
                .satisfies(RuntimeDependenciesTest::checkDemetra)
                .satisfies(RuntimeDependenciesTest::checkJavaSqlUtil)
                .satisfies(RuntimeDependenciesTest::checkJavaIoUtil)
                .satisfies(RuntimeDependenciesTest::checkSpreadsheet4j)
                .satisfies(RuntimeDependenciesTest::checkSlf4j)
                .satisfies(RuntimeDependenciesTest::checkLog4j)
                .satisfies(RuntimeDependenciesTest::checkGuava);
    }

    private static void checkGuava(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "com.google.guava")
                .extracting(DependencyResolver.GAV::getArtifactId)
                .contains("guava")
                .hasSize(3);
    }

    private static void checkLog4j(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "org.apache.logging.log4j")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
                .containsExactlyInAnyOrder("log4j-api", "log4j-to-slf4j");
    }

    private static void checkSlf4j(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "org.slf4j")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
                .containsExactlyInAnyOrder("slf4j-api", "slf4j-jdk14");
    }

    private static void checkSpreadsheet4j(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.spreadsheet4j")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
                .containsExactlyInAnyOrder(
                        "spreadsheet-api",
                        "spreadsheet-html",
                        "spreadsheet-od",
                        "spreadsheet-poi",
                        "spreadsheet-util",
                        "spreadsheet-xl",
                        "spreadsheet-xmlss");
    }

    private static void checkJavaIoUtil(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.java-io-util")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
                .contains("java-io-win")
                .hasSize(4);
    }

    private static void checkJavaSqlUtil(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "com.github.nbbrd.java-sql-util")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
                .contains("java-sql-lhod")
                .hasSize(3);
    }

    private static void checkDemetra(List<? extends DependencyResolver.GAV> coordinates) {
        assertThatGroupId(coordinates, "eu.europa.ec.joinup.sat")
                .has(sameVersion())
                .extracting(DependencyResolver.GAV::getArtifactId)
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

    private static ListAssert<? extends DependencyResolver.GAV> assertThatGroupId(List<? extends DependencyResolver.GAV> coordinates, String groupId) {
        return assertThat(coordinates)
                .describedAs("Check " + groupId)
                .filteredOn(DependencyResolver.GAV::getGroupId, groupId);
    }

    private static Condition<List<? extends DependencyResolver.GAV>> sameVersion() {
        return new Condition<>(DependencyResolver.GAV::haveSameVersion, "same version");
    }

    private static List<DependencyResolver.GAV> getRuntimeDependencies() throws IOException {
        return TextParser.onParsingReader(reader -> DependencyResolver.parse(asBufferedReader(reader).lines()))
                .parseResource(AppTest.class, "/runtime-dependencies.txt", UTF_8);
    }

    private static BufferedReader asBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }
}
