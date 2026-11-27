package com.havana.harness.analyzer;

import com.havana.harness.ai.GeminiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskAnalyzerTest {
    @Test
    void shouldSendTasksAndSourceCodeToGemini() {

        // Arrange
        GeminiClient geminiClient = mock(GeminiClient.class);

        when(geminiClient.ask(argThat(prompt ->
                prompt.contains("M1-1 Tile")
                        && prompt.contains("public record Tile")
        ))).thenReturn("M1-1：COMPLETED");

        TaskAnalyzer analyzer = new TaskAnalyzer(geminiClient);

        String tasks = "M1-1 Tile";
        String sourceCode = "public record Tile(int left, int right) {}";

        // Act
        String result = analyzer.analyze(tasks, sourceCode);

        // Assert
        assertThat(result).contains("COMPLETED");

        verify(geminiClient).ask(argThat(prompt ->
                prompt.contains("M1-1 Tile")
                        && prompt.contains("public record Tile")
                        && prompt.contains("NEEDS_REVIEW")
        ));
    }
}
