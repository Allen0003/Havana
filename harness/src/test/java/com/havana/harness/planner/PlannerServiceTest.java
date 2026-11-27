package com.havana.harness.planner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlannerServiceTest {

    @Test
    void shouldGeneratePlanUsingGemini() throws Exception {

        PlannerService plannerService = new PlannerService(
                new com.havana.harness.context.ContextBuilder(
                        java.nio.file.Path.of("..", ".kiro", "steering")
                )
        );

        String result = plannerService.plan(
                "請幫 Havana 增加古巴骨牌計分功能，先產生開發計畫，不要修改程式碼。"
        );

        System.out.println("AI 開發計畫：");
        System.out.println(result);

        assertThat(result).isNotBlank();
    }
}