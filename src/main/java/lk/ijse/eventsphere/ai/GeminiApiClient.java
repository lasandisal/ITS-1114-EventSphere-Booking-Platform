package lk.ijse.eventsphere.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Thin wrapper around Google's Gemini API (generativelanguage.googleapis.com
// — the free AI Studio endpoint, not the paid Vertex AI one, which needs a
// billed GCP project). Called server-side only; the API key never reaches
// the frontend, same guardrail as the original Claude design.
@Component
public class GeminiApiClient {

    private final RestClient restClient;
    private final String model;

    public GeminiApiClient(@Value("${app.gemini.api-key}") String apiKey,
                           @Value("${app.gemini.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public JsonNode generateContent(String systemPrompt, List<Map<String, Object>> contents,
                                    List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", contents);
        body.put("tools", tools);

        return restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }
}
