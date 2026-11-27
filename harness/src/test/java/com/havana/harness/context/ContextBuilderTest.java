package com.havana.harness.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadSteeringFilesAndTasks() throws Exception {

        // Arrange：建立模擬 Havana 專案的目錄結構
        Path steeringDir = tempDir.resolve(".kiro").resolve("steering");
        Files.createDirectories(steeringDir);

        Path docsDir = tempDir.resolve("docs");
        Files.createDirectories(docsDir);

        Files.writeString(
                steeringDir.resolve("project.md"),
                "# Havana Project Rules"
        );

        Files.writeString(
                docsDir.resolve("tasks.md"),
                "# Havana Development Tasks"
        );

        // Act
        ContextBuilder builder = new ContextBuilder(steeringDir);
        String context = builder.buildContext();

        // Assert
        assertThat(context)
                .contains("Havana Project Rules")
                .contains("Havana Development Tasks");
    }

    @Test
    void shouldReadRealHavanaSteering() throws Exception {

        Path steeringPath = Path.of("..", ".kiro", "steering")
                .toAbsolutePath()
                .normalize();

        ContextBuilder builder = new ContextBuilder(steeringPath);

        String context = builder.buildContext();

        assertThat(context)
                .isNotBlank()
                .contains("Havana")
                .contains("專案開發任務");
    }
}
