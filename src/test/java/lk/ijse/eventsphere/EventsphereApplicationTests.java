package lk.ijse.eventsphere;

import lk.ijse.eventsphere.entity.*;
import lk.ijse.eventsphere.enums.BookingStatus;
import lk.ijse.eventsphere.repository.*;
import lk.ijse.eventsphere.service.BookingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import lk.ijse.eventsphere.service.TicketEmailItem;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventsphereApplicationTests {

    @Autowired
    private ITemplateEngine templateEngine;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void testOrganizerCreateCustomVenue() throws Exception {
        mockMvc.perform(post("/api/v1/organizer/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Lotus Rooftop Lounge",
                        "addressLine": "123 Galle Road",
                        "city": "Colombo",
                        "capacity": 350
                    }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Lotus Rooftop Lounge"));
    }

    @Test
    @WithMockUser(username = "info.pulsefitgym@gmail.com", roles = {"ORGANIZER"})
    void testDuplicateEventAndVenueCollisionRejection() throws Exception {
        Category cat = categoryRepository.findAll().stream().findFirst().orElse(null);
        if (cat == null) return;

        // Create an isolated test venue to guarantee zero test pollution
        Venue venue = venueRepository.save(Venue.builder()
                .name("Isolated Arena " + System.currentTimeMillis())
                .addressLine("123 Test St")
                .city("Colombo")
                .capacity(500)
                .build());

        String uniqueTitle = "Tech Summit Collision Test " + System.currentTimeMillis();
        String body = String.format("""
            {
                "categoryId": %d,
                "venueId": %d,
                "title": "%s",
                "description": "Annual Summit",
                "startDatetime": "2027-06-15T09:00:00",
                "endDatetime": "2027-06-15T17:00:00"
            }
        """, cat.getId(), venue.getId(), uniqueTitle);

        // 1. First event should succeed
        mockMvc.perform(post("/api/v1/organizer/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201));

        // 2. Exact same title & day by the same organizer should return 409 Conflict
        mockMvc.perform(post("/api/v1/organizer/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("You already have an active event titled")));

        // 3. Different title but overlapping venue time window should return 409 Conflict
        String collidingVenueBody = String.format("""
            {
                "categoryId": %d,
                "venueId": %d,
                "title": "%s",
                "description": "Different Event",
                "startDatetime": "2027-06-15T14:00:00",
                "endDatetime": "2027-06-15T19:00:00"
            }
        """, cat.getId(), venue.getId(), "Different Event " + System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/organizer/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(collidingVenueBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("is already booked for another event")));
    }

    @Test
    void testStaleBookingExpiryAndInventoryRestoration() {
        Event event = eventRepository.findAll().stream().findFirst().orElse(null);
        User user = userRepository.findAll().stream().findFirst().orElse(null);
        if (event == null || user == null) return;

        // 1. Create a fresh TicketType with total=100, available=89 (simulating 11 tickets currently locked)
        TicketType ticketType = ticketTypeRepository.save(TicketType.builder()
                .event(event)
                .name("VIP Stale Expiry Test " + System.currentTimeMillis())
                .price(BigDecimal.valueOf(1500))
                .totalQuantity(100)
                .availableQuantity(89)
                .build());

        // 2. Create a PENDING booking with expiresAt 5 minutes ago holding 11 tickets
        Booking booking = Booking.builder()
                .bookingReference("EXP-" + UUID.randomUUID().toString().substring(0, 8))
                .user(user)
                .event(event)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(16500))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .items(new ArrayList<>())
                .build();

        BookingItem item = BookingItem.builder()
                .booking(booking)
                .ticketType(ticketType)
                .quantity(11)
                .unitPrice(BigDecimal.valueOf(1500))
                .subtotal(BigDecimal.valueOf(16500))
                .build();

        booking.getItems().add(item);
        Booking savedBooking = bookingRepository.save(booking);

        // 3. Trigger expireStaleBookings
        bookingService.expireStaleBookings();

        // 4. Verify availableQuantity is restored back to 100
        TicketType reloadedTicketType = ticketTypeRepository.findById(ticketType.getId()).orElseThrow();
        Assertions.assertEquals(100, reloadedTicketType.getAvailableQuantity(),
                "Inventory must be released back to totalQuantity when booking expires");

        // 5. Verify booking status transitioned to EXPIRED
        Booking reloadedBooking = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        Assertions.assertEquals(BookingStatus.EXPIRED, reloadedBooking.getStatus(),
                "Booking status must transition to EXPIRED");
    }

    @Autowired
    private lk.ijse.eventsphere.service.OrganizerAnalyticsService organizerAnalyticsService;

    @Test
    @WithMockUser(username = "info.pulsefitgym@gmail.com", roles = {"ORGANIZER"})
    void testOrganizerAnalyticsOverview() {
        var overview = organizerAnalyticsService.getOverview();
        Assertions.assertNotNull(overview);
        System.out.println("TEST SUCCESS - TOTAL EVENTS: " + overview.getTotalEvents());
        System.out.println("TEST SUCCESS - TOTAL TICKETS: " + overview.getTotalTicketsSold());
        System.out.println("TEST SUCCESS - TOTAL REVENUE: " + overview.getTotalRevenue());
    }

    @Test
    void testEmailThymeleafTemplatesRenderProperly() {
        // 1. Verify OTP Verification Template
        Context otpCtx = new Context();
        otpCtx.setVariable("name", "Lasandi");
        otpCtx.setVariable("otp", "789123");
        String otpHtml = templateEngine.process("mail/otp-verification", otpCtx);
        Assertions.assertNotNull(otpHtml);
        Assertions.assertTrue(otpHtml.contains("789123"));
        Assertions.assertTrue(otpHtml.contains("Lasandi"));
        Assertions.assertTrue(otpHtml.contains("ACCOUNT VERIFICATION"));

        // 2. Verify Password Reset OTP Template
        Context resetCtx = new Context();
        resetCtx.setVariable("name", "Lasandi");
        resetCtx.setVariable("otp", "456789");
        String resetHtml = templateEngine.process("mail/password-reset-otp", resetCtx);
        Assertions.assertNotNull(resetHtml);
        Assertions.assertTrue(resetHtml.contains("456789"));
        Assertions.assertTrue(resetHtml.contains("PASSWORD RESET CODE"));

        // 3. Verify Master Order Receipt Template
        TicketEmailItem item1 = TicketEmailItem.builder()
                .ticketCode("TC-001")
                .ticketTypeName("VIP")
                .attendeeName("Alice Smith")
                .seatNumber("Row A, 12")
                .eventDate("Oct 10, 2026")
                .eventTime("07:00 PM")
                .venueName("Grand Arena")
                .venueAddress("456 Ocean Drive")
                .qrPng(new byte[]{1, 2, 3})
                .build();

        Context receiptCtx = new Context();
        receiptCtx.setVariable("name", "Lasandi");
        receiptCtx.setVariable("eventTitle", "Glow EDM Night");
        receiptCtx.setVariable("bookingReference", "REF-EDM-999");
        receiptCtx.setVariable("eventDate", "Oct 10, 2026");
        receiptCtx.setVariable("eventTime", "07:00 PM");
        receiptCtx.setVariable("venueName", "Grand Arena");
        receiptCtx.setVariable("venueAddress", "456 Ocean Drive");
        receiptCtx.setVariable("totalTickets", 1);
        receiptCtx.setVariable("currency", "LKR");
        receiptCtx.setVariable("formattedAmount", "4500.00");
        receiptCtx.setVariable("tickets", List.of(item1));

        String receiptHtml = templateEngine.process("mail/order-receipt", receiptCtx);
        Assertions.assertNotNull(receiptHtml);
        Assertions.assertTrue(receiptHtml.contains("Glow EDM Night"));
        Assertions.assertTrue(receiptHtml.contains("REF-EDM-999"));
        Assertions.assertTrue(receiptHtml.contains("Alice Smith"));
        Assertions.assertTrue(receiptHtml.contains("cid:receipt_qr0"));

        // 4. Verify Individual Ticket Pass Template
        Context passCtx = new Context();
        passCtx.setVariable("name", "Alice Smith");
        passCtx.setVariable("eventTitle", "Glow EDM Night");
        passCtx.setVariable("bookingReference", "REF-EDM-999");
        passCtx.setVariable("eventDate", "Oct 10, 2026");
        passCtx.setVariable("eventTime", "07:00 PM");
        passCtx.setVariable("venueName", "Grand Arena");
        passCtx.setVariable("venueAddress", "456 Ocean Drive");
        passCtx.setVariable("ticketTier", "VIP");
        passCtx.setVariable("ticket", item1);

        String passHtml = templateEngine.process("mail/ticket-pass", passCtx);
        Assertions.assertNotNull(passHtml);
        Assertions.assertTrue(passHtml.contains("Glow EDM Night"));
        Assertions.assertTrue(passHtml.contains("Alice Smith"));
        Assertions.assertTrue(passHtml.contains("cid:guest_qr"));
        Assertions.assertTrue(passHtml.contains("Row A, 12"));
    }
}
