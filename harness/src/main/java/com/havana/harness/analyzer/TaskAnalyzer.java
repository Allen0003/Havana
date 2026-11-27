
package com.havana.harness.analyzer;

import com.havana.harness.ai.GeminiClient;

public class TaskAnalyzer {

    private final GeminiClient geminiClient;

    public TaskAnalyzer(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String analyze(String tasks, String sourceCode) {

        String prompt = """
                你是一位資深 Java Spring Boot 工程師，
                現在要協助檢查 Havana 古巴骨牌專案的開發進度。

                請根據提供的「開發任務清單」與「現有程式碼」，
                逐項判斷任務是否已經完成。

                判斷規則：
                1. 不能只因為類別或方法存在，就認定任務完成。
                2. 必須檢查實際程式碼是否符合任務要求。
                3. 不可以假設不存在於提供程式碼中的功能已經完成。
                4. 如果證據不足，必須標記 NEEDS_REVIEW。
                5. 如果確認缺少必要實作，標記 INCOMPLETE。
                6. 只有有充分程式碼證據時，才能標記 COMPLETED。
                7. 不要修改或產生任何程式碼。

                請使用以下格式回覆每一項任務：

                任務 ID：
                任務名稱：
                狀態：COMPLETED / INCOMPLETE / NEEDS_REVIEW
                程式碼證據：
                判斷理由：
                尚缺少的內容：

                ===== 開發任務清單 =====
                %s

                ===== 現有 Java 程式碼 =====
                %s
                """.formatted(tasks, sourceCode);

        return geminiClient.ask(prompt);
    }
}
