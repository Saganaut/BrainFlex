/**
 * MongoDB repository for GameSession documents.
 * Provides lookup by roomCode and inviteToken to support both join flows,
 * and by hostUserId for host-management operations.
 */
package cephadex.brainflex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.GameSession;
import cephadex.brainflex.model.enums.GameStatus;

public interface GameSessionRepository extends MongoRepository<GameSession, String> {

    Optional<GameSession> findByRoomCode(String roomCode);

    Optional<GameSession> findByInviteToken(String inviteToken);

    List<GameSession> findByHostUserId(String hostUserId);

    List<GameSession> findByStatus(GameStatus status);
}
