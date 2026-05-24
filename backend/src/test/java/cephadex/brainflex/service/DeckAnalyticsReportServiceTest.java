/**
 * Unit tests for {@link DeckAnalyticsReportService}.
 *
 * The per-element distribution math is exercised by
 * {@link DeckAnalyticsServiceTest}; these tests pin down CSV-specific
 * concerns:
 *   - three-section layout (deck row + by-format rows + per-element rows) is stable
 *   - never-played decks still produce a valid header + zero-default rows
 *   - element titles/kinds are joined from the live deck, with a blank
 *     title preserved when an element has been deleted since play
 *   - RFC-4180 escaping (commas, quotes, newlines) works
 *   - the distribution map survives the round-trip as JSON in a quoted cell
 *   - the by-format section emits both rows even when one rollup is null
 *     (legacy doc) or empty
 *   - the suggested filename slugifies the deck name
 */
package cephadex.brainflex.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckAnalytics;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.session.ElementStats;
import cephadex.brainflex.model.session.FormatRollup;

class DeckAnalyticsReportServiceTest {

        private final DeckAnalyticsReportService service = new DeckAnalyticsReportService(new ObjectMapper());

        @Test
        void buildCsv_NeverPlayed_StillEmitsAllSectionHeaders() {
                Deck deck = deck("deck-1", "Geography 101");
                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");

                // Section 1: Deck Analytics
                assertEquals("Deck Analytics", lines[0]);
                assertTrue(lines[1].startsWith("Deck ID,Deck Name,Total Sessions,Total Participants,"),
                                "deck headers should use session/participant language; got: " + lines[1]);
                assertTrue(lines[2].startsWith("deck-1,Geography 101,0,0,"),
                                "zero-default deck row should still be emitted; got: " + lines[2]);

                // Section 2: By Format — two rows, one per SessionFormat, even when null.
                assertEquals("", lines[3]);
                assertEquals("By Format", lines[4]);
                assertTrue(lines[5].startsWith("Format,Session Count,Participant Count,"));
                assertTrue(lines[6].startsWith("GAME,0,0,"), "GAME row, got: " + lines[6]);
                assertTrue(lines[7].startsWith("PRESENTATION,0,0,"), "PRESENTATION row, got: " + lines[7]);

                // Section 3: Per-Element Stats — header only when no plays yet.
                assertEquals("", lines[8]);
                assertEquals("Per-Element Stats", lines[9]);
                assertTrue(lines[10].startsWith("Element ID,Kind,Title,"));
                assertEquals(11, lines.length,
                                "no per-element rows should follow when the rollup is empty");
        }

        @Test
        void buildCsv_OneMcqWithDistribution_RoundTripsAsJsonInQuotedCell() {
                Deck deck = deck("deck-1", "Geography 101");
                deck.getContent().setElements(List.of(mcq("mcq-1", "What is the capital of France?")));

                ElementStats stats = new ElementStats();
                stats.setPresentedCount(3);
                stats.setAnsweredCount(10);
                stats.setCorrectCount(7);
                stats.setTotalTimeMs(15_000L);
                stats.setAverageTimeMs(1_500.0);
                stats.setReactionsReceived(4);
                stats.setChatMessagesDuringRound(2);
                stats.setDistribution(new java.util.LinkedHashMap<>(
                                Map.of("opt-paris", 7, "opt-london", 3)));

                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");
                analytics.setTotalPlays(3);
                analytics.setTotalPlayers(10);
                analytics.setAverageScore(78.5);
                analytics.setAverageAccuracy(0.70);
                analytics.setAverageDurationMs(420_000L);
                analytics.setLastPlayedAt(Instant.parse("2026-05-19T10:00:00Z"));
                analytics.getPerElement().put("mcq-1", stats);

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");

                // Deck row carries the rollup's KPIs.
                assertTrue(lines[2].startsWith(
                                "deck-1,Geography 101,3,10,78.5000,0.7000,420000,2026-05-19T10:00:00,2026-05-19T10:05:00"),
                                "expected populated deck row, got: " + lines[2]);

                // Per-element row sits below the by-format section (lines 4–7) and an
                // empty separator + the Per-Element Stats header.
                String elementRow = lines[11];
                assertTrue(elementRow
                                .startsWith("mcq-1,MCQ,What is the capital of France?,3,10,7,0.7000,1500.0000,4,2,"),
                                "expected per-element row prefix, got: " + elementRow);

                // Distribution cell: parse out the trailing quoted JSON and confirm it
                // round-trips. The exact ordering is preserved by the LinkedHashMap above.
                int firstQuote = elementRow.indexOf('"');
                int lastQuote = elementRow.lastIndexOf('"');
                assertTrue(firstQuote >= 0 && lastQuote > firstQuote,
                                "expected a quoted JSON cell at the end: " + elementRow);
                String quoted = elementRow.substring(firstQuote + 1, lastQuote).replace("\"\"", "\"");
                assertTrue(quoted.contains("\"opt-paris\":7"));
                assertTrue(quoted.contains("\"opt-london\":3"));
        }

        @Test
        void buildCsv_DeletedElement_StillEmitsRowWithBlankTitle() {
                Deck deck = deck("deck-1", "Anything");
                // No elements on the live deck — simulate one having been deleted.
                deck.getContent().setElements(List.of());

                ElementStats stats = new ElementStats();
                stats.setPresentedCount(1);
                stats.setAnsweredCount(2);
                stats.setCorrectCount(0);

                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");
                analytics.getPerElement().put("ghost-1", stats);

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");
                // Per-element row index shifted to 11 by the new by-format section.
                String row = lines[11];
                // id present, kind+title blank, counts emitted, accuracy=0.0000
                assertTrue(row.startsWith("ghost-1,,,1,2,0,0.0000,"),
                                "expected deleted-element row with blank kind/title, got: " + row);
        }

        @Test
        void buildCsv_EscapesCommasAndQuotesAndNewlines() {
                Deck deck = deck("deck-1", "Big, \"Quoted\" Deck");
                DeckElement evil = mcq("e-1", "Multi-line\nTitle, with \"quotes\"");
                deck.getContent().setElements(List.of(evil));

                ElementStats stats = new ElementStats();
                stats.setPresentedCount(1);
                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");
                analytics.getPerElement().put("e-1", stats);

                String csv = service.buildCsv(deck, analytics);

                // Deck-name cell should be quoted with doubled inner quotes.
                assertTrue(csv.contains("\"Big, \"\"Quoted\"\" Deck\""),
                                "expected RFC-4180 escaped deck name; CSV was:\n" + csv);
                // Element title contains a comma + quotes + newline → quoted, doubled.
                assertTrue(csv.contains("\"Multi-line\nTitle, with \"\"quotes\"\"\""),
                                "expected RFC-4180 escaped element title; CSV was:\n" + csv);
        }

        @Test
        void buildCsv_AnsweredZero_LeavesAccuracyBlank() {
                Deck deck = deck("d-1", "D");
                deck.getContent().setElements(List.of(mcq("e-1", "Title")));
                ElementStats stats = new ElementStats();
                stats.setPresentedCount(1);
                stats.setAnsweredCount(0);
                stats.setCorrectCount(0);
                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("d-1");
                analytics.getPerElement().put("e-1", stats);

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");
                // accuracy column is the 7th comma-separated cell after id/kind/title/3 counts;
                // per-element row index is 11 with the by-format section in between.
                String row = lines[11];
                // ...,1,0,0,,0.0000,... — accuracy cell is blank.
                assertTrue(row.contains(",1,0,0,,0.0000,"),
                                "expected blank accuracy cell when answeredCount=0, got: " + row);
                assertFalse(row.contains(",1,0,0,0.0000,0.0000,"),
                                "should NOT emit 0.0000 accuracy for zero-answered rows");
        }

        @Test
        void buildCsv_ByFormatSection_EmitsPopulatedGameAndPresentationRows() {
                Deck deck = deck("deck-1", "Mixed Deck");

                FormatRollup game = new FormatRollup();
                game.setSessionCount(4);
                game.setParticipantCount(12);
                game.setAverageScore(85.5);
                game.setAverageAccuracy(0.75);
                game.setAverageDurationMs(540_000L);
                game.setLastRunAt(Instant.parse("2026-05-19T14:00:00Z"));

                FormatRollup pres = new FormatRollup();
                pres.setSessionCount(2);
                pres.setParticipantCount(7);
                pres.setAverageScore(0.0); // PRESENTATION leaves this at 0 by design
                pres.setAverageAccuracy(0.60);
                pres.setAverageDurationMs(300_000L);
                pres.setLastRunAt(Instant.parse("2026-05-20T09:30:00Z"));

                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");
                analytics.setGameRollup(game);
                analytics.setPresentationRollup(pres);

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");

                assertEquals("By Format", lines[4]);
                assertEquals("GAME,4,12,85.5000,0.7500,540000,2026-05-19T14:00:00", lines[6]);
                assertEquals("PRESENTATION,2,7,0.0000,0.6000,300000,2026-05-20T09:30:00", lines[7]);
        }

        @Test
        void buildCsv_ByFormatSection_NullRollupRendersAsZeroRow() {
                // Legacy doc predating PR3 — gameRollup + presentationRollup are null
                // on disk. The CSV shape must stay stable so downstream tooling
                // doesn't choke on a missing row.
                Deck deck = deck("deck-1", "Legacy Deck");
                DeckAnalytics analytics = new DeckAnalytics();
                analytics.setDeckId("deck-1");
                analytics.setGameRollup(null);
                analytics.setPresentationRollup(null);

                String csv = service.buildCsv(deck, analytics);
                String[] lines = csv.split("\n");

                assertEquals("GAME,0,0,0.0000,0.0000,0,", lines[6]);
                assertEquals("PRESENTATION,0,0,0.0000,0.0000,0,", lines[7]);
        }

        @Test
        void suggestedFilename_SlugifiesAndAppendsSuffix() {
                assertEquals("my-deck-analytics.csv",
                                service.suggestedFilename(deck("d-1", "My Deck")));
                assertEquals("geography-101-analytics.csv",
                                service.suggestedFilename(deck("d-1", "Geography 101!!!")));
                assertEquals("deck-analytics.csv",
                                service.suggestedFilename(deck("d-1", "    ")));
                Deck nullName = new Deck();
                nullName.setId("d-1");
                assertEquals("deck-analytics.csv", service.suggestedFilename(nullName));
        }

        // ── Builders ─────────────────────────────────────────────────────────

        private static Deck deck(String id, String name) {
                Deck d = new Deck();
                d.setId(id);
                d.getContent().setName(name);
                return d;
        }

        private static DeckElement mcq(String id, String title) {
                return new McqQuestion(
                                id, null, List.of(), Set.of(),
                                10, null, null,
                                false, false, 0,
                                TestElementChromes.scored(id, title));
        }
}
