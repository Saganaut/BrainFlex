/**
 * Free-text submission from a player during a Q&A round (or future Best Answer
 * variants that accept open content). Lives in its own collection because
 * submissions span all participants of a showcase and the host moderates them
 * live; embedding on Showcase would force a full write per submission.
 */
package cephadex.brainflex.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.SubmissionStatus;
import lombok.Data;

@Data
@Document(collection = "audience_submissions")
public class AudienceSubmission {
    @Id
    private String id;

    @Indexed
    private String showcaseId;

    @Indexed
    private String elementId;

    private String userId;
    private String userName;
    private boolean guest;

    private String text;
    private SubmissionStatus status = SubmissionStatus.PENDING;
    private int upvotes;

    private LocalDateTime submittedAt = LocalDateTime.now();
}
