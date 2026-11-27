package com.havana.harness.analyzer;

import com.havana.harness.ai.GeminiClient;
import com.havana.harness.context.ContextBuilder;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;


//1. 開發規範	.kiro/steering/	告訴 AI 程式開發規則
//2. 開發任務	docs/tasks.md	告訴 AI 每個 Milestone 要做什麼
//3. 現有程式碼	backend/src/main/java/	讓 AI 檢查功能是否已經實作

//TaskAnalyzerIntegrationTest
//       │
//       ├── ContextBuilder
//       │      └── 讀取 Steering + Tasks
//       │
//       ├── SourceCodeReader
//       │      └── 讀取 backend Java 程式碼
//       │
//       └── TaskAnalyzer
//              └── 組合資料交給 Gemini
//                       │
//                       ▼
//                  任務完成度分析



class TaskAnalyzerIntegrationTest {

    @Test
    void shouldAnalyzeRealHavanaProject() throws Exception {

        // 1. 讀取 Havana 的 Steering 和 Tasks
        Path steeringPath = Path.of("..", ".kiro", "steering")
                .toAbsolutePath()
                .normalize();

        ContextBuilder contextBuilder =
                new ContextBuilder(steeringPath);

        String projectContext = contextBuilder.buildContext();

        // 2. 讀取 Havana 現有 Java 程式碼
        Path sourcePath = Path.of("..", "backend", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceCodeReader sourceCodeReader =
                new SourceCodeReader(sourcePath);

        String sourceCode = sourceCodeReader.readAllJavaFiles();

        // 3. 真正呼叫 Gemini
        GeminiClient geminiClient = new GeminiClient();

        TaskAnalyzer analyzer = new TaskAnalyzer(geminiClient);

        String result = analyzer.analyze(
                projectContext,
                sourceCode
        );

        // 4. 顯示分析結果
        System.out.println("\n========== Havana 任務分析結果 ==========");
        System.out.println(result);
        System.out.println("=========================================");

        assertThat(result).isNotBlank();
    }
}
