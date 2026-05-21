/**
 * Spring Data repository for {@link Notification} rows.
 *
 * The two hot reads — paginated dropdown and unread-count badge — are served
 * by the compound indexes declared on the document. Writes are append-only
 * apart from {@code read} flips, which go through the bulk update on
 * {@link cephadex.brainflex.service.NotificationService} so the dropdown
 * never has to N+1 mark a list of rows.
 */
package cephadex.brainflex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findAllByUserId(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);
}
