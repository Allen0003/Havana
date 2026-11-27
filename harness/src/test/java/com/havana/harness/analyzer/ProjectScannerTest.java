package com.havana.harness.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldScanBackendFrontendAndProjectDocuments()
            throws Exception {

        // Arrange
        Path backendDir = tempDir.resolve("backend/src/main/java");
        Path frontendDir = tempDir.resolve("frontend/src");
        Path docsDir = tempDir.resolve("docs");
        Path nodeModulesDir = tempDir.resolve("frontend/node_modules");

        Files.createDirectories(backendDir);
        Files.createDirectories(frontendDir);
        Files.createDirectories(docsDir);
        Files.createDirectories(nodeModulesDir);

        Files.writeString(
                backendDir.resolve("Tile.java"),
                "public record Tile(int left, int right) {}"
        );

        Files.writeString(
                frontendDir.resolve("game.component.ts"),
                "export class GameComponent {}"
        );

        Files.writeString(
                docsDir.resolve("tasks.md"),
                "# Milestone 1"
        );

        Files.writeString(
                nodeModulesDir.resolve("ignored.ts"),
                "This should not be scanned"
        );

        // Act
        ProjectScanner scanner = new ProjectScanner(tempDir);
        String result = scanner.scan();

        // Assert
        assertThat(result)
                .contains("backend/src/main/java/Tile.java")
                .contains("frontend/src/game.component.ts")
                .contains("docs/tasks.md")
                .contains("public record Tile")
                .contains("export class GameComponent")
                .doesNotContain("This should not be scanned");
    }
}
