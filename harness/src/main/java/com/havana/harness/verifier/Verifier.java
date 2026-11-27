
package com.havana.harness.verifier;

import com.havana.harness.ai.GeminiClient;

import java.io.IOException;

public class Verifier {

    private final GeminiClient geminiClient;

    public Verifier(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String verify(String requirement, String context, String plan)
            throws IOException {

        String prompt = """
                你是一位嚴格的 Java 程式碼審查員。

                請根據以下專案規範，檢查開發計畫。

                【使用者需求】
                %s

                【專案規範】
                %s

                【AI 產生的開發計畫】
                %s

                請檢查：
                1. 是否違反專案規範？
                2. 是否有 Controller 包含商業邏輯？
                3. 是否缺少必要的單元測試？
                4. 是否提到不合理或未確認存在的類別、方法？

                最後請明確給出結果：
                PASS：沒有發現明顯違規。
                FAIL：有發現問題，並列出原因。

                不要修改任何程式碼。
                """.formatted(requirement, context, plan);

        return geminiClient.ask(prompt);
        //Gemini 回傳檢查結果。
    }
}
