
package com.havana.harness.planner;

public class Planner {

    public String createPlan(String requirement, String context) {

        return """
                開發需求：
                %s

                專案開發規範：
                %s

                請根據以上資訊，產生開發計畫。
                """.formatted(requirement, context);
    }
}
