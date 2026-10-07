package com.routeplanner.ai;

import com.routeplanner.graph.RouteResult;
import com.routeplanner.model.City;

/** Builds the system/user prompts sent to the AI for journey narration. */
public class NarrationPromptBuilder {

    public static String systemPrompt() {
        return "You are a witty, knowledgeable travel narrator. Given a computed route between " +
               "cities, including each city's landmark, you write a short, vivid, engaging narrative " +
               "of the journey stop by stop. Weave in the landmark facts naturally, mention the " +
               "distance of each leg, and keep the whole narration under 350 words. Do not invent " +
               "landmarks that were not provided; if a city has no landmark listed, keep it brief.";
    }

    public static String userPrompt(RouteResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Narrate this journey computed using the ").append(result.getAlgorithmUsed()).append(" algorithm.\n");
        sb.append("Total distance: ").append(String.format("%.1f", result.getTotalWeight())).append(" km.\n\n");
        sb.append("Route stops in order:\n");

        for (int i = 0; i < result.getPath().size(); i++) {
            City c = result.getPath().get(i);
            sb.append(i + 1).append(". ").append(c.getName());
            if (c.getLandmark() != null && !c.getLandmark().isBlank()) {
                sb.append(" — Landmark: ").append(c.getLandmark());
            }
            sb.append("\n");
        }

        sb.append("\nWrite the narration now.");
        return sb.toString();
    }
}
