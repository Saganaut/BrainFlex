/**
 * A vote in the second phase of a Best-Answer-mode question. Each player picks
 * one of the (anonymized) submissions from the SUBMIT phase. Tallies determine
 * which submission wins the bonus.
 *
 * Unique-per-(showcase, element, voter) so the vote is overwritable but final
 * per voter. (Enforced in the service, not via Mongo unique index, so we can
 * support "change your vote" without a delete-then-insert dance.)
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

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
    private String showcaseId;

    @Indexed
    private String elementId;

    private String submissionId;
    private String voterUserId;

    private LocalDateTime votedAt = LocalDateTime.now();
}
