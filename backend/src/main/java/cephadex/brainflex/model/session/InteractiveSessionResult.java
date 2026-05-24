/**
 * Permanent record of a completed game's final standings.
 * Written once when the session transitions to FINISHED and used to
 * display post-game results and update global PlayerStats on each User.
 */
package cephadex.brainflex.model.session;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

//TODO: we need to rethink this document.  Instead we should have a larger one that contains
// all the analytics of the session and also include placements
@Data
@Document(collection = "interactive_session_results")
public class InteractiveSessionResult {
    @Id
    private String id;

    @Indexed
    private String interactiveSessionId; // reference to InteractiveSession

    private List<PlayerPlacement> placements; // ordered by placement (1st first)

    private Instant endedAt = Instant.now();
}
