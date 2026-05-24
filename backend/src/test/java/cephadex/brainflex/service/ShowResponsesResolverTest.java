/**
 * Covers the full ShowResponses cascade matrix:
 *   element value if not INHERIT → deck value if not INHERIT → session value if not INHERIT
 *   → per-format default (GAME → INSTANT, PRESENTATION → ON_CLICK).
 *
 * Each level pre-empts the levels below; tests assert that explicit values at
 * the same level win against the per-format default and that INHERIT at every
 * level falls all the way through to that default.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.block.SlideBlock;
import cephadex.brainflex.model.enums.JoinType;
import cephadex.brainflex.model.enums.ResultsDisplayType;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.enums.SlideKind;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.InteractiveSessionSettings;

class ShowResponsesResolverTest {

    private final ShowResponsesResolver resolver = new ShowResponsesResolver();

    @Test
    void elementValue_winsWhenNotInherit() {
        ShowResponsesMode out = resolver.resolve(
                session(SessionFormat.GAME, ShowResponsesMode.INSTANT),
                deck(ShowResponsesMode.PRIVATE),
                slide(ShowResponsesMode.ON_CLICK));
        assertEquals(ShowResponsesMode.ON_CLICK, out);
    }

    @Test
    void deckValue_winsWhenElementIsInherit() {
        ShowResponsesMode out = resolver.resolve(
                session(SessionFormat.GAME, ShowResponsesMode.INSTANT),
                deck(ShowResponsesMode.PRIVATE),
                slide(ShowResponsesMode.INHERIT));
        assertEquals(ShowResponsesMode.PRIVATE, out);
    }

    @Test
    void sessionValue_winsWhenElementAndDeckBothInherit() {
        ShowResponsesMode out = resolver.resolve(
                session(SessionFormat.GAME, ShowResponsesMode.PRIVATE),
                deck(ShowResponsesMode.INHERIT),
                slide(ShowResponsesMode.INHERIT));
        assertEquals(ShowResponsesMode.PRIVATE, out);
    }

    @Test
    void gameFormat_defaultsToInstant_whenEverythingInherits() {
        ShowResponsesMode out = resolver.resolve(
                session(SessionFormat.GAME, ShowResponsesMode.INHERIT),
                deck(ShowResponsesMode.INHERIT),
                slide(ShowResponsesMode.INHERIT));
        assertEquals(ShowResponsesMode.INSTANT, out);
    }

    @Test
    void presentationFormat_defaultsToOnClick_whenEverythingInherits() {
        ShowResponsesMode out = resolver.resolve(
                session(SessionFormat.PRESENTATION, ShowResponsesMode.INHERIT),
                deck(ShowResponsesMode.INHERIT),
                slide(ShowResponsesMode.INHERIT));
        assertEquals(ShowResponsesMode.ON_CLICK, out);
    }

    @Test
    void nullSessionAndDeck_stillResolvesViaPerFormatDefault() {
        // Element INHERIT, no session, no deck → fall back to GAME default.
        assertEquals(ShowResponsesMode.INSTANT,
                resolver.resolve(null, null, slide(ShowResponsesMode.INHERIT)));
    }

    private static InteractiveSession session(SessionFormat format, ShowResponsesMode mode) {
        InteractiveSession s = new InteractiveSession();
        s.getContent().setFormat(format);
        InteractiveSessionSettings settings = new InteractiveSessionSettings();
        settings.setShowResponses(mode);
        s.getContent().setSettings(settings);
        return s;
    }

    private static Deck deck(ShowResponsesMode mode) {
        Deck d = new Deck();
        d.getContent().setShowResponses(mode);
        return d;
    }

    private static Slide slide(ShowResponsesMode mode) {
        return new Slide(
                "el-1", SlideKind.TITLE, null, java.util.List.<SlideBlock>of(),
                ResultsDisplayType.PIE_CHART, false, 1, false,
                JoinType.INSTRUCTIONS_BAR, false, false, mode,
                null, null, null,
                TestElementChromes.slideChrome("el-1", "Title"));
    }
}
