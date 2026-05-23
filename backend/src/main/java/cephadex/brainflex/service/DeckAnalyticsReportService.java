/**
 * CSV export builder for {@link cephadex.brainflex.model.deck.DeckAnalytics}.
 *
 * Renders the rollup into a two-section CSV — a deck-level KPI row followed by
 * a per-element table — that opens cleanly in Excel/Sheets and round-trips the
 * full distribution map as JSON in a quoted cell. Element titles + kinds are
 * pulled from the live {@link Deck}; elements that have been deleted since
 * being played still appear with their id but a blank title.
 *
 * Why a dedicated service instead of inlining in the controller: keeps the
 * controller thin, lets the same builder feed a future per-show CSV (chunk 16
 * checklist item) or PDF, and isolates the CSV-escape logic for unit tests
 * (controller tests verify status + Content-Type; service tests verify shape).
 */
package cephadex.brainflex.service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cephadex.brainflex.model.session.ElementStats;
import cephadex.brainflex.model.session.FormatRollup;
import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.DeckAnalytics;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.enums.SessionFormat;

@Service
public class DeckAnalyticsReportService {

    // Audit timestamps are Instants; bind the zone-less ISO date-time formatter
    // to UTC so it can render an Instant (which has no Year/local fields of its
    // own) as e.g. "2026-05-19T10:00:00" without a trailing offset.
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final String[] DECK_HEADERS = {
            "Deck ID", "Deck Name", "Total Sessions", "Total Participants",
            "Average Score", "Average Accuracy", "Average Duration (ms)",
            "Last Run At", "Updated At"
    };
    private static final String[] FORMAT_HEADERS = {
            "Format", "Session Count", "Participant Count",
            "Average Score", "Average Accuracy", "Average Duration (ms)",
            "Last Run At"
    };
    private static final String[] ELEMENT_HEADERS = {
            "Element ID", "Kind", "Title",
            "Presented", "Answered", "Correct", "Accuracy",
            "Average Time (ms)", "Reactions", "Chat Messages",
            "Distribution"
    };

    private final ObjectMapper objectMapper;

    public DeckAnalyticsReportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Build a CSV body for the given deck rollup. {@code analytics} may be a
     * fresh empty document (deck never played); in that case the deck row is
     * still emitted with zero values so the dashboard's "Export" button never
     * produces an empty file.
     */
    public String buildCsv(Deck deck, DeckAnalytics analytics) {
        StringBuilder sb = new StringBuilder();

        sb.append("Deck Analytics\n");
        writeRow(sb, DECK_HEADERS);
        writeRow(sb,
                nullSafe(deck.getId()),
                nullSafe(deck.getContent().getName()),
                Integer.toString(analytics.getTotalPlays()),
                Integer.toString(analytics.getTotalPlayers()),
                formatDouble(analytics.getAverageScore()),
                formatDouble(analytics.getAverageAccuracy()),
                Long.toString(analytics.getAverageDurationMs()),
                analytics.getLastPlayedAt() == null ? "" : ISO.format(analytics.getLastPlayedAt()),
                analytics.getUpdatedAt() == null ? "" : ISO.format(analytics.getUpdatedAt()));

        // Per-format breakdown — one row per SessionFormat. Always present, so
        // downstream consumers (Sheets formulas, analysts) can rely on a stable
        // shape even when one format has zero sessions; legacy docs predating
        // PR3 render as zero rows.
        sb.append('\n');
        sb.append("By Format\n");
        writeRow(sb, FORMAT_HEADERS);
        writeFormatRow(sb, SessionFormat.GAME, analytics.getGameRollup());
        writeFormatRow(sb, SessionFormat.PRESENTATION, analytics.getPresentationRollup());

        sb.append('\n');
        sb.append("Per-Element Stats\n");
        writeRow(sb, ELEMENT_HEADERS);

        Map<String, DeckElement> liveById = indexElementsById(deck);
        Map<String, ElementStats> perElement = analytics.getPerElement() == null
                ? Map.of()
                : analytics.getPerElement();
        for (Map.Entry<String, ElementStats> entry : perElement.entrySet()) {
            String elementId = entry.getKey();
            ElementStats stats = entry.getValue();
            DeckElement live = liveById.get(elementId);
            writeRow(sb,
                    nullSafe(elementId),
                    live == null ? "" : live.kind().name(),
                    live == null ? "" : nullSafe(live.title()),
                    Integer.toString(stats.getPresentedCount()),
                    Integer.toString(stats.getAnsweredCount()),
                    Integer.toString(stats.getCorrectCount()),
                    formatAccuracy(stats),
                    formatDouble(stats.getAverageTimeMs()),
                    Integer.toString(stats.getReactionsReceived()),
                    Integer.toString(stats.getChatMessagesDuringRound()),
                    serializeDistribution(stats));
        }

        return sb.toString();
    }

    /**
     * Suggested filename for the {@code Content-Disposition} header. Slugified
     * deck name keeps it human-readable; the {@code -analytics.csv} suffix
     * matches what shipping tools (Sheets / Excel) expect to round-trip.
     */
    public String suggestedFilename(Deck deck) {
        String base = deck.getContent().getName() == null ? "deck" : deck.getContent().getName();
        String slug = base.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty())
            slug = "deck";
        return slug + "-analytics.csv";
    }

    private void writeFormatRow(StringBuilder sb, SessionFormat format, FormatRollup rollup) {
        if (rollup == null) {
            // Legacy doc without a rollup yet — emit a zero row so the CSV
            // shape stays the same as a freshly-recorded deck.
            writeRow(sb,
                    format.name(),
                    "0", "0",
                    formatDouble(0.0), formatDouble(0.0),
                    "0",
                    "");
            return;
        }
        writeRow(sb,
                format.name(),
                Integer.toString(rollup.getSessionCount()),
                Integer.toString(rollup.getParticipantCount()),
                formatDouble(rollup.getAverageScore()),
                formatDouble(rollup.getAverageAccuracy()),
                Long.toString(rollup.getAverageDurationMs()),
                rollup.getLastRunAt() == null ? "" : ISO.format(rollup.getLastRunAt()));
    }

    private static Map<String, DeckElement> indexElementsById(Deck deck) {
        if (deck.getContent().getElements() == null)
            return Map.of();
        Map<String, DeckElement> out = new HashMap<>(deck.getContent().getElements().size());
        for (DeckElement e : deck.getContent().getElements()) {
            if (e == null || e.id() == null)
                continue;
            out.put(e.id(), e);
        }
        return out;
    }

    private String serializeDistribution(ElementStats stats) {
        Map<String, Integer> dist = stats.getDistribution();
        if (dist == null || dist.isEmpty())
            return "";
        try {
            return objectMapper.writeValueAsString(dist);
        } catch (JsonProcessingException e) {
            // Distribution maps are simple Map<String,Integer> — Jackson cannot
            // fail this in practice, but if it ever does we'd rather emit a
            // visibly-empty cell than fail the whole export.
            return "";
        }
    }

    private static String formatAccuracy(ElementStats stats) {
        if (stats.getAnsweredCount() <= 0)
            return "";
        return String.format("%.4f", (double) stats.getCorrectCount() / stats.getAnsweredCount());
    }

    private static String formatDouble(double value) {
        return String.format("%.4f", value);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static void writeRow(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append('\n');
    }

    /**
     * RFC-4180 escape: quote when the cell contains a comma, double-quote, CR,
     * or LF; escape inner double-quotes by doubling them.
     */
    private static String escape(String cell) {
        if (cell == null || cell.isEmpty())
            return "";
        boolean mustQuote = false;
        for (int i = 0; i < cell.length(); i++) {
            char c = cell.charAt(i);
            if (c == ',' || c == '"' || c == '\n' || c == '\r') {
                mustQuote = true;
                break;
            }
        }
        if (!mustQuote)
            return cell;
        StringBuilder quoted = new StringBuilder(cell.length() + 4);
        quoted.append('"');
        for (int i = 0; i < cell.length(); i++) {
            char c = cell.charAt(i);
            if (c == '"')
                quoted.append('"');
            quoted.append(c);
        }
        quoted.append('"');
        return quoted.toString();
    }

}
