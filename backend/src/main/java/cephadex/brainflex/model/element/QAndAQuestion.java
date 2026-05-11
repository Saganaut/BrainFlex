/**
 * Slido-style audience Q&A. Players submit free-text questions; the host
 * pins / dismisses them in real time. Never scored — `pointValue` is forced
 * to 0 server-side. Submissions live in the `audience_submissions` collection.
 *
 * `bestAnswerMode` is meaningless for Q&A and ignored by the scorer.
 */
package cephadex.brainflex.model.element;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;

public record QAndAQuestion(
        String id,
        String prompt,                 // e.g. "Ask the host anything"
        int maxSubmissionsPerPlayer,   // 0 = unlimited
        boolean allowVoting,           // upvote others' submissions
        boolean autoApprove,           // skip the moderation queue
        // scoring (forced to 0)
        int pointValue,
        Difficulty difficulty,
        // best-answer modifier (ignored)
        boolean bestAnswerMode,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String hostNotes,
        String backgroundImageUrl,
        String imageUrl,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.Q_AND_A;
    }
}
