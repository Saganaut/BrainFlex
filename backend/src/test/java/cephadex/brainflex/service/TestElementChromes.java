/**
 * Tiny factory for {@link ElementChrome} instances inside unit tests. Removes
 * the ~30-arg "chrome tail" that every test record constructor used to repeat
 * after the chunk-25 refactor.
 *
 * Every other test field (style, media, audit) is filled with the most
 * defensible "blank" value (null / empty / version=1 / reactions on) so a
 * caller only specifies what their assertion actually depends on.
 */
package cephadex.brainflex.service;

import java.util.List;

import cephadex.brainflex.model.element.ElementChrome;
import cephadex.brainflex.model.enums.BestAnswerScoring;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.ResponseMode;

final class TestElementChromes {

    private TestElementChromes() {}

    /** Standard scored, single-select question chrome. */
    static ElementChrome scored(String id, String title) {
        return chrome(id, title, true, false, null, 30,
                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE);
    }

    /** Standard survey (unscored) chrome. */
    static ElementChrome survey(String id, String title) {
        return chrome(id, title, false, true, null, 30,
                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE);
    }

    /** Non-interactive (slide-style) chrome. */
    static ElementChrome slideChrome(String id, String title) {
        return chrome(id, title, false, false, null, 6,
                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE);
    }

    /** Fully-specified factory for tests that exercise atypical chrome. */
    static ElementChrome chrome(String id, String title,
                                boolean scored, boolean survey,
                                Integer multipleSelections, int displaySeconds,
                                boolean bestAnswerMode, String bestAnswerTitle,
                                int bestAnswerPoints, BestAnswerScoring bestAnswerScoring) {
        return new ElementChrome(
                "pub-" + id, "priv-" + id, title, null, null,
                scored, survey, multipleSelections, ResponseMode.ACCEPTING_RESPONSES,
                displaySeconds, null,
                null, null, null, null, null, null, MediaPosition.NONE,
                bestAnswerMode, bestAnswerTitle, bestAnswerPoints, bestAnswerScoring,
                null, null, null, null, List.<String>of(),
                null, null, true, 1);
    }
}
