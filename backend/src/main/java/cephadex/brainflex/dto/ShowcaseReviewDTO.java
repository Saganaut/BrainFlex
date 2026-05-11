/**
 * Full post-showcase review payload: every round's question, aggregate distribution,
 * and per-player breakdown. Built from the persisted Showcase + Question documents
 * after the showcase reaches FINISHED.
 *
 * MCQ rounds populate `mcqDistribution` (option index → count). TEXT_INPUT rounds
 * populate `textSubmissions` (frequency-sorted list of normalized submissions).
 */
package cephadex.brainflex.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import cephadex.brainflex.model.PlayerPlacement;
import cephadex.brainflex.model.enums.QuestionType;

public record ShowcaseReviewDTO(
        String showcaseId,
        String roomCode,
        LocalDateTime endedAt,
        boolean scoringEnabled,
        List<PlayerPlacement> placements,
        List<RoundReview> rounds) {

    /** One round's worth of review data. */
    public record RoundReview(
            int round,
            String questionId,
            QuestionType questionType,
            String questionText,
            String imageUrl,
            // MCQ: 0-based index of the right option (-1 for TEXT_INPUT).
            int correctOptionIndex,
            // MCQ: the right option's label. TEXT_INPUT: the canonical correct text.
            String correctAnswerText,
            // MCQ only: full list of options so the bar chart can label each bar.
            List<String> options,
            // MCQ only: option index → number of players who picked it.
            Map<Integer, Integer> mcqDistribution,
            // TEXT_INPUT only: each unique submission with its count, sorted desc.
            List<TextSubmission> textSubmissions,
            // Number of players who didn't submit at all in time.
            int timedOutCount,
            List<PlayerRoundDetail> playerAnswers) {
    }

    /** A unique text-input submission and how many players gave that exact (normalized) answer. */
    public record TextSubmission(
            String text,
            int count,
            boolean isCorrect) {
    }

    /** Per-player outcome for a single round, surfaced in the review's detail rows. */
    public record PlayerRoundDetail(
            String userId,
            String userName,
            int selectedOption,     // MCQ: -1 means timeout / N/A
            String textAnswer,      // TEXT_INPUT only
            boolean wasCorrect,
            int pointsAwarded) {
    }
}
