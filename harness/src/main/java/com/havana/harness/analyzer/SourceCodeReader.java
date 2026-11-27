
package com.havana.harness.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//1. 找出 Havana src/main/java 底下所有 .java 檔案。
//2. 把每個檔案的路徑和程式碼讀出來，合併成一份文字。

public class SourceCodeReader {

    private final Path sourceDirectory;

    public SourceCodeReader(Path sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public String readAllJavaFiles() throws IOException {

        if (!Files.isDirectory(sourceDirectory)) {
            throw new IllegalStateException(
                    "Source directory not found: " + sourceDirectory
            );
        }

        try (Stream<Path> files = Files.walk(sourceDirectory)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::readFile)
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private String readFile(Path path) {
        try {
            return "\n===== FILE: " + path + " =====\n"
                    + Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read source file: " + path, e
            );
        }
    }
}
