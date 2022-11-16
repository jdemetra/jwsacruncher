package _test;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

@lombok.experimental.UtilityClass
public class DependencyResolver {

    private static final String PREFIX = "   ";
    private static final String SUFFIX = " -- module";

    public static List<MavenCoordinates> parseCoordinates(Reader reader) {
        return asBufferedReader(reader).lines()
                .filter(DependencyResolver::isValidLine)
                .map(DependencyResolver::removeAnsiControlChars)
                .map(DependencyResolver::extract)
                .map(MavenCoordinates::parse)
                .collect(Collectors.toList());
    }

    private static BufferedReader asBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    private static boolean isValidLine(String line) {
        return line.startsWith(PREFIX);
    }

    private static String removeAnsiControlChars(String input) {
        return input.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private static String extract(String input) {
        int start = PREFIX.length();
        int end = input.indexOf(SUFFIX, start);
        return input.substring(start, end == -1 ? input.length() : end);
    }
}
