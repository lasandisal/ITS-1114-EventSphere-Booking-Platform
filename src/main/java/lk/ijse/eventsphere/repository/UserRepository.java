package lk.ijse.eventsphere.repository;

import lk.ijse.eventsphere.entity.User;
import lk.ijse.eventsphere.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // used by the JWT auth filter / login flow
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // backs the admin user directory's live search/filter table (name or email, partial match)
    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR
                   LOWER(u.name)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);
}