/**
 * Shared "chrome" carried by every {@link DeckElement}. Extracted in chunk 25
 * so that adding a new universal element field (e.g. a new best-answer
 * tunable, a new audit stamp, a new media slot) is a one-file edit instead
 * of 13 record edits.
 *
 * Layout deliberately groups related fields:
 *   1. Identity tokens (publicKey / privateKey) for Mentimeter-style routing.
 *   2. Heading (title + styledTitle TipTap doc).
 *   3. Response config (scored / survey / multipleSelections / responseMode).
 *   4. Display config (displaySeconds + speakerNotes).
 *   5. Media (background, image, video, audio, mediaPosition).
 *   6. Best-answer modifier (bestAnswerMode etc.).
 *   7. Audit + tag/caption/version metadata.
 *
 * `id` and `kind` stay on the host element record rather than living here —
 * `id` is the primary key and `kind` is the polymorphic discriminator. Every
 * other shared field is delegated through {@link DeckElement}'s default
 * accessors that call into this record.
 *
 * Mongo storage: nested under {@code chrome} on the element subdocument
 * (e.g. {@code deck.elements[].chrome.title}). The legacy
 * {@code @Field("bestAnswerBonus")} alias was dropped here — the storage path
 * is fresh, so we use the clean {@code chrome.bestAnswerPoints} name.
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.BestAnswerScoring;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

public record ElementChrome(
        // identity
        String publicKey,
        String privateKey,
        // heading
        String title,
        String titleLabel,           // pre-heading label (chunk 21) — e.g. "Question 3 of 9"
        Map<String, Object> styledTitle,
        // response config
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // display
        int displaySeconds,
        String speakerNotes,
        // media
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        String videoAssetId,
        String audioAssetId,
        MediaPosition mediaPosition,
        // best-answer modifier
        boolean bestAnswerMode,
        String bestAnswerTitle,
        int bestAnswerPoints,
        BestAnswerScoring bestAnswerScoring,
        // audit + metadata
        String createdByUserId,
        String lastEditedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tagIds,
        String mediaCaption,
        String altText,
        boolean reactionsEnabled,
        Integer version
) {

    /**
     * Returns a chrome with the audit-block fields replaced. Used by
     * {@link cephadex.brainflex.service.DeckElementCloner#withMetadata} so
     * provenance stamping doesn't need to know any kind-specific shape.
     */
    public ElementChrome withMetadata(
            String createdByUserId, String lastEditedByUserId,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            List<String> tagIds, String mediaCaption, String altText,
            boolean reactionsEnabled, Integer version) {
        return new ElementChrome(
                publicKey, privateKey, title, titleLabel, styledTitle,
                scored, survey, multipleSelections, responseMode,
                displaySeconds, speakerNotes,
                background, image, videoUrl, audioUrl,
                videoAssetId, audioAssetId, mediaPosition,
                bestAnswerMode, bestAnswerTitle, bestAnswerPoints, bestAnswerScoring,
                createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                tagIds, mediaCaption, altText, reactionsEnabled, version);
    }

    /**
     * Returns a chrome with {@code background} and {@code image} replaced.
     * Used by {@link cephadex.brainflex.service.DeckImageMapper} when
     * rehydrating Image references at read time.
     */
    public ElementChrome withImages(Image background, Image image) {
        return new ElementChrome(
                publicKey, privateKey, title, titleLabel, styledTitle,
                scored, survey, multipleSelections, responseMode,
                displaySeconds, speakerNotes,
                background, image, videoUrl, audioUrl,
                videoAssetId, audioAssetId, mediaPosition,
                bestAnswerMode, bestAnswerTitle, bestAnswerPoints, bestAnswerScoring,
                createdByUserId, lastEditedByUserId, createdAt, updatedAt,
                tagIds, mediaCaption, altText, reactionsEnabled, version);
    }
}
