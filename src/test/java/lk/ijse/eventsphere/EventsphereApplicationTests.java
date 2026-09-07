package lk.ijse.eventsphere;

import lk.ijse.eventsphere.entity.Category;
import lk.ijse.eventsphere.entity.Venue;
import lk.ijse.eventsphere.repository.CategoryRepository;
import lk.ijse.eventsphere.repository.EventRepository;
import lk.ijse.eventsphere.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventsphereApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

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
}
