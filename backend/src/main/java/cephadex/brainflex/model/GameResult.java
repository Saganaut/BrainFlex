/**
 * Permanent record of a completed game's final standings.
 * Written once when the session transitions to FINISHED and used to
 * display post-game results and update global PlayerStats on each User.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "game_results")
public class GameResult {
    @Id
    private String id;

    @Indexed
    private String gameSessionId; // reference to GameSession

    private List<PlayerPlacement> placements; // ordered by placement (1st first)

    private LocalDateTime endedAt = LocalDateTime.now();
}
