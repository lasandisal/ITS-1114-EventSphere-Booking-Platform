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

}
