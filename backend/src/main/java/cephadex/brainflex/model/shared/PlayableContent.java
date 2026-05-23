/**
 * The "playable bits" of a deck — the subset of authored content that an
 * InteractiveSession actually needs at runtime (lobby, gameplay, scoring,
 * review). Embedded by both {@link Deck} (as the authored content) and
 * {@link InteractiveSession} (as a frozen copy taken at session-create).
 *
 * <p>This is the value-object half of the template → instance pattern. The
 * template (Deck) owns one PlayableContent forever; the instance (Session)
 * gets a deep copy at create time and is free to mutate it for per-session
 * overrides (e.g. host flips the format, shuffles elements). Mutating a
 * session's copy never reaches back to the deck.
 *
 * <p>Mutable @Data by design — overrides on a running session are expressed
 * as direct setter calls on {@code session.content.setX(...)}, so there is
 * no shadow / fallback logic. The mutability is also what lets
 * {@code InteractiveSessionService.startGame} shuffle elements in place.
 *
 * <p>Template-only fields (visibility, publishStatus, tagIds, ratings,
 * lineage, etc.) stay on {@link Deck} and are intentionally NOT part of
 * this snapshot — they are meaningless on a running session.
 */
package cephadex.brainflex.model.shared;

import java.util.ArrayList;
import java.util.List;

import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import lombok.Data;
import cephadex.brainflex.model.session.InteractiveSessionSettings;

@Data
public class PlayableContent {
    private String name;
    private String description;

    private Image cover;
    private Image background;
    private String themeId;

    private List<DeckElement> elements = new ArrayList<>();

    private SessionFormat format = SessionFormat.GAME;
    private ShowResponsesMode showResponses = ShowResponsesMode.INHERIT;
    private InteractiveSessionSettings settings = new InteractiveSessionSettings();
}
