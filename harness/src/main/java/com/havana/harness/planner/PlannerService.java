
package com.havana.harness.planner;

import com.havana.harness.ai.GeminiClient;
import com.havana.harness.context.ContextBuilder;
import com.havana.harness.verifier.Verifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PlannerService {

    private final ContextBuilder contextBuilder;
    private final Planner planner;
    private final GeminiClient geminiClient;
    private final Verifier verifier;

    public PlannerService(ContextBuilder contextBuilder)
            throws IOException {

        this.contextBuilder = contextBuilder;
        this.planner = new Planner();
        this.geminiClient = new GeminiClient();
        this.verifier = new Verifier(geminiClient);
    }

    public String plan(String requirement) throws IOException {

        String context = contextBuilder.buildContext();

        // 第一步：產生開發計畫
        String plan = geminiClient.ask(
                planner.createPlan(requirement, context)
        );

        // 第二步：檢查開發計畫
        String verification = verifier.verify(
                requirement, context, plan
        );

        return "【開發計畫】\n" + plan
                + "\n\n【Verifier 檢查結果】\n" + verification;
    }
}
