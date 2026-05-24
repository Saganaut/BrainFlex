/**
 * Unit tests for {@link DeckAnalyticsService}.
 *
 * Repositories are mocked; the goal is to pin down the per-element-kind
 * distribution shapes (since a regression in any one bucket is a silent
 * data-quality bug) and the running-average math.
 *
 * Coverage:
 *   - Every concrete AnswerPayload variant is bucketed correctly (count-style
 *     vs sum-style per the {@link DeckAnalyticsService} javadoc).
 *   - Slides + TimeoutAnswers contribute nothing.
 *   - Q_AND_A elements get presentedCount but no answer rollup (audience
 *     submissions are not on PlayerAnswer).
 *   - Survey-only kinds skip correctCount even when the scorer set
 *     {@code correct=false}.
 *   - {@code averageScore} aggregates per (player, game); {@code averageDurationMs}
 *     aggregates per game.
 *   - Reactions + total session chat populate the per-element stats.
 *   - Exceptions in any sub-step do not bubble out of recordSessionFinish.
 *   - PR3 — GAME / PRESENTATION finishes route into the matching
 *     {@link cephadex.brainflex.model.session.FormatRollup}; PRESENTATION leaves
 *     {@code averageScore} alone so unscored sessions don't pollute the
 *     game-mode score average; mixed decks populate both rollups.
 */
package cephadex.brainflex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cephadex.brainflex.model.answer.AllocationAnswer;
import cephadex.brainflex.model.answer.AnswerPayload;
import cephadex.brainflex.model.answer.DrawingAnswer;
import cephadex.brainflex.model.answer.GridAnswer;
import cephadex.brainflex.model.answer.MatchingAnswer;
import cephadex.brainflex.model.answer.McqAnswer;
import cephadex.brainflex.model.answer.NumberAnswer;
import cephadex.brainflex.model.answer.PlaceOnImageAnswer;
import cephadex.brainflex.model.answer.RankingAnswer;
import cephadex.brainflex.model.answer.ScalesAnswer;
import cephadex.brainflex.model.answer.TextAnswer;
import cephadex.brainflex.model.answer.TimeoutAnswer;
import cephadex.brainflex.model.answer.WordCloudAnswer;
import cephadex.brainflex.model.deck.DeckAnalytics;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.session.ElementStats;
import cephadex.brainflex.model.session.FormatRollup;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.InteractiveSessionPlayer;
import cephadex.brainflex.model.session.PlayerAnswer;
import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.repository.DeckAnalyticsRepository;
import cephadex.brainflex.repository.InteractiveSessionChatMessageRepository;
import cephadex.brainflex.repository.ReactionRepository;

@ExtendWith(MockitoExtension.class)
class DeckAnalyticsServiceTest {

    @Mock
    private DeckAnalyticsRepository analyticsRepository;
    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private InteractiveSessionChatMessageRepository chatRepository;

    @InjectMocks
    private DeckAnalyticsService service;

    @BeforeEach
    @SuppressWarnings("unused")
    void emptyRollupByDefault() {
        // Most tests start with an empty rollup and just inspect the captured
        // save. lenient() because a handful of tests (e.g. null-deck short-circuit)
        // never reach the repository.
        lenient().when(analyticsRepository.findById(anyString())).thenReturn(Optional.empty());
        lenient().when(analyticsRepository.save(any(DeckAnalytics.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Game-level rollup ────────────────────────────────────────────

    @Test
    void recordSessionFinish_FirstPlay_InitializesCountersAndAverages() {
        InteractiveSession session = baseSession();
        session.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        session.setEndedAt(Instant.parse("2026-05-20T08:30:00Z"));
        session.getPlayers().add(playerWithScore("p-1", 100, 0.80));
        session.getPlayers().add(playerWithScore("p-2", 50, 0.40));

        service.recordSessionFinish(session);

        DeckAnalytics saved = captureSaved();
        assertEquals(1, saved.getTotalPlays());
        assertEquals(2, saved.getTotalPlayers());
        assertEquals(75.0, saved.getAverageScore(), 0.0001);
        assertEquals(0.60, saved.getAverageAccuracy(), 0.0001);
        assertEquals(30L * 60_000L, saved.getAverageDurationMs(), "30 min duration");
        assertEquals(session.getEndedAt(), saved.getLastPlayedAt());
        // {@code updatedAt} is now populated by Spring Data's {@code @LastModifiedDate}
        // hook on real Mongo save; this unit test mocks the repository, so the
        // auditing infrastructure never fires and the field stays null. Production
        // behaviour is covered by the integration tests against a live MongoDB.
    }

    @Test
    void recordSessionFinish_TwoPlays_AveragesAreRunningWelford() {
        // First game: one player, score 100.
        InteractiveSession game1 = baseSession();
        game1.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        game1.setEndedAt(Instant.parse("2026-05-20T08:10:00Z")); // 10 min
        game1.getPlayers().add(playerWithScore("p-1", 100, 1.0));
        service.recordSessionFinish(game1);

        DeckAnalytics afterGame1 = captureSaved();
        // Second game: same deck, returns the stored rollup; one player, score 50.
        when(analyticsRepository.findById("deck-1")).thenReturn(Optional.of(afterGame1));
        InteractiveSession game2 = baseSession();
        game2.setStartedAt(Instant.parse("2026-05-21T08:00:00Z"));
        game2.setEndedAt(Instant.parse("2026-05-21T08:20:00Z")); // 20 min
        game2.getPlayers().add(playerWithScore("p-2", 50, 0.5));
        service.recordSessionFinish(game2);

        DeckAnalytics finalRollup = lastSaved();
        assertEquals(2, finalRollup.getTotalPlays());
        assertEquals(2, finalRollup.getTotalPlayers());
        assertEquals(75.0, finalRollup.getAverageScore(), 0.0001);
        assertEquals(0.75, finalRollup.getAverageAccuracy(), 0.0001);
        assertEquals(15L * 60_000L, finalRollup.getAverageDurationMs(), "mean of 10 + 20 min");
    }

    // ── Slide + Q&A + Timeout edge cases ──────────────────────────────

    @Test
    void recordSessionFinish_Slide_SkippedFromPerElementRollup() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(slide("slide-1"));
        session.getPlayers().add(playerWithScore("p-1", 100, 1.0));

        service.recordSessionFinish(session);

        DeckAnalytics saved = captureSaved();
        assertFalse(saved.getPerElement().containsKey("slide-1"),
                "Slides never appear in perElement");
    }

    @Test
    void recordSessionFinish_QAndA_PresentedButNoAnswerRollup() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(qAndA("qa-1"));
        InteractiveSessionPlayer p = playerWithScore("p-1", 0, 0.0);
        // No PlayerAnswer for Q&A — submissions live in audience_submissions.
        session.getPlayers().add(p);

        service.recordSessionFinish(session);

        ElementStats stats = captureSaved().getPerElement().get("qa-1");
        assertEquals(1, stats.getPresentedCount());
        assertEquals(0, stats.getAnsweredCount());
        assertTrue(stats.getDistribution().isEmpty());
    }

    @Test
    void recordSessionFinish_TimeoutAnswer_DoesNotCountAsAnswered() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("mcq-1"));
        InteractiveSessionPlayer p = playerWithScore("p-1", 0, 0.0);
        p.getAnswers().add(answer("mcq-1", new TimeoutAnswer(), false, 0L));
        session.getPlayers().add(p);

        service.recordSessionFinish(session);

        ElementStats stats = captureSaved().getPerElement().get("mcq-1");
        assertEquals(1, stats.getPresentedCount());
        assertEquals(0, stats.getAnsweredCount(), "timeout != answered");
        assertEquals(0, stats.getCorrectCount());
        assertTrue(stats.getDistribution().isEmpty());
    }

    // ── Per-kind bucketing ─────────────────────────────────────────────

    @Test
    void recordSessionFinish_McqAnswer_CountsHitsPerOptionId() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("mcq-1"));
        addAnswer(session, "p-1", "mcq-1", new McqAnswer(List.of("opt-a")), true, 1500);
        addAnswer(session, "p-2", "mcq-1", new McqAnswer(List.of("opt-b")), false, 2000);
        addAnswer(session, "p-3", "mcq-1", new McqAnswer(List.of("opt-a", "opt-b")), false, 3000);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("mcq-1").getDistribution();
        assertEquals(2, dist.get("opt-a"));
        assertEquals(2, dist.get("opt-b"));
    }

    @Test
    void recordSessionFinish_NumberAnswer_BucketsByIntegerFloor() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("num-1")); // kind doesn't matter for bucketing
        addAnswer(session, "p-1", "num-1", new NumberAnswer(42.0), true, 1000);
        addAnswer(session, "p-2", "num-1", new NumberAnswer(42.7), false, 1500);
        addAnswer(session, "p-3", "num-1", new NumberAnswer(50.0), false, 800);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("num-1").getDistribution();
        assertEquals(2, dist.get("42"));
        assertEquals(1, dist.get("50"));
    }

    @Test
    void recordSessionFinish_TextAnswer_NormalizesCaseAndTrims() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("text-1"));
        addAnswer(session, "p-1", "text-1", new TextAnswer("  Frodo "), true, 0);
        addAnswer(session, "p-2", "text-1", new TextAnswer("frodo"), true, 0);
        addAnswer(session, "p-3", "text-1", new TextAnswer("Sam"), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("text-1").getDistribution();
        assertEquals(2, dist.get("frodo"));
        assertEquals(1, dist.get("sam"));
    }

    @Test
    void recordSessionFinish_RankingAnswer_SumsPlacementsPerItem() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("rank-1"));
        // Player 1 ranks item-x first, item-y second.
        addAnswer(session, "p-1", "rank-1",
                new RankingAnswer(List.of("item-x", "item-y")), true, 0);
        // Player 2 ranks item-y first, item-x second.
        addAnswer(session, "p-2", "rank-1",
                new RankingAnswer(List.of("item-y", "item-x")), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("rank-1").getDistribution();
        assertEquals(1, dist.get("item-x"), "0 + 1 = 1");
        assertEquals(1, dist.get("item-y"), "1 + 0 = 1");
    }

    @Test
    void recordSessionFinish_ScalesAnswer_SumsRatingsPerStatement() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("scales-1"));
        addAnswer(session, "p-1", "scales-1",
                new ScalesAnswer(Map.of("s-a", 5, "s-b", 3)), true, 0);
        addAnswer(session, "p-2", "scales-1",
                new ScalesAnswer(Map.of("s-a", 4, "s-b", 2)), true, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("scales-1").getDistribution();
        assertEquals(9, dist.get("s-a"));
        assertEquals(5, dist.get("s-b"));
    }

    @Test
    void recordSessionFinish_GridAnswer_CountsCellHits() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("grid-1"));
        addAnswer(session, "p-1", "grid-1", new GridAnswer(Set.of(3, 5)), true, 0);
        addAnswer(session, "p-2", "grid-1", new GridAnswer(Set.of(3, 7)), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("grid-1").getDistribution();
        assertEquals(2, dist.get("3"));
        assertEquals(1, dist.get("5"));
        assertEquals(1, dist.get("7"));
    }

    @Test
    void recordSessionFinish_PlaceOnImageAnswer_BucketsTo10x10Grid() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("place-1"));
        // (0.05, 0.05) → (row 0, col 0); (0.07, 0.04) → also (0, 0); (0.55, 0.95) → (9,
        // 5)
        addAnswer(session, "p-1", "place-1", new PlaceOnImageAnswer(0.05, 0.05), true, 0);
        addAnswer(session, "p-2", "place-1", new PlaceOnImageAnswer(0.07, 0.04), true, 0);
        addAnswer(session, "p-3", "place-1", new PlaceOnImageAnswer(0.55, 0.95), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("place-1").getDistribution();
        assertEquals(2, dist.get("0,0"));
        assertEquals(1, dist.get("9,5"));
    }

    @Test
    void recordSessionFinish_WordCloudAnswer_NormalizesAndCounts() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("wc-1"));
        addAnswer(session, "p-1", "wc-1", new WordCloudAnswer(List.of("Hope", "Faith")), false, 0);
        addAnswer(session, "p-2", "wc-1", new WordCloudAnswer(List.of("hope", "love")), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("wc-1").getDistribution();
        assertEquals(2, dist.get("hope"));
        assertEquals(1, dist.get("faith"));
        assertEquals(1, dist.get("love"));
    }

    @Test
    void recordSessionFinish_AllocationAnswer_SumsPointsPerOption() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("alloc-1"));
        addAnswer(session, "p-1", "alloc-1",
                new AllocationAnswer(Map.of("opt-a", 60, "opt-b", 40)), false, 0);
        addAnswer(session, "p-2", "alloc-1",
                new AllocationAnswer(Map.of("opt-a", 30, "opt-b", 70)), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("alloc-1").getDistribution();
        assertEquals(90, dist.get("opt-a"));
        assertEquals(110, dist.get("opt-b"));
    }

    @Test
    void recordSessionFinish_MatchingAnswer_BucketsByPairString() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("match-1"));
        addAnswer(session, "p-1", "match-1",
                new MatchingAnswer(Map.of("L-1", "R-x", "L-2", "R-y")), true, 0);
        addAnswer(session, "p-2", "match-1",
                new MatchingAnswer(Map.of("L-1", "R-x", "L-2", "R-z")), false, 0);

        service.recordSessionFinish(session);
        Map<String, Integer> dist = captureSaved().getPerElement().get("match-1").getDistribution();
        assertEquals(2, dist.get("L-1>R-x"));
        assertEquals(1, dist.get("L-2>R-y"));
        assertEquals(1, dist.get("L-2>R-z"));
    }

    @Test
    void recordSessionFinish_DrawingAnswer_RecordsAnsweredCountButEmptyDistribution() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("draw-1"));
        addAnswer(session, "p-1", "draw-1", new DrawingAnswer(List.of()), false, 1000);
        addAnswer(session, "p-2", "draw-1", new DrawingAnswer(List.of()), false, 1500);

        service.recordSessionFinish(session);
        ElementStats stats = captureSaved().getPerElement().get("draw-1");
        assertEquals(2, stats.getAnsweredCount());
        assertTrue(stats.getDistribution().isEmpty(), "Drawing has no per-stroke bucketing");
        assertEquals(0, stats.getCorrectCount(), "survey-only kind never bumps correctCount");
    }

    // ── Aggregations attached to the element ───────────────────────────

    @Test
    void recordSessionFinish_AverageTimeMs_IsTotalDividedByAnsweredCount() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("mcq-1"));
        addAnswer(session, "p-1", "mcq-1", new McqAnswer(List.of("opt-a")), true, 1000);
        addAnswer(session, "p-2", "mcq-1", new McqAnswer(List.of("opt-a")), true, 3000);

        service.recordSessionFinish(session);
        ElementStats stats = captureSaved().getPerElement().get("mcq-1");
        assertEquals(4000L, stats.getTotalTimeMs());
        assertEquals(2000.0, stats.getAverageTimeMs(), 0.0001);
    }

    @Test
    void recordSessionFinish_ReactionRepository_FoldedIntoElementStats() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("mcq-1"));
        addAnswer(session, "p-1", "mcq-1", new McqAnswer(List.of("opt-a")), true, 0);
        when(reactionRepository.countByInteractiveSessionIdAndElementId("session-1", "mcq-1"))
                .thenReturn(7L);

        service.recordSessionFinish(session);
        assertEquals(7, captureSaved().getPerElement().get("mcq-1").getReactionsReceived());
    }

    @Test
    void recordSessionFinish_TotalSessionChat_AttachesToFirstScoredElement() {
        InteractiveSession session = baseSession();
        session.getContent().getElements().add(slide("slide-1")); // skipped
        session.getContent().getElements().add(mcqElement("mcq-1")); // gets the chat count
        session.getContent().getElements().add(mcqElement("mcq-2"));
        addAnswer(session, "p-1", "mcq-1", new McqAnswer(List.of("opt-a")), true, 0);
        addAnswer(session, "p-1", "mcq-2", new McqAnswer(List.of("opt-a")), true, 0);
        when(chatRepository.countByInteractiveSessionId("session-1")).thenReturn(12L);

        service.recordSessionFinish(session);
        DeckAnalytics saved = captureSaved();
        assertEquals(12, saved.getPerElement().get("mcq-1").getChatMessagesDuringRound());
        assertEquals(0, saved.getPerElement().get("mcq-2").getChatMessagesDuringRound());
    }

    // ── PR3 — game / presentation format split ────────────────────────

    @Test
    void recordSessionFinish_GameOnly_PopulatesGameRollupNotPresentation() {
        InteractiveSession session = baseSession();
        session.getContent().setFormat(SessionFormat.GAME);
        session.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        session.setEndedAt(Instant.parse("2026-05-20T08:30:00Z"));
        session.getPlayers().add(playerWithScore("p-1", 100, 0.80));
        session.getPlayers().add(playerWithScore("p-2", 50, 0.40));

        service.recordSessionFinish(session);

        DeckAnalytics saved = captureSaved();
        FormatRollup game = saved.getGameRollup();
        FormatRollup pres = saved.getPresentationRollup();

        assertNotNull(game, "game rollup should be initialised on first finish");
        assertEquals(1, game.getSessionCount());
        assertEquals(2, game.getParticipantCount());
        assertEquals(75.0, game.getAverageScore(), 0.0001);
        assertEquals(0.60, game.getAverageAccuracy(), 0.0001);
        assertEquals(30L * 60_000L, game.getAverageDurationMs());
        assertEquals(session.getEndedAt(), game.getLastRunAt());

        assertNotNull(pres, "presentation rollup should be initialised (empty) alongside");
        assertEquals(0, pres.getSessionCount(),
                "no presentation finishes yet → presentation rollup stays empty");
        assertEquals(0.0, pres.getAverageScore(), 0.0001);
        assertNull(pres.getLastRunAt());
    }

    @Test
    void recordSessionFinish_PresentationOnly_LeavesScoreAtZero() {
        // PRESENTATION sessions typically run unscored — averaging zeros into
        // averageScore would make a real game's average look worse later if
        // the same deck mixes formats. The recorder skips that update.
        InteractiveSession session = baseSession();
        session.getContent().setFormat(SessionFormat.PRESENTATION);
        session.setStartedAt(Instant.parse("2026-05-20T09:00:00Z"));
        session.setEndedAt(Instant.parse("2026-05-20T09:15:00Z"));
        session.getPlayers().add(playerWithScore("p-1", 0, 0.50));
        session.getPlayers().add(playerWithScore("p-2", 0, 0.30));

        service.recordSessionFinish(session);

        DeckAnalytics saved = captureSaved();
        FormatRollup pres = saved.getPresentationRollup();
        FormatRollup game = saved.getGameRollup();

        assertEquals(1, pres.getSessionCount());
        assertEquals(2, pres.getParticipantCount());
        assertEquals(0.0, pres.getAverageScore(), 0.0001,
                "presentation finish must not pollute averageScore with zeros");
        assertEquals(0.40, pres.getAverageAccuracy(), 0.0001,
                "accuracy still tracked — scored elements inside a presentation contribute");
        assertEquals(15L * 60_000L, pres.getAverageDurationMs());

        assertEquals(0, game.getSessionCount(), "no GAME finishes → empty rollup");
    }

    @Test
    void recordSessionFinish_MixedFormats_BothRollupsPopulatedIndependently() {
        // First finish: GAME with one 100-point player.
        InteractiveSession game = baseSession();
        game.getContent().setFormat(SessionFormat.GAME);
        game.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        game.setEndedAt(Instant.parse("2026-05-20T08:10:00Z"));
        game.getPlayers().add(playerWithScore("p-1", 100, 1.0));
        service.recordSessionFinish(game);
        DeckAnalytics afterGame = captureSaved();

        // Second finish: PRESENTATION with two participants, unscored.
        when(analyticsRepository.findById("deck-1")).thenReturn(Optional.of(afterGame));
        InteractiveSession pres = baseSession();
        pres.setId("session-2");
        pres.getContent().setFormat(SessionFormat.PRESENTATION);
        pres.setStartedAt(Instant.parse("2026-05-21T10:00:00Z"));
        pres.setEndedAt(Instant.parse("2026-05-21T10:20:00Z"));
        pres.getPlayers().add(playerWithScore("p-2", 0, 0.70));
        pres.getPlayers().add(playerWithScore("p-3", 0, 0.50));
        service.recordSessionFinish(pres);

        DeckAnalytics finalRollup = lastSaved();
        FormatRollup gameRollup = finalRollup.getGameRollup();
        FormatRollup presRollup = finalRollup.getPresentationRollup();

        assertEquals(1, gameRollup.getSessionCount(), "GAME stays at 1");
        assertEquals(100.0, gameRollup.getAverageScore(), 0.0001,
                "PRESENTATION finish must not touch GAME rollup");

        assertEquals(1, presRollup.getSessionCount());
        assertEquals(2, presRollup.getParticipantCount());
        assertEquals(0.0, presRollup.getAverageScore(), 0.0001);
        assertEquals(0.60, presRollup.getAverageAccuracy(), 0.0001);

        // Deck-wide totals stay populated for back-compat (sum across formats).
        assertEquals(2, finalRollup.getTotalPlays(), "totalPlays = games + presentations");
        assertEquals(3, finalRollup.getTotalPlayers(), "1 game player + 2 presentation participants");
    }

    @Test
    void recordSessionFinish_LegacyRollupWithoutByFormat_LazilyInitialisesRollups() {
        // Simulate a pre-PR3 document: gameRollup + presentationRollup are null
        // because the schema didn't have the fields yet.
        DeckAnalytics legacy = new DeckAnalytics();
        legacy.setDeckId("deck-1");
        legacy.setTotalPlays(5);
        legacy.setGameRollup(null);
        legacy.setPresentationRollup(null);
        when(analyticsRepository.findById("deck-1")).thenReturn(Optional.of(legacy));

        InteractiveSession session = baseSession();
        session.getContent().setFormat(SessionFormat.GAME);
        session.getPlayers().add(playerWithScore("p-1", 100, 1.0));

        service.recordSessionFinish(session);

        DeckAnalytics saved = captureSaved();
        assertNotNull(saved.getGameRollup(),
                "lazy initialisation should backfill the missing rollup");
        assertNotNull(saved.getPresentationRollup(),
                "the opposite rollup should also be initialised (empty) to keep schema consistent");
        assertEquals(1, saved.getGameRollup().getSessionCount());
    }

    // ── Robustness ─────────────────────────────────────────────────────

    @Test
    void recordSessionFinish_NullDeckId_DoesNothing() {
        InteractiveSession session = new InteractiveSession();
        session.setId("session-x");
        // deckId is null
        service.recordSessionFinish(session);
        verify(analyticsRepository, org.mockito.Mockito.never()).save(any(DeckAnalytics.class));
    }

    @Test
    void recordSessionFinish_RepositoryThrows_ExceptionIsSwallowed() {
        when(analyticsRepository.findById("deck-1"))
                .thenThrow(new RuntimeException("mongo down"));

        InteractiveSession session = baseSession();
        session.getContent().getElements().add(mcqElement("mcq-1"));
        // Must not throw — the analytics call is failure-isolated from endGame.
        service.recordSessionFinish(session);
    }

    @Test
    void findByDeckId_ReturnsNullWhenAbsent() {
        when(analyticsRepository.findById("nope")).thenReturn(Optional.empty());
        assertNull(service.findByDeckId("nope"));
    }

    // ── Fixtures ────────────────────────────────────────────────────────

    private InteractiveSession baseSession() {
        InteractiveSession s = new InteractiveSession();
        s.setId("session-1");
        s.setDeckId("deck-1");
        s.setPlayers(new ArrayList<>());
        s.getContent().setElements(new ArrayList<>());
        s.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        s.setEndedAt(Instant.parse("2026-05-20T08:10:00Z"));
        return s;
    }

    private InteractiveSessionPlayer playerWithScore(String userId, int score, double accuracy) {
        InteractiveSessionPlayer p = new InteractiveSessionPlayer();
        p.setUser(UserSnapshot.of(userId, null));
        p.setScore(score);
        p.setEndStats(p.getEndStats().withAccuracy(accuracy));
        return p;
    }

    private void addAnswer(InteractiveSession session, String userId, String elementId,
            AnswerPayload payload, boolean correct, long timeMs) {
        InteractiveSessionPlayer p = session.getPlayers().stream()
                .filter(x -> userId.equals(x.getUserId()))
                .findFirst()
                .orElseGet(() -> {
                    InteractiveSessionPlayer newP = playerWithScore(userId, 0, 0.0);
                    session.getPlayers().add(newP);
                    return newP;
                });
        p.getAnswers().add(answer(elementId, payload, correct, timeMs));
    }

    private PlayerAnswer answer(String elementId, AnswerPayload payload, boolean correct, long timeMs) {
        PlayerAnswer a = new PlayerAnswer();
        a.setElementId(elementId);
        a.setPayload(payload);
        a.setCorrect(correct);
        a.setTimeTakenMs(timeMs);
        return a;
    }

    private DeckElement mcqElement(String id) {
        return new cephadex.brainflex.model.element.McqQuestion(
                id, null, List.of(), Set.of(),
                10, null, null,
                false, false, 0,
                TestElementChromes.scored(id, "title"));
    }

    private DeckElement slide(String id) {
        return new Slide(
                id, null, null, null,
                null, false, 0, false, null, false, false, null, null, null,
                null,
                TestElementChromes.slideChrome(id, "Slide title"));
    }

    private DeckElement qAndA(String id) {
        return new QAndAQuestion(
                id, null, 0, false, false,
                0, null, null,
                false, 0,
                TestElementChromes.survey(id, "Audience Q&A"));
    }

    private DeckAnalytics captureSaved() {
        ArgumentCaptor<DeckAnalytics> captor = ArgumentCaptor.forClass(DeckAnalytics.class);
        verify(analyticsRepository).save(captor.capture());
        return captor.getValue();
    }

    private DeckAnalytics lastSaved() {
        ArgumentCaptor<DeckAnalytics> captor = ArgumentCaptor.forClass(DeckAnalytics.class);
        verify(analyticsRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<DeckAnalytics> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }
}
