package lk.ijse.eventsphere.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.eventsphere.dto.EventResponseDTO;
import lk.ijse.eventsphere.entity.Category;
import lk.ijse.eventsphere.repository.CategoryRepository;
import lk.ijse.eventsphere.service.BookingService;
import lk.ijse.eventsphere.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

// Dispatches a tool_use block from Claude to the real service layer, and
// serializes the result back to a plain string for tool_result — every
// answer the model gives about events/bookings is grounded in an actual DB
// read through the same services the REST API uses, never invented.
@Component
@RequiredArgsConstructor
public class AssistantToolExecutor {

    // Keep results small — this goes straight into the model's context on
    // every turn, and a chatbot reply doesn't need 50 events to work with.
    private static final int SEARCH_RESULT_LIMIT = 5;
    private static final int MY_BOOKINGS_LIMIT = 10;

    private final EventService eventService;
    private final BookingService bookingService;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public String execute(String toolName, JsonNode input) {
        try {
            return switch (toolName) {
                case "search_events" -> searchEvents(input);
                case "get_event_details" -> getEventDetails(input);
                case "get_my_bookings" -> getMyBookings();
                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            // A tool failure becomes a plain-text explanation the model can
            // relay conversationally, not a stack trace or a broken turn.
            return "This lookup failed: " + e.getMessage();
        }
    }

    private String searchEvents(JsonNode input) throws Exception {
        String keyword = input.hasNonNull("keyword") ? input.get("keyword").asText() : null;
        String categoryName = input.hasNonNull("category") ? input.get("category").asText() : null;

        Long categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            categoryId = categoryRepository.findByNameIgnoreCase(categoryName.trim())
                    .map(Category::getId)
                    .orElse(null);
            if (categoryId == null) {
                return "No category named '" + categoryName + "' exists — try searching by keyword instead.";
            }
        }

        Pageable pageable = PageRequest.of(0, SEARCH_RESULT_LIMIT);
        var results = eventService.searchPublishedEvents(keyword, categoryId, pageable);

        if (results.isEmpty()) {
            return "No published events matched that search.";
        }

        var summaries = results.getContent().stream()
                .map(e -> Map.of(
                        "id", e.getId(),
                        "title", e.getTitle(),
                        "category", e.getCategoryName(),
                        "venue", e.getVenueName(),
                        "startDatetime", e.getStartDatetime().toString()
                ))
                .toList();
        return objectMapper.writeValueAsString(summaries);
    }

    private String getEventDetails(JsonNode input) throws Exception {
        if (!input.hasNonNull("eventId")) {
            return "eventId is required.";
        }
        Long eventId = input.get("eventId").asLong();
        EventResponseDTO event = eventService.getEventById(eventId);
        return objectMapper.writeValueAsString(event);
    }

    private String getMyBookings() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "The user is currently browsing as a guest and is not logged in. Inform them that to view their personal bookings, they need to log in or create an account on EventSphere.";
        }

        Pageable pageable = PageRequest.of(0, MY_BOOKINGS_LIMIT);
        var results = bookingService.getMyBookings(pageable);

        if (results.isEmpty()) {
            return "This user has no bookings yet.";
        }

        var summaries = results.getContent().stream()
                .map(b -> Map.of(
                        "bookingReference", b.getBookingReference(),
                        "eventTitle", b.getEventTitle(),
                        "status", b.getStatus().name(),
                        "totalAmount", b.getTotalAmount().toString(),
                        "ticketCount", b.getItems().stream().mapToInt(i -> i.getQuantity()).sum()
                ))
                .toList();
        return objectMapper.writeValueAsString(summaries);
    }
}
