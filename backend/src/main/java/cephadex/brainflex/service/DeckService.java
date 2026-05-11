/**
 * Business logic for browsing, creating, and editing content packs and their questions.
 * All mutation methods enforce ownership — callers that don't own the pack get 403.
 */
package cephadex.brainflex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateDeckRequest;
import cephadex.brainflex.dto.UpdateDeckRequest;
import cephadex.brainflex.dto.UpsertQuestionRequest;
import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.ElementKind;
import cephadex.brainflex.model.enums.QuestionType;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.QuestionRepository;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final QuestionRepository questionRepository;

    public DeckService(DeckRepository deckRepository,
            QuestionRepository questionRepository) {
        this.deckRepository = deckRepository;
        this.questionRepository = questionRepository;
    }

    public List<Deck> listPublic() {
        return deckRepository.findByIsPublicTrue();
    }

    public List<Deck> listByOwner(String userId) {
        return deckRepository.findByCreatorUserId(userId);
    }

    public Deck getById(String id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content pack not found"));
    }

    public Deck createDeck(User creator, CreateDeckRequest request) {
        Deck pack = new Deck();
        pack.setName(request.name());
        pack.setDescription(request.description());
        pack.setCategory(request.category());
        pack.setCoverImageUrl(request.coverImageUrl());
        pack.setBackgroundImageUrl(request.backgroundImageUrl());
        pack.setSystem(false);
        pack.setPublic(true);
        pack.setCreatorUserId(creator.getId());
        return deckRepository.save(pack);
    }

    public Deck updateDeck(String id, User caller, UpdateDeckRequest request) {
        Deck pack = requireOwned(id, caller);
        if (request.name() != null) pack.setName(request.name());
        if (request.description() != null) pack.setDescription(request.description());
        if (request.category() != null) pack.setCategory(request.category());
        // Empty string clears the image; null leaves it alone.
        if (request.coverImageUrl() != null) {
            pack.setCoverImageUrl(request.coverImageUrl().isEmpty() ? null : request.coverImageUrl());
        }
        if (request.backgroundImageUrl() != null) {
            pack.setBackgroundImageUrl(request.backgroundImageUrl().isEmpty() ? null : request.backgroundImageUrl());
        }
        return deckRepository.save(pack);
    }

    public void deleteDeck(String id, User caller) {
        Deck pack = requireOwned(id, caller);
        questionRepository.deleteByDeckId(pack.getId());
        deckRepository.delete(pack);
    }

    public List<Question> listQuestions(String packId, User caller) {
        requireOwned(packId, caller);
        return questionRepository.findByDeckIdOrderByPositionAscIdAsc(packId);
    }

    public Question addQuestion(String packId, User caller, UpsertQuestionRequest request) {
        Deck pack = requireOwned(packId, caller);
        Question question = buildQuestion(packId, request);
        // Auto-assign position at the end of the deck if the caller didn't supply one.
        if (question.getPosition() == null) {
            question.setPosition(nextPositionForDeck(packId));
        }
        Question saved = questionRepository.save(question);
        pack.setQuestionCount(pack.getQuestionCount() + 1);
        deckRepository.save(pack);
        return saved;
    }

    /** Returns the next position slot for a deck — last position + 10 (or 10 for empty decks). */
    private double nextPositionForDeck(String deckId) {
        List<Question> existing = questionRepository.findByDeckIdOrderByPositionAscIdAsc(deckId);
        if (existing.isEmpty()) return 10.0;
        Question last = existing.get(existing.size() - 1);
        return (last.getPosition() == null ? existing.size() * 10.0 : last.getPosition()) + 10.0;
    }

    public Question updateQuestion(String packId, String questionId, User caller,
            UpsertQuestionRequest request) {
        requireOwned(packId, caller);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!packId.equals(question.getDeckId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        applyQuestion(question, request);
        return questionRepository.save(question);
    }

    public void deleteQuestion(String packId, String questionId, User caller) {
        Deck pack = requireOwned(packId, caller);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!packId.equals(question.getDeckId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.delete(question);
        pack.setQuestionCount(Math.max(0, pack.getQuestionCount() - 1));
        deckRepository.save(pack);
    }

    // --- helpers ---

    private Deck requireOwned(String packId, User caller) {
        Deck pack = getById(packId);
        if (pack.isSystem() || !caller.getId().equals(pack.getCreatorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this content pack");
        }
        return pack;
    }

    private Question buildQuestion(String packId, UpsertQuestionRequest request) {
        Question q = new Question();
        q.setDeckId(packId);
        applyQuestion(q, request);
        return q;
    }

    private void applyQuestion(Question q, UpsertQuestionRequest request) {
        ElementKind kind = request.resolvedKind();
        QuestionType type = request.resolvedType();
        q.setKind(kind);
        q.setType(type);
        q.setTitle(request.title());
        q.setQuestionText(request.questionText());
        q.setPointValue(request.pointValue());
        q.setTimeLimit(request.timeLimit());
        q.setDifficulty(request.difficulty() != null ? request.difficulty() : Difficulty.MEDIUM);
        // Caller-provided position overrides; otherwise leave whatever's on the doc
        // (preserves position on update; addQuestion auto-fills for inserts).
        if (request.position() != null) q.setPosition(request.position());

        if (kind == ElementKind.SLIDE) {
            // Slides carry only display fields. Wipe any leftover answer state from a
            // prior question state if the editor toggled an element between kinds.
            q.setOptions(null);
            q.setCorrectAnswer(0);
            q.setCorrectAnswerText(null);
        } else if (type == QuestionType.TEXT_INPUT) {
            q.setCorrectAnswerText(request.correctAnswerText().trim());
            q.setOptions(null);
            q.setCorrectAnswer(0);
        } else {
            q.setOptions(request.options());
            q.setCorrectAnswer(request.correctAnswer() == null ? 0 : request.correctAnswer());
            q.setCorrectAnswerText(null);
        }
    }
}
