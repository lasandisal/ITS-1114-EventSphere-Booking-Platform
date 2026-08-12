package lk.ijse.eventsphere.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public GeminiApiClient(@Value("${app.gemini.api-key}") String apiKey,
                           @Value("${app.gemini.model:gemini-3.6-flash}") String model) {
        // Strip "models/" prefix if present to ensure proper URL format
        this.model = model.startsWith("models/") ? model.substring(7) : model;
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public JsonNode generateContent(String systemPrompt, List<Map<String, Object>> contents,
                                    List<Map<String, Object>> tools) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }

        body.put("contents", contents);

        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(this.model))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

        } catch (RestClientResponseException e) {
            log.error(">>> GEMINI API ERROR [Status {}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API call failed [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            log.error(">>> UNEXPECTED AI CLIENT ERROR: ", e);
            throw new RuntimeException("Unexpected error communicating with Gemini API: " + e.getMessage(), e);
        }
    }
}