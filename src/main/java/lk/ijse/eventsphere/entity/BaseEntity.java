package lk.ijse.eventsphere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Shared soft-delete fields for entities that can be removed by a user/organizer/admin
 * but must be retained for referential integrity and audit history (Event, TicketType, User).
 *
 * Paired with @SQLDelete + @SQLRestriction on the subclass:
 *  - repository.delete(entity) issues an UPDATE instead of a DELETE
 *  - every normal find/query automatically excludes rows where deleted = true
 * No manual "WHERE deleted = false" needed anywhere in service/repository code.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime deletedAt;
}
