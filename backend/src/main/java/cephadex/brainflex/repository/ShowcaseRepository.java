/**
 * MongoDB repository for Showcase documents.
 * Provides lookup by roomCode and inviteToken to support both join flows,
 * and by hostUserId for host-management operations.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.Showcase;
import cephadex.brainflex.model.enums.GameStatus;

public interface ShowcaseRepository extends MongoRepository<Showcase, String> {

    Optional<Showcase> findByRoomCode(String roomCode);

    Optional<Showcase> findByInviteToken(String inviteToken);

    List<Showcase> findByHostUserId(String hostUserId);

    List<Showcase> findByStatus(GameStatus status);

    // Chunk 13 — used by PresenceService to find a player's current active
    // game on WS connect/disconnect so the per-showcase
    // ShowcasePlayer.disconnected flag can be flipped.
    List<Showcase> findByStatusAndPlayersUserId(GameStatus status, String userId);
}
