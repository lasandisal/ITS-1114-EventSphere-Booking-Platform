package lk.ijse.eventsphere.controller;

import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.EventResponseDTO;
import lk.ijse.eventsphere.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<CommonResponse<Page<EventResponseDTO>>> searchPublishedEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.searchPublishedEvents(keyword, categoryId, pageable);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Published events retrieved", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<EventResponseDTO>> getById(@PathVariable Long id) {
        EventResponseDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Event details retrieved", event));
    }
}