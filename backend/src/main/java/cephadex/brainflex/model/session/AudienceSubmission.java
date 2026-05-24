/**
 * Free-text submission from a player during a Q&A round (or future Best Answer
 * variants that accept open content). Lives in its own collection because
 * submissions span all participants of a interactiveSession and the host moderates them
 * live; embedding on InteractiveSession would force a full write per submission.
 */
package cephadex.brainflex.model.session;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cephadex.brainflex.model.enums.SubmissionModeration;
import cephadex.brainflex.model.shared.UserSnapshot;
import lombok.Data;

@Data
@Document(collection = "audience_submissions")
public class AudienceSubmission {
    @Id
    private String id;

    @Indexed
    private String interactiveSessionId;

    @Indexed
    private String elementId;

    /** Denormalized submitter display snapshot, frozen at submit time. */
    private UserSnapshot user;

    // TODO: THis should be using playerId not userId
    @JsonIgnore
    public String getUserId() {
        return user == null ? null : user.userId();
    }

    @JsonIgnore
    public String getUserName() {
        return user == null ? null : user.name();
    }

    @JsonIgnore
    public boolean isGuest() {
        return user != null && user.guest();
    }

    private String text;
    private SubmissionModeration status = SubmissionModeration.PENDING;
    private int upvotes;

    /**
     * Symmetric to {@link #upvotes}. Negative scores don't hide a submission — the
     * host
     * still has the final say via {@link #moderatedByUserId} — but they re-rank it.
     */
    private int downvotes;

    /**
     * Set when a moderator/host acts on this submission (approve/hide/etc.). Lets
     * the
     * moderator audit log distinguish "auto-approved" rows from human-touched rows.
     */
    private String moderatedByUserId;
    private Instant moderatedAt;

    /**
     * Free-form rationale the moderator typed when they acted. Surfaced to
     * moderators
     * only — never sent to the submitter.
     */
    private String moderationReason;

    private Instant submittedAt = Instant.now();
}
