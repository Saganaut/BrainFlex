/**
 * A single question belonging to a ContentPack.
 * Stored in its own collection so questions can be queried, shuffled, and
 * drawn into a GameSession independently of the pack document.
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
    private String contentPackId; // reference to ContentPack

    private QuestionType type = QuestionType.MULTIPLE_CHOICE;
    private String questionText;
    private String imageUrl; // null for text-only questions

    private List<String> options; // 2–4 answer choices
    private int correctAnswer;    // index into options (0-based)

    private int pointValue = 100;
    private int timeLimit = 15; // seconds

    private Difficulty difficulty = Difficulty.MEDIUM;
}
