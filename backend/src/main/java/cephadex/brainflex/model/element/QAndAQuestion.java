/**
 * Slido-style audience Q&A. Players submit free-text questions; the host
 * pins / dismisses them in real time. Never scored — `scored=false` is the
 * canonical signal (replaces the older "force `pointValue=0`" trick).
 * Submissions live in the `audience_submissions` collection.
 *
 * `bestAnswerMode` is meaningless for Q&A and ignored by the scorer.
 */
package cephadex.brainflex.model.element;

import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record QAndAQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,                 // e.g. "Ask the host anything"
        int maxSubmissionsPerPlayer,   // 0 = unlimited
        boolean allowVoting,           // upvote others' submissions
        boolean autoApprove,           // skip the moderation queue
        // scoring (forced to 0)
        int pointValue,
        Difficulty difficulty,
        boolean scored,                // always false for Q&A
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier (ignored)
        boolean bestAnswerMode,
        String bestAnswerTitle,
        int bestAnswerBonus,
        String explanation,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.Q_AND_A;
    }
}
