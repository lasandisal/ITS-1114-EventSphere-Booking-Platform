package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.*;
import lk.ijse.eventsphere.service.BookingService;
import lk.ijse.eventsphere.service.EventService;
import lk.ijse.eventsphere.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerEventController {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<CommonResponse<EventResponseDTO>> create(
            @Valid @RequestBody EventCreateRequestDTO request) {
        EventResponseDTO event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), "Event created in draft status", event));
    }

    @PostMapping("/{eventId}/ticket-types")
    public ResponseEntity<CommonResponse<TicketTypeResponseDTO>> createTicketType(
            @PathVariable Long eventId,
            @Valid @RequestBody TicketTypeRequestDTO request) {
        TicketTypeResponseDTO response = ticketTypeService.addTicketType(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(), "Ticket type added successfully", response));
    }

    @GetMapping("/{eventId}/bookings")
    public ResponseEntity<CommonResponse<List<BookingResponseDTO>>> getEventBookings(
            @PathVariable Long eventId) {
        List<BookingResponseDTO> bookings = bookingService.getBookingsByEventId(eventId);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Event bookings retrieved successfully", bookings));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<EventResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EventUpdateRequestDTO request) {
        EventResponseDTO event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Event updated successfully", event));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CommonResponse<EventResponseDTO>> publish(@PathVariable Long id) {
        EventResponseDTO event = eventService.publishEvent(id);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Event published successfully", event));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<CommonResponse<EventResponseDTO>> cancel(@PathVariable Long id) {
        EventResponseDTO event = eventService.cancelEvent(id);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "Event cancelled", event));
    }

    @GetMapping("/my-events")
    public ResponseEntity<CommonResponse<Page<EventResponseDTO>>> getMyEvents(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.getMyEvents(pageable);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK.value(), "My events retrieved", events));
    }
}