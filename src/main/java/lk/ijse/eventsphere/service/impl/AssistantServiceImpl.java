package lk.ijse.eventsphere.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.eventsphere.ai.AssistantToolDefinitions;
import lk.ijse.eventsphere.ai.AssistantToolExecutor;
import lk.ijse.eventsphere.ai.GeminiApiClient;
import lk.ijse.eventsphere.dto.ChatMessageDTO;
import lk.ijse.eventsphere.dto.ChatRequestDTO;
import lk.ijse.eventsphere.dto.ChatResponseDTO;
import lk.ijse.eventsphere.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    // Hard cap on function-call round trips per chat turn — a safety valve
    // against a runaway loop. Legitimate turns need at most 1-2.
    private static final int MAX_TOOL_ITERATIONS = 4;

    private static final String SYSTEM_PROMPT = """
            You are the EventSphere assistant, embedded in the EventSphere event booking platform.

            SCOPE — this is the most important rule: you ONLY discuss EventSphere events, venues,
            categories, and the current user's own bookings. You are not a general-purpose assistant.
            If the user asks about anything else — general knowledge, other topics, coding help,
            opinions, current events, or any request unrelated to EventSphere events and bookings —
            politely decline, explain you can only help with events and bookings on EventSphere, and
            invite them to ask about that instead. Do not answer the off-topic question even partially.

            You have three tools: search_events, get_event_details, get_my_bookings. Always ground
            factual claims about an event, price, date, availability, or booking in a tool call —
            never state a price, date, or seat count from memory or by guessing, and never fabricate
            an event or booking a tool call did not return.

            You cannot create, modify, or cancel a booking, and you cannot process payment — you are
            read-only by design. If the user wants to book something, tell them you've found the
            event and that they can complete the booking in the app themselves.

            If a request is ambiguous (e.g. "find me something fun this weekend"), make a reasonable
            search with the closest matching keyword rather than blocking on perfect information, or
            ask one short clarifying question.

            Keep replies short and conversational — this is a chat interface, not a report.
            """;

    private final GeminiApiClient geminiApiClient;
    private final AssistantToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.getHistory() != null) {
            for (ChatMessageDTO turn : request.getHistory()) {
                // Gemini's two roles are "user" and "model" (not "assistant").
                String geminiRole = "assistant".equals(turn.getRole()) ? "model" : "user";
                contents.add(Map.of("role", geminiRole, "parts", List.of(Map.of("text", turn.getContent()))));
            }
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.getMessage()))));

        JsonNode response = null;
        int iterations = 0;

        while (true) {
            response = geminiApiClient.generateContent(SYSTEM_PROMPT, contents, AssistantToolDefinitions.all());
            JsonNode parts = response.path("candidates").path(0).path("content").path("parts");

            List<JsonNode> functionCalls = new ArrayList<>();
            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    functionCalls.add(part);
                }
            }

            if (functionCalls.isEmpty() || iterations >= MAX_TOOL_ITERATIONS) {
                break;
            }
            iterations++;

            // Echo the model's own turn back verbatim (role "model", the
            // functionCall part(s) as returned) before supplying results —
            // Gemini expects the full exchange replayed each call, same
            // stateless-history pattern as Anthropic's API.
            contents.add(Map.of("role", "model", "parts", toObjectList(parts)));

            List<Map<String, Object>> resultParts = new ArrayList<>();
            for (JsonNode fcPart : functionCalls) {
                JsonNode functionCall = fcPart.path("functionCall");
                String toolName = functionCall.path("name").asText();
                JsonNode args = functionCall.path("args");
                String resultText = toolExecutor.execute(toolName, args);

                // functionResponse.response must be a JSON object, not a
                // bare string — wrap the tool's plain-text/JSON-string result.
                resultParts.add(Map.of(
                        "functionResponse", Map.of(
                                "name", toolName,
                                "response", Map.of("result", resultText)
                        )
                ));
            }
            // Function results go back as role "user" per Gemini's REST spec
            // (there is no distinct "function" role on this endpoint).
            contents.add(Map.of("role", "user", "parts", resultParts));
        }

        String replyText = extractText(response.path("candidates").path(0).path("content").path("parts"));

        List<ChatMessageDTO> updatedHistory = new ArrayList<>(
                request.getHistory() != null ? request.getHistory() : List.of());
        updatedHistory.add(new ChatMessageDTO("user", request.getMessage()));
        updatedHistory.add(new ChatMessageDTO("assistant", replyText));

        return ChatResponseDTO.builder()
                .reply(replyText)
                .history(updatedHistory)
                .build();
    }

    private String extractText(JsonNode parts) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.hasNonNull("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.length() > 0 ? sb.toString()
                : "Sorry, I couldn't put together an answer for that — could you rephrase?";
    }

    // Converts a JsonNode array to a List<Object> Jackson can re-serialize
    // as a plain JSON array on the next request body.
    private List<Object> toObjectList(JsonNode arrayNode) {
        List<Object> result = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            result.add(objectMapper.convertValue(node, Object.class));
        }
        return result;
    }
}
