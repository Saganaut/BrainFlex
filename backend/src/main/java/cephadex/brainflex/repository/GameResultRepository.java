/**
 * MongoDB repository for GameResult documents.
 * Results are looked up by session ID to serve the post-game results
 * screen and verify that stat updates have been applied.
 */
package cephadex.brainflex.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import cephadex.brainflex.model.GameResult;

public interface GameResultRepository extends MongoRepository<GameResult, String> {

    Optional<GameResult> findByGameSessionId(String gameSessionId);
}
