package _test;

import lombok.NonNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@lombok.experimental.UtilityClass
public class DependencyResolver {

    public static @NonNull List<GAV> parse(@NonNull Stream<String> lines) {
        return lines
                .filter(DependencyResolver::isValidLine)
                .map(DependencyResolver::removeAnsiControlChars)
                .map(DependencyResolver::extract)
                .map(GAV::parse)
                .collect(Collectors.toList());
    }

    private static final String PREFIX = "   ";
    private static final String SUFFIX = " -- module";

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

    @lombok.Value
    public static class GAV {

        public static @NonNull GAV parse(@NonNull CharSequence input) {
            String[] items = input.toString().split(":", -1);
            if (items.length < 4) {
                throw new IllegalArgumentException("Invalid GAV: '" + input + "'");
            }
            return new GAV(items[0], items[1], items[3]);
        }

        @NonNull String groupId;
        @NonNull String artifactId;
        @NonNull String version;

        public static boolean haveSameVersion(@NonNull List<? extends GAV> list) {
            return list
                    .stream()
                    .map(GAV::getVersion)
                    .distinct()
                    .count() == 1;
        }
    }
}
