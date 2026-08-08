package lk.ijse.eventsphere.config;

import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Runs once on every startup; inserts the three fixed roles only if they
// don't already exist, so it's safe to run repeatedly against the same
// database (both local and Aiven) without duplicating rows.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
        }
    }
}
