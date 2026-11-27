package com.havana.harness.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCodeReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadAllJavaFilesRecursively() throws Exception {

        // Arrange：模擬 Havana Java 專案
        Path modelDir = tempDir.resolve("model");
        Path serviceDir = tempDir.resolve("service");

        Files.createDirectories(modelDir);
        Files.createDirectories(serviceDir);

        Files.writeString(
                modelDir.resolve("Tile.java"),
                "public record Tile(int left, int right) {}"
        );

        Files.writeString(
                serviceDir.resolve("GameEngineService.java"),
                "public class GameEngineService {}"
        );

        Files.writeString(
                modelDir.resolve("notes.txt"),
                "This is not Java"
        );

        // Act
        SourceCodeReader reader = new SourceCodeReader(tempDir);
        String result = reader.readAllJavaFiles();

        // Assert
        assertThat(result)
                .contains("Tile.java")
                .contains("GameEngineService.java")
                .contains("public record Tile")
                .contains("public class GameEngineService")
                .doesNotContain("This is not Java");
    }
}
