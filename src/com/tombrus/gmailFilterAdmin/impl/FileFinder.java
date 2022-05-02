package com.tombrus.gmailFilterAdmin.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileFinder {
    public record FilePair(Path csv, Path xml) {
    }

    public static List<FilePair> find(Path rootDir) throws IOException {
        try (Stream<Path> files = Files.list(rootDir)) {
            return files
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().matches("^mailFilters.*[.](csv|xml)$"))
                    .map(f -> new FilePair(giveExtention(f, "csv"), giveExtention(f, "xml")))
                    .distinct()
                    .toList();
        }
    }

    private static Path giveExtention(Path f, String ext) {
        return f.getParent().resolve(f.getFileName().toString().replaceAll("[.](csv|xml)$", "") + "." + ext);
    }
}
