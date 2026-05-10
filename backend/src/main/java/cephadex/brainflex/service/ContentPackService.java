/**
 * Business logic for browsing, creating, and editing content packs and their questions.
 * All mutation methods enforce ownership — callers that don't own the pack get 403.
 */
package cephadex.brainflex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cephadex.brainflex.dto.CreateContentPackRequest;
import cephadex.brainflex.dto.UpdateContentPackRequest;
import cephadex.brainflex.dto.UpsertQuestionRequest;
import cephadex.brainflex.model.ContentPack;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.repository.ContentPackRepository;
import cephadex.brainflex.repository.QuestionRepository;

@Service
public class ContentPackService {

    private final ContentPackRepository contentPackRepository;
    private final QuestionRepository questionRepository;

    public ContentPackService(ContentPackRepository contentPackRepository,
            QuestionRepository questionRepository) {
        this.contentPackRepository = contentPackRepository;
        this.questionRepository = questionRepository;
    }

    public List<ContentPack> listPublic() {
        return contentPackRepository.findByIsPublicTrue();
    }

    public List<ContentPack> listByOwner(String userId) {
        return contentPackRepository.findByCreatorUserId(userId);
    }

    public ContentPack getById(String id) {
        return contentPackRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content pack not found"));
    }

    public ContentPack createPack(User creator, CreateContentPackRequest request) {
        ContentPack pack = new ContentPack();
        pack.setName(request.name());
        pack.setDescription(request.description());
        pack.setCategory(request.category());
        pack.setSystem(false);
        pack.setPublic(true);
        pack.setCreatorUserId(creator.getId());
        return contentPackRepository.save(pack);
    }

    public ContentPack updatePack(String id, User caller, UpdateContentPackRequest request) {
        ContentPack pack = requireOwned(id, caller);
        if (request.name() != null) pack.setName(request.name());
        if (request.description() != null) pack.setDescription(request.description());
        if (request.category() != null) pack.setCategory(request.category());
        return contentPackRepository.save(pack);
    }

    public void deletePack(String id, User caller) {
        ContentPack pack = requireOwned(id, caller);
        questionRepository.deleteByContentPackId(pack.getId());
        contentPackRepository.delete(pack);
    }

    public List<Question> listQuestions(String packId, User caller) {
        requireOwned(packId, caller);
        return questionRepository.findByContentPackId(packId);
    }

    public Question addQuestion(String packId, User caller, UpsertQuestionRequest request) {
        ContentPack pack = requireOwned(packId, caller);
        Question question = buildQuestion(packId, request);
        Question saved = questionRepository.save(question);
        pack.setQuestionCount(pack.getQuestionCount() + 1);
        contentPackRepository.save(pack);
        return saved;
    }

    public Question updateQuestion(String packId, String questionId, User caller,
            UpsertQuestionRequest request) {
        requireOwned(packId, caller);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!packId.equals(question.getContentPackId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        applyQuestion(question, request);
        return questionRepository.save(question);
    }

    public void deleteQuestion(String packId, String questionId, User caller) {
        ContentPack pack = requireOwned(packId, caller);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!packId.equals(question.getContentPackId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        questionRepository.delete(question);
        pack.setQuestionCount(Math.max(0, pack.getQuestionCount() - 1));
        contentPackRepository.save(pack);
    }

    // --- helpers ---

    private ContentPack requireOwned(String packId, User caller) {
        ContentPack pack = getById(packId);
        if (pack.isSystem() || !caller.getId().equals(pack.getCreatorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this content pack");
        }
        return pack;
    }

    private Question buildQuestion(String packId, UpsertQuestionRequest request) {
        Question q = new Question();
        q.setContentPackId(packId);
        applyQuestion(q, request);
        return q;
    }

    private void applyQuestion(Question q, UpsertQuestionRequest request) {
        q.setQuestionText(request.questionText());
        q.setOptions(request.options());
        q.setCorrectAnswer(request.correctAnswer());
        q.setPointValue(request.pointValue());
        q.setTimeLimit(request.timeLimit());
        q.setDifficulty(request.difficulty() != null ? request.difficulty() : Difficulty.MEDIUM);
    }
}
