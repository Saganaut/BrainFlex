/**
 * Discovers every {@link BestAnswerScoringStrategy} bean at startup and
 * indexes them by their {@link BestAnswerScoring} kind so the runtime can
 * resolve "what strategy does this element want?" with a map lookup instead
 * of a switch statement. Adding a new strategy is a one-file change.
 *
 * Resolution falls back to {@link BestAnswerScoring#POINTS_PER_VOTE} when an
 * element returns null (old documents persisted before chunk 24 lacked the
 * field entirely — the Mongo read leaves it null, and that should mean
 * "default" rather than "fail").
 */
package cephadex.brainflex.service.bestanswer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cephadex.brainflex.model.enums.BestAnswerScoring;

@Component
public class BestAnswerScoringRegistry {

    private final Map<BestAnswerScoring, BestAnswerScoringStrategy> byKind;

    public BestAnswerScoringRegistry(List<BestAnswerScoringStrategy> strategies) {
        EnumMap<BestAnswerScoring, BestAnswerScoringStrategy> map = new EnumMap<>(BestAnswerScoring.class);
        for (BestAnswerScoringStrategy s : strategies) {
            map.put(s.kind(), s);
        }
        this.byKind = map;
    }

    public BestAnswerScoringStrategy resolve(BestAnswerScoring kind) {
        BestAnswerScoring resolved = (kind == null) ? BestAnswerScoring.POINTS_PER_VOTE : kind;
        BestAnswerScoringStrategy strategy = byKind.get(resolved);
        if (strategy == null) {
            throw new IllegalStateException(
                    "No BestAnswerScoringStrategy registered for " + resolved);
        }
        return strategy;
    }
}
