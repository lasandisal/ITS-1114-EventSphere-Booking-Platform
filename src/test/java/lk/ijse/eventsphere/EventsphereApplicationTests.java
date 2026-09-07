package lk.ijse.eventsphere;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class EventsphereApplicationTests {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ORGANIZER")
    void testOrganizerCreateCustomVenue() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/organizer/venues")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Lotus Rooftop Lounge",
                        "addressLine": "123 Galle Road",
                        "city": "Colombo",
                        "capacity": 350
                    }
                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(201))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.name").value("Lotus Rooftop Lounge"));
    }

}
