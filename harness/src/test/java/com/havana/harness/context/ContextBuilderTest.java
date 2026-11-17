
package com.havana.harness.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;



class ContextBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadSteeringFiles() throws Exception {

        // Arrange
        Path steeringDir = Files.createDirectory(
                tempDir.resolve("steering")
        );

        Files.writeString(
                steeringDir.resolve("project.md"),
                "# Havana Project Rules"
        );

        // Act
        ContextBuilder builder = new ContextBuilder(steeringDir);
        String context = builder.buildContext();

        // Assert
        assertThat(context)
                .contains("Havana Project Rules");
    }
}
