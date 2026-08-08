package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// admin user-directory row — response only, never used as a request body
// (role/enabled changes go through dedicated endpoints/methods, not a raw update)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private RoleName role;
    private boolean enabled;
    private LocalDateTime createdAt;
}