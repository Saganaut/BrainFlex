/**
 * Spring Data repository for {@link EmailSuppression}. The unique index on
 * {@code emailLower} is declared on the document so concurrent unsubscribes
 * collapse to one row at the database level — the service catches the
 * duplicate-key exception and upserts the {@code blocked} set instead of
 * writing a second row.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.user.EmailSuppression;
public interface EmailSuppressionRepository extends MongoRepository<EmailSuppression, String> {

    Optional<EmailSuppression> findByEmailLower(String emailLower);
}
