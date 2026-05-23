/**
 * A vote in the second phase of a Best-Answer-mode question. Each player picks
 * one of the (anonymized) submissions from the SUBMIT phase. Tallies determine
 * which submission wins the bonus.
 *
 * Unique-per-(interactiveSession, element, voter) so the vote is overwritable but final
 * per voter. (Enforced in the service, not via Mongo unique index, so we can
 * support "change your vote" without a delete-then-insert dance.)
 */
package cephadex.brainflex.model.session;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "best_answer_votes")
public class BestAnswerVote {
    @Id
    private String id;

    @Indexed
    private String interactiveSessionId;

    @Indexed
    private String elementId;

    private String submissionId;
    private String voterUserId;

    /** Multiplier applied to this vote in the tally. Defaults to 1 (one player, one
     *  vote). Game modes that want host weighting can set 2 when the voter is the host;
     *  the tallier consumes this verbatim. Stored on the vote so a re-tally is
     *  deterministic without re-resolving the voter. */
    private int weight = 1;

    private Instant votedAt = Instant.now();
}
