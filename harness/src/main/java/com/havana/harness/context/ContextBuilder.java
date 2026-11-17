
package com.havana.harness.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//1. steeringDirectory：告訴 ContextBuilder Steering 放在哪裡。
//2. Files.list()：列出 Steering 資料夾裡的檔案。
//3. Files.readString()：讀取 Markdown 內容。


public class ContextBuilder {

    private final Path steeringDirectory;

    public ContextBuilder(Path steeringDirectory) {
        this.steeringDirectory = steeringDirectory;
    }

    public String buildContext() throws IOException {

        if (!Files.isDirectory(steeringDirectory)) {
            throw new IllegalStateException(
                    "Steering directory not found: " + steeringDirectory
            );
        }

        try (Stream<Path> files = Files.list(steeringDirectory)) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(this::readFile)
                    .collect(Collectors.joining("\n\n"));
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e
            );
        }
    }
}
