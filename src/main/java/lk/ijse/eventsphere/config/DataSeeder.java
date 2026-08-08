package lk.ijse.eventsphere.config;

import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.enums.UserStatus;
import lk.ijse.eventsphere.repository.RoleRepository;
import lk.ijse.eventsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

// Runs once on every startup; inserts the three fixed roles, and one ADMIN
// account, only if they don't already exist — safe to run repeatedly against
// the same database (both local and Aiven) without duplicating rows.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Self-registration only ever grants USER (see AuthServiceImpl), so there
    // is otherwise no path to an ADMIN account at all. Override these via env
    // vars — never leave the fallback password in place past first login.
    @Value("${app.admin.email:admin@eventsphere.lk}")
    private String adminEmail;

    @Value("${app.admin.password:ChangeMe123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing after seeding"));

        User admin = User.builder()
                .fullName("EventSphere Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);

        log.warn("Seeded default admin account ({}) with the configured/fallback password — " +
                "log in and change it, or set app.admin.email / app.admin.password before first run.", adminEmail);
    }
}
