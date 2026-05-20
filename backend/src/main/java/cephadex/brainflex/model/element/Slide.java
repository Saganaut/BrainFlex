/**
 * Non-interactive element used for title screens, section dividers, callouts,
 * and the end card. Carries no answer fields; the round just advances when the
 * display timer expires (or the host clicks Next in TURN_BASED).
 *
 * Inherits `bestAnswerMode` / `bestAnswerTitle` / `bestAnswerBonus` defaults
 * from the interface — slides are never eligible for best-answer voting.
 *
 * The trailing block of audience/display options (resultsDisplayType through
 * participantInformation) is shared with the rest of the deck-element family
 * conceptually but only declared here for now; other kinds will inherit the
 * same controls in a follow-up once the UI surfaces them everywhere. New
 * boolean / int fields are declared as primitives, so every payload from the
 * frontend must include defaults (Jackson cannot deserialize null into a
 * primitive — see useCreateDashboard.buildNewElement).
 */
package cephadex.brainflex.model.element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.JoinType;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.ResultsDisplayType;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.enums.SlideKind;

public record Slide(
        String id,
        SlideKind slideKind,
        String publicKey,
        String privateKey,
        String title,
        Map<String, Object> styledTitle,
        String body,                  // markdown-friendly; rendered as plain text for v1
        boolean scored,
        boolean survey,
        Integer multipleSelections,
        ResponseMode responseMode,
        // shared chrome
        int displaySeconds,
        String speakerNotes,
        Image background,
        Image image,
        String videoUrl,
        String audioUrl,
        MediaPosition mediaPosition,
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
        return ElementKind.SLIDE;
    }
}
