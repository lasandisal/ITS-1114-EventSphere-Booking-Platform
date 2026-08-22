package lk.ijse.eventsphere.config;

import lk.ijse.eventsphere.entity.Organizer;
import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.OrganizerStatus;
import lk.ijse.eventsphere.enums.RoleName;
import lk.ijse.eventsphere.enums.UserStatus;
import lk.ijse.eventsphere.repository.OrganizerRepository;
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

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.admin.email:eventsphere.tickets@gmail.com}")
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
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing"));
        Role organizerRole = roleRepository.findByName(RoleName.ORGANIZER)
                .orElseThrow(() -> new IllegalStateException("ORGANIZER role missing"));

        User admin = User.builder()
                .fullName("EventSphere Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(adminRole, organizerRole))
                .build();
        userRepository.save(admin);

        Organizer adminOrganizer = Organizer.builder()
                .user(admin)
                .businessName("EventSphere HQ")
                .bio("System Administration Event Operations")
                .status(OrganizerStatus.APPROVED) // Admin organizer profile approved by default
                .build();
        organizerRepository.save(adminOrganizer);

        log.info("Seeded default admin account ({}) with ORGANIZER profile.", adminEmail);
    }
}