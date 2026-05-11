/**
 * A single question belonging to a Deck.
 * Stored in its own collection so questions can be queried, shuffled, and
 * drawn into a Showcase independently of the pack document.
 */
package cephadex.brainflex.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.QuestionType;
import lombok.Data;

@Data
@Document(collection = "questions")
public class Question {
    @Id
    private String id;

    @Indexed
    private String deckId; // reference to Deck

    private QuestionType type = QuestionType.MULTIPLE_CHOICE;
    private String questionText;
    private String imageUrl; // null for text-only questions

    // MULTIPLE_CHOICE only: 2–4 answer choices and the 0-based index of the right one.
    private List<String> options;
    private int correctAnswer;

    // TEXT_INPUT only: free-text correct answer. Matched case-insensitively, trimmed.
    private String correctAnswerText;

    private int pointValue = 100;
    private int timeLimit = 15; // seconds

    private Difficulty difficulty = Difficulty.MEDIUM;
}
