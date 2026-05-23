/**
 * MongoDB repository for InteractiveSession documents.
 * Provides lookup by roomCode and inviteToken to support both join flows,
 * and by hostUserId for host-management operations.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.enums.SessionLifecycle;

public interface InteractiveSessionRepository extends MongoRepository<InteractiveSession, String> {

    Optional<InteractiveSession> findByRoomCode(String roomCode);

    Optional<InteractiveSession> findByInviteToken(String inviteToken);

    List<InteractiveSession> findByHostUserId(String hostUserId);

    List<InteractiveSession> findByStatus(SessionLifecycle status);

    // Chunk 13 — used by PresenceService to find a player's current active
    // game on WS connect/disconnect so the per-interactiveSession
    // InteractiveSessionPlayer.disconnected flag can be flipped.
    List<InteractiveSession> findByStatusAndPlayersUserId(SessionLifecycle status, String userId);
}
