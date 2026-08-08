package lk.ijse.eventsphere.dto;

import lk.ijse.eventsphere.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private Long userId;
    private String name;
    private String email;
    private RoleName role;
}
