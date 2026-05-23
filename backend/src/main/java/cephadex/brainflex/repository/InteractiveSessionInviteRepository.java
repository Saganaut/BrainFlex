/**
 * MongoDB repository for InteractiveSessionInvite. The redeem endpoint hits
 * `findByInviteToken`; the schedule detail page lists invites by their
 * parent ScheduledInteractiveSession.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.session.InteractiveSessionInvite;

public interface InteractiveSessionInviteRepository
        extends MongoRepository<InteractiveSessionInvite, String> {

    Optional<InteractiveSessionInvite> findByInviteToken(String inviteToken);

    List<InteractiveSessionInvite> findByScheduledInteractiveSessionId(String scheduledInteractiveSessionId);

    boolean existsByEmailAndScheduledInteractiveSessionId(String email, String scheduledInteractiveSessionId);
}
