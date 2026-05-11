/**
 * A single element belonging to a Deck — either a Question (interactive) or a Slide
 * (non-interactive content), discriminated by `kind`. Stored in a single "questions"
 * collection so the draw / shuffle / broadcast pipeline stays uniform across kinds.
 *
 * For SLIDE elements: `questionText` carries the body content; `title` is optional;
 * `imageUrl` is the slide image; `options` / `correctAnswer` / `correctAnswerText`
 * / `pointValue` are unused. `timeLimit` is the slide's display duration in seconds.
 */
package cephadex.brainflex.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.QuestionType;
import lombok.Data;

@Data
@Document(collection = "questions")
public class Question {
    @Id
    private String id;

    @Indexed
    private String deckId; // reference to Deck

    // Explicit ordering within a deck. Float so the editor can insert between two
    // existing positions without renumbering — pick the midpoint. Null on legacy docs
    // is backfilled at startup; new inserts auto-assign max+10.
    private Double position;

    // Discriminator across element kinds. Defaults to QUESTION so existing documents
    // without the field continue to deserialize as questions.
    private ElementKind kind = ElementKind.QUESTION;

    private QuestionType type = QuestionType.MULTIPLE_CHOICE;
    private String title;       // SLIDE: optional headline. Ignored for questions.
    private String questionText;
    private String imageUrl; // null for text-only questions

    // MULTIPLE_CHOICE only: 2–4 answer choices and the 0-based index of the right one.
    private List<String> options;
    private int correctAnswer;

    // TEXT_INPUT only: free-text correct answer. Matched case-insensitively, trimmed.
    private String correctAnswerText;

    private int pointValue = 100;
    private int timeLimit = 15; // seconds (also used as slide display time)

    private Difficulty difficulty = Difficulty.MEDIUM;
}
