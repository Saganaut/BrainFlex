/**
 * Shared createdAt / updatedAt audit fields for top-level {@code @Document}
 * types. {@link org.springframework.data.annotation.CreatedDate} and
 * {@link org.springframework.data.annotation.LastModifiedDate} are populated by
 * Spring Data Mongo's auditing infrastructure (enabled by
 * {@link cephadex.brainflex.BrainflexApplication}'s {@code @EnableMongoAuditing})
 * so services no longer need to call {@code setCreatedAt} / {@code setUpdatedAt}
 * before every save.
 *
 * Only documents whose audit semantics are the standard "row was created /
 * last modified" pair extend this. Documents with a domain-specific creation
 * timestamp (e.g. {@code sentAt}, {@code votedAt}, {@code favoritedAt}) keep
 * their bespoke field name.
 */
package cephadex.brainflex.model;

//TODO remove setter, follow pattern in our todo
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Auditable {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
