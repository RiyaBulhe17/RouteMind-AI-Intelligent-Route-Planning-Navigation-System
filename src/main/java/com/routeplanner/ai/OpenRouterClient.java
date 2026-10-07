package com.routeplanner.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal client for OpenRouter's chat completions endpoint
 * (https://openrouter.ai/docs). Used to turn a computed route + landmark
 * data into a narrated travel story.
 */
public class OpenRouterClient {

    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public OpenRouterClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? "openai/gpt-4o-mini" : model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Sends the prompt to OpenRouter and returns the narrated text.
     * Throws RuntimeException with a readable message on failure.
     */
    public String narrate(String systemPrompt, String userPrompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
        body.put("messages", messages);
        body.put("temperature", 0.8);
        body.put("max_tokens", 700);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                // Optional but recommended by OpenRouter for routing/analytics:
                .header("HTTP-Referer", "https://localhost")
                .header("X-Title", "AI-Augmented Route Planner")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("OpenRouter returned no choices: " + response.body());
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message.optString("content", "(no content returned)");
    }
}
