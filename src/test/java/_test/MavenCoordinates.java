package _test;

import lombok.NonNull;

@lombok.Value
public class MavenCoordinates {

    public static MavenCoordinates parse(CharSequence input) {
        String[] items = input.toString().split(":", -1);
        return new MavenCoordinates(items[0], items[1], items[3]);
    }

    @NonNull String groupId;
    @NonNull String artifactId;
    @NonNull String version;
}
