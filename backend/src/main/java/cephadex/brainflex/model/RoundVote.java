/**
 * One vote cast by a player during the VOTE phase of a Best Answer round.
 * Embedded in `ShowcasePlayer.votes` so a player's full voting history travels
 * with their session record. The voted submission is referenced by its
 * `submissionId` (anonymous on the wire); the server maps submissionId →
 * userId at tally time using the `PlayerAnswer.submissionId` index built when
 * the SUBMIT phase ended.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RoundVote {
    private String elementId;
    private String votedSubmissionId;
    private LocalDateTime votedAt;
}
