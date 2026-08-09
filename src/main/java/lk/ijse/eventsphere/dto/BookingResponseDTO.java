package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {
    private Long id;
    private String bookingReference;
    private Long eventId;
    private String eventTitle;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private List<BookingItemResponseDTO> items;
}
