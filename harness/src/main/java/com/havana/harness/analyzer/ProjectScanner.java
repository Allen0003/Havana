package com.havana.harness.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectScanner {

    private final Path projectRoot;

    // 要掃描的檔案類型
    private static final Set<String> INCLUDED_EXTENSIONS = Set.of(
            ".java",
            ".ts",
            ".html",
            ".css",
            ".scss",
            ".json",
            ".xml",
            ".md"
    );

    // 不掃描這些目錄，避免讀取大量無關檔案
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            ".idea",
            "node_modules",
            "target",
            "dist",
            ".angular",
            "harness"
    );

    public ProjectScanner(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    public String scan() throws IOException {

        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalStateException(
                    "Project root not found: " + projectRoot
            );
        }

        try (Stream<Path> files = Files.walk(projectRoot)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isIncludedFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::readFile)
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private boolean isIncludedFile(Path path) {

        Path relativePath = projectRoot.relativize(path);

        // 排除指定目錄
        for (Path part : relativePath) {
            if (EXCLUDED_DIRECTORIES.contains(part.toString())) {
                return false;
            }
        }

        String fileName = path.getFileName().toString();

        return INCLUDED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
    }

    private String readFile(Path path) {
        try {
            String relativePath = projectRoot
                    .relativize(path)
                    .toString();

            return "\n===== FILE: " + relativePath + " =====\n"
                    + Files.readString(path);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read file: " + path, e
            );
        }
    }
}
