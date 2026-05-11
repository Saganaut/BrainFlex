/**
 * Frozen per-round snapshot of a question's option order after shuffling.
 * Stored on the Showcase so every consumer (broadcast, scoring, review) reads
 * the same ordering — clients can't desync across the question's lifecycle.
 *
 * Keyed by Question id in Showcase.mcqShuffles.
 */
package cephadex.brainflex.model;

import java.util.List;

import lombok.Data;

@Data
public class McqShuffle {
    private String questionId;
    // Options in the order players see them.
    private List<String> shuffledOptions;
    // Position of the correct answer within shuffledOptions.
    private int shuffledCorrectIndex;
}
