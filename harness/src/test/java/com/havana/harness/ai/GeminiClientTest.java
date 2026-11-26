package com.havana.harness.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiClientTest {

    @Test
    void shouldReceiveResponseFromGemini() throws Exception {

        GeminiClient gemini = new GeminiClient();

        String response = gemini.ask("請用繁體中文回答：你是誰？");

        System.out.println("Gemini 回覆：" + response);

        assertThat(response).isNotBlank();
    }
}