/**
 * Non-interactive element used for title screens, section dividers, callouts,
 * and the end card. Carries no answer fields; the round just advances when the
 * display timer expires (or the host clicks Next in TURN_BASED).
 *
 * Slides hold a chrome with {@code bestAnswerMode=false} — slides are never
 * eligible for best-answer voting, and the chrome's best-answer fields
 * carry sentinel values for the interface contract.
 *
 * Slide-only fields (resultsDisplayType through participantInformation) cover
 * the audience/display tab in the deck editor; they're conceptually shared
 * with question kinds but only declared here for now. New boolean / int
 * fields are primitives, so every payload from the frontend must include
 * defaults (Jackson cannot deserialize null into a primitive — see
 * useCreateDashboard.buildNewElement).
 */
package cephadex.brainflex.model.element;

import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.JoinType;
import cephadex.brainflex.model.enums.ResultsDisplayType;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.enums.SlideKind;

public record Slide(
        String id,
        SlideKind slideKind,
        // Legacy single rich-text body. Replaced by `blocks` (chunk 10c); kept
        // for one release as a fallback so existing decks render until the
        // SlideBlocksMigrationRunner backfills `blocks` from `body`. The
        // {@link #effectiveBlocks()} helper hides this fallback from callers.
        String body,
        // Polymorphic stacked content (chunk 10c). When set, takes precedence
        // over `body`. Null/empty + non-empty `body` is the unmigrated shape.
        List<SlideBlock> blocks,
        // audience / display options (first "Edit slide" tab in the deck editor)
        ResultsDisplayType resultsDisplayType,
        boolean multipleSelectionsEnabled,
        int selectionsPerParticipant,
        boolean showResultsAsPercentage,
        JoinType joinType,            // legacy — read-only; new writes use showQrCode + showJoinInformation
        boolean showJoinInformation,
        boolean showQrCode,           // chunk 21 — independent of showJoinInformation
        ShowResponsesMode showResponses,
        String heading,
        Map<String, Object> participantInformation,  // TipTap/ProseMirror rich-text doc
        // per-kind ergonomics (chunk 10)
        Integer autoAdvanceSeconds,                  // null = host advances manually; n = auto-next after n seconds
        // shared chrome (chunk 25)
        ElementChrome chrome
) implements DeckElement {

    @Override
    public ElementKind kind() {
        return ElementKind.SLIDE;
    }

    /**
     * Returns the authoritative block list for rendering. When `blocks` is
     * populated, hands it back unchanged. When `blocks` is null/empty but
     * `body` carries a non-empty legacy value, wraps the body string in a
     * single {@link BodyBlock} so renderers (and the host preview) can stay on
     * a single read path. Returns an empty list when both are unset.
     *
     * Intentionally does not mutate the record — the migration runner is the
     * one place that persists the wrap.
     */
    public List<SlideBlock> effectiveBlocks() {
        if (blocks != null && !blocks.isEmpty()) {
            return blocks;
        }
        if (body != null && !body.isBlank()) {
            return List.of(new BodyBlock("legacy-body-" + (id == null ? "anon" : id), body));
        }
        return List.of();
    }
}
