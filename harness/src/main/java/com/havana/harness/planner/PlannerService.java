package com.havana.harness.planner;

import com.havana.harness.context.ContextBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PlannerService {

    private final ContextBuilder contextBuilder;
    private final Planner planner;

    public PlannerService(ContextBuilder contextBuilder) {
        this.contextBuilder = contextBuilder;
        this.planner = new Planner();
    }

    public String plan(String requirement) throws IOException {
        String context = contextBuilder.buildContext();

        return planner.createPlan(requirement, context);
    }
}