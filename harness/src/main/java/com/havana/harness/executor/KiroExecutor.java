package com.havana.harness.executor;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class KiroExecutor {

    public String execute(String task) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "kiro-cli",
                "chat",
                "--no-interactive",
                task
        );

        processBuilder.directory(
                new java.io.File("..").getCanonicalFile()
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Kiro 執行失敗，exitCode=" + exitCode
                    + "\n" + output);
        }

        return output.toString();
    }
}


//Java 收到任務。
//ProcessBuilder 啟動 Kiro CLI。
//把任務傳給 Kiro。
//等待 Kiro 執行。
//把 Kiro 的輸出結果交還給 Java。