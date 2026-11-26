
package com.havana.harness.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GeminiClient {

    private final Client client;
    private final String model = "gemini-2.5-flash";

    public GeminiClient() throws IOException {
        Properties properties = new Properties();

        try (FileInputStream input =
                     new FileInputStream("gemini.properties")) {
            properties.load(input);
        }

        String apiKey = properties.getProperty("gemini.api.key");


        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API Key is missing");
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public String ask(String prompt) {
        GenerateContentResponse response =
                client.models.generateContent(model, prompt, null);

        return response.text();
    }
}
