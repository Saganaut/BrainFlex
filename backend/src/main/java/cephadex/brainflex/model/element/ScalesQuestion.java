/**
 * Likert-style: rate each statement on a numeric scale. Default unscored
 * (Pulse use case — show the distribution). When `scored=true` and
 * `correctRatings` is provided, the question becomes "guess the average
 * rating" — submitted rating compared against the correct one per statement.
 *
 * `scored` is the shared interface field — same semantic, just lifted to
 * the interface so every element kind exposes it uniformly.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record ScalesQuestion(
        String id,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String prompt,
        List<ScaleStatement> statements,
        int scaleMin,                  // typically 1
        int scaleMax,                  // typically 5 or 7
        String minLabel,               // anchor text, e.g. "Strongly disagree"
        String maxLabel,               // anchor text, e.g. "Strongly agree"
        List<Integer> correctRatings,  // when scored: one rating per statement, in order
        // scoring
        int pointValue,
        Difficulty difficulty,
        boolean scored,                // false: Pulse-style; just collect data
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // best-answer modifier
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
        MediaPosition mediaPosition,
        // shared metadata (chunk 10b)
        String createdByUserId,
        String lastEditedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tagIds,
        String mediaCaption,
        String altText,
        boolean reactionsEnabled,
        Integer version
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.SCALES;
    }
}
