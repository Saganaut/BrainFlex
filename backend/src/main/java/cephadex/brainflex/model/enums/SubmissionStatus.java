/**
 * Moderation state of an AudienceSubmission.
 *   PENDING   — submitted, awaiting host action
 *   PINNED    — host promoted; renders prominently
 *   DISMISSED — host hid; still stored for review
 */
package cephadex.brainflex.model.enums;

public enum SubmissionStatus {
    PENDING,
    PINNED,
    DISMISSED
}
