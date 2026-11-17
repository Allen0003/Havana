package com.havana.harness.planner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlannerTest {

    @Test
    void shouldCombineRequirementAndContext() {
        Planner planner = new Planner();

        String result = planner.createPlan(
                "增加古巴骨牌計分功能",
                "遊戲邏輯必須放在 GameEngineService"
        );

        assertThat(result)
                .contains("增加古巴骨牌計分功能")
                .contains("GameEngineService");
    }
}
