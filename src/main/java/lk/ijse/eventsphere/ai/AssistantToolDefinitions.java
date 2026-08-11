package lk.ijse.eventsphere.ai;

import java.util.List;
import java.util.Map;

// Same three tools, same "no booking/payment tool exists at all" guardrail
// as the Claude version — only the schema shape changed: Gemini's Schema
// type enum uses UPPERCASE (STRING/OBJECT/INTEGER), and declarations are
// wrapped one level deeper under "functionDeclarations".
public final class AssistantToolDefinitions {

    private AssistantToolDefinitions() {}

    public static List<Map<String, Object>> all() {
        return List.of(
                Map.of("functionDeclarations", List.of(
                        Map.of(
                                "name", "search_events",
                                "description", "Search published events by free-text keyword and/or category name. Returns a short list of matching events with id, title, date, and venue.",
                                "parameters", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "keyword", Map.of("type", "STRING", "description", "Free-text search term, e.g. event title or theme"),
                                                "category", Map.of("type", "STRING", "description", "Category name, e.g. 'Music', 'Technology'")
                                        )
                                )
                        ),
                        Map.of(
                                "name", "get_event_details",
                                "description", "Get full details for one event by its id — description, venue, dates, and all ticket types with live pricing and availability.",
                                "parameters", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "eventId", Map.of("type", "INTEGER", "description", "The numeric event id")
                                        ),
                                        "required", List.of("eventId")
                                )
                        ),
                        Map.of(
                                "name", "get_my_bookings",
                                "description", "Get the current user's own booking history, including each booking's status and tickets. Takes no input.",
                                "parameters", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of()
                                )
                        )
                ))
        );
    }
}
