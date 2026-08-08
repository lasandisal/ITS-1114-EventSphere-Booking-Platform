package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.Role;
import lk.ijse.eventsphere.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
