package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Long eventId;
    private String eventTitle;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private List<TicketDTO> tickets;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // null once CONFIRMED
}