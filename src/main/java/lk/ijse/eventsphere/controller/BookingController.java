package lk.ijse.eventsphere.controller;

import jakarta.validation.Valid;
import lk.ijse.eventsphere.constant.CommonResponse;
import lk.ijse.eventsphere.dto.BookingCreateRequestDTO;
import lk.ijse.eventsphere.dto.BookingResponseDTO;
import lk.ijse.eventsphere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<CommonResponse<BookingResponseDTO>> create(
            @Valid @RequestBody BookingCreateRequestDTO request) {
        BookingResponseDTO booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(HttpStatus.CREATED.value(),
                        "Booking held — complete payment before " + booking.getExpiresAt(), booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<BookingResponseDTO>> getById(@PathVariable Long id) {
        BookingResponseDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Booking retrieved", booking));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<BookingResponseDTO>>> myBookings(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BookingResponseDTO> bookings = bookingService.getMyBookings(pageable);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Your bookings retrieved", bookings));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<CommonResponse<BookingResponseDTO>> cancel(@PathVariable Long id) {
        BookingResponseDTO booking = bookingService.cancelBooking(id);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK.value(), "Booking cancelled", booking));
    }
}
