package com.havana.harness.executor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KiroExecutorTest {

    @Test
    void shouldCallKiroSuccessfully() throws Exception {

        KiroExecutor executor = new KiroExecutor();

        String result = executor.execute(
                "請不要修改任何檔案，只回答：Java 成功呼叫 Kiro"
        );

        System.out.println(result);

        assertTrue(result.contains("Java 成功呼叫 Kiro"));
    }
}