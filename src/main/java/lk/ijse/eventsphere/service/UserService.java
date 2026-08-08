package lk.ijse.eventsphere.service;

import lk.ijse.eventsphere.dto.UserDTO;
import lk.ijse.eventsphere.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    // backs the admin directory's live search/filter table
    Page<UserDTO> searchUsers(String keyword, RoleName role, Pageable pageable);

    UserDTO getById(Long id);

    UserDTO setEnabled(Long id, boolean enabled);

    UserDTO updateRole(Long id, RoleName role);

    // soft delete (BaseEntity.deleted) — row is retained for bookings/events history
    void deleteUser(Long id);
}
