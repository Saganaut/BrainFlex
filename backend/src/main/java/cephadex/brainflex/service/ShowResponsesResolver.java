/**
 * Walks the runtime show-responses cascade and returns a concrete
 * {@link ShowResponsesMode} (never {@code INHERIT}).
 *
 * Order — most-specific runtime override wins, then bubbles up:
 *   element value if not INHERIT
 *   → deck value if not INHERIT
 *   → session value if not INHERIT
 *   → per-format default (GAME → INSTANT, PRESENTATION → ON_CLICK).
 *
 * Note: this is the inverse of the *authored content* cascade (where
 * element-level values win because the author was most specific). Runtime
 * behavior cascades the other direction — the host's choice at run time wins,
 * the deck is a suggestion, the element is the per-question knob.
 *
 * Stateless on purpose; no Spring bean wiring required — callers
 * instantiate directly or hold a static reference.
 */
package cephadex.brainflex.service;

import org.springframework.stereotype.Component;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.session.InteractiveSession;

@Component
public class ShowResponsesResolver {

    public ShowResponsesMode resolve(
            InteractiveSession session,
            Deck deck,
            DeckElement element) {
        ShowResponsesMode elementValue = element == null ? null : element.showResponses();
        if (elementValue != null && elementValue != ShowResponsesMode.INHERIT) {
            return elementValue;
        }
        ShowResponsesMode deckValue = deck == null ? null : deck.getContent().getShowResponses();
        if (deckValue != null && deckValue != ShowResponsesMode.INHERIT) {
            return deckValue;
        }
        ShowResponsesMode sessionValue = (session == null || session.getContent().getSettings() == null)
                ? null
                : session.getContent().getSettings().getShowResponses();
        if (sessionValue != null && sessionValue != ShowResponsesMode.INHERIT) {
            return sessionValue;
        }
        SessionFormat format = session == null ? SessionFormat.GAME : session.getContent().getFormat();
        return defaultFor(format);
    }

    /** Per-format default when every level of the cascade returned INHERIT. */
    public ShowResponsesMode defaultFor(SessionFormat format) {
        return format == SessionFormat.PRESENTATION
                ? ShowResponsesMode.ON_CLICK
                : ShowResponsesMode.INSTANT;
    }
}
