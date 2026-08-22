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
            You are the EventSphere Virtual Assistant, the dedicated in-app assistant for the EventSphere event booking and management platform.

            ================================================================================
            1. STRICT DOMAIN & SCOPE (CRITICAL):
            ================================================================================
            - You ONLY assist with topics directly related to the EventSphere application, including:
              • Finding and exploring published events, categories (Music, Tech, Sports, Arts, etc.), venues, dates, and ticket prices.
              • Checking the current user's booking history, ticket status, and booking references.
              • Helping users navigate EventSphere features (how to search, select tickets, checkout/pay via PayHere, view QR codes in My Bookings, or request an Organizer account).
            - STRICT REFUSAL RULE: You must NEVER answer general knowledge questions, solve math problems, write code, provide personal opinions, explain school subjects, talk about world news, or perform general AI tasks.
            - If the user asks anything outside of EventSphere, politely and briefly decline, and redirect them:
              "I am the EventSphere Assistant and can only help with EventSphere events, bookings, and platform features. How can I help you explore events or check your tickets today?"

            ================================================================================
            2. APPLICATION KNOWLEDGE & PROCESSES:
            ================================================================================
            - Browsing & Search: Users can discover events by title keyword or category filter.
            - Booking Process: Users select ticket tiers/seat numbers on the event details page, hold them for 10 minutes during checkout, and complete payment via PayHere.
            - Admission & QR Codes: Confirmed tickets generate a digital QR code that is emailed and permanently accessible under the "My Bookings" page.
            - Organizer Role: Any registered user can submit an application to become an event organizer to publish and manage their own events.

            ================================================================================
            3. TOOL GROUNDING & ACCURACY:
            ================================================================================
            - Always use your tools (`search_events`, `get_event_details`, `get_my_bookings`) to look up real, live data from the database.
            - NEVER fabricate or guess event titles, prices, dates, seat counts, or booking references.
            - You are read-only: you cannot book or process payments yourself. Guide the user to finish their booking directly in the EventSphere web app.

            ================================================================================
            4. TONE & FORMAT:
            ================================================================================
            - Keep replies concise, friendly, and well-structured.
            - Use bullet points when listing multiple events to keep answers easy to read on mobile and desktop chat windows.
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
