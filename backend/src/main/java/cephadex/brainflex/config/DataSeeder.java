/**
 * Loads seed data from src/main/resources/seed/*.json on startup.
 *
 * Seeds are idempotent — the loader inserts only documents whose id is missing
 * from the target collection, so re-running on an existing DB (e.g. to pick up
 * new system content like slides) doesn't disturb user-created data.
 *
 * Users seed is treated specially: the existing file is keyed by Google ID and
 * registration data, so we still skip when the users collection is non-empty.
 */
package cephadex.brainflex.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.Question;
import cephadex.brainflex.model.User;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.QuestionRepository;
import cephadex.brainflex.repository.UserRepository;

@Configuration
public class DataSeeder {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Bean
    @SuppressWarnings("unused")
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            DeckRepository deckRepository,
            QuestionRepository questionRepository) {
        return args -> {
            seedIfCollectionEmpty(userRepository, "seed/users.json", User.class);
            // Decks are upsert-by-id (each seed deck has a pinned _id), so new system
            // decks land without disturbing user-authored ones.
            upsertById(deckRepository, "seed/decks.json", Deck.class, Deck::getId);
            // Questions don't have stable ids in the seed file, so we re-seed per-deck:
            // for every unique deckId referenced in the seed, if that deck currently
            // has zero questions, insert all of its seed questions in order. This is
            // idempotent across normal restarts and lets us re-seed a single system
            // deck by dropping its questions before restart.
            seedQuestionsPerDeck(questionRepository);
            // One-time backfill: assign deterministic positions to any pre-existing
            // documents that predate the position field. Subsequent runs are cheap
            // since the filter returns no candidates.
            backfillQuestionPositions(questionRepository);
        };
    }

    /**
     * Assigns position values to legacy questions where it's null. Uses the existing
     * _id-based insertion order so the result matches what those decks have been
     * playing as. Idempotent — re-running finds zero candidates.
     */
    private void backfillQuestionPositions(QuestionRepository questionRepository) {
        List<Question> needsBackfill = questionRepository.findAll().stream()
                .filter(q -> q.getPosition() == null)
                .toList();
        if (needsBackfill.isEmpty()) return;

        // Bucket by deckId, then walk each bucket in _id order assigning 10, 20, 30...
        Map<String, List<Question>> byDeck = new HashMap<>();
        for (Question q : needsBackfill) {
            byDeck.computeIfAbsent(q.getDeckId(), k -> new ArrayList<>()).add(q);
        }
        int touched = 0;
        for (Map.Entry<String, List<Question>> entry : byDeck.entrySet()) {
            // Existing questions in this deck that already have a position — start above their max.
            List<Question> existing = questionRepository.findByDeckIdOrderByPositionAscIdAsc(entry.getKey());
            double next = existing.stream()
                    .filter(q -> q.getPosition() != null)
                    .mapToDouble(Question::getPosition)
                    .max()
                    .orElse(0.0);

            // Walk to-backfill items in _id ascending so the order is deterministic.
            entry.getValue().sort((a, b) -> a.getId().compareTo(b.getId()));
            for (Question q : entry.getValue()) {
                next += 10.0;
                q.setPosition(next);
                touched++;
            }
            questionRepository.saveAll(entry.getValue());
        }
        System.out.println("Backfilled position on " + touched + " legacy questions across "
                + byDeck.size() + " deck(s)");
    }

    private void seedQuestionsPerDeck(QuestionRepository questionRepository) throws Exception {
        List<Question> all = readSeed("seed/questions.json", Question.class);
        Map<String, List<Question>> byDeck = new HashMap<>();
        for (Question q : all) {
            byDeck.computeIfAbsent(q.getDeckId(), k -> new ArrayList<>()).add(q);
        }
        int totalInserted = 0;
        int decksSeeded = 0;
        for (Map.Entry<String, List<Question>> entry : byDeck.entrySet()) {
            String deckId = entry.getKey();
            if (deckId == null) continue;
            if (questionRepository.countByDeckId(deckId) > 0) continue;
            // Assign positions in JSON-order so the deck plays as authored.
            List<Question> deckQuestions = entry.getValue();
            double pos = 0.0;
            for (Question q : deckQuestions) {
                pos += 10.0;
                if (q.getPosition() == null) q.setPosition(pos);
            }
            questionRepository.saveAll(deckQuestions);
            totalInserted += deckQuestions.size();
            decksSeeded++;
        }
        if (totalInserted > 0) {
            System.out.println("Seeded " + totalInserted + " questions across "
                    + decksSeeded + " deck(s) from seed/questions.json");
        }
    }

    /** Original behavior: load only when the collection is empty. Used for users. */
    private <T> void seedIfCollectionEmpty(
            MongoRepository<T, String> repository, String resourcePath, Class<T> type) throws Exception {
        if (repository.count() > 0) return;
        List<T> items = readSeed(resourcePath, type);
        repository.saveAll(items);
        System.out.println("Seeded " + items.size() + " " + type.getSimpleName() + " records from " + resourcePath);
    }

    /** Inserts only items whose id is not already in the collection. Safe to re-run. */
    private <T> void upsertById(
            MongoRepository<T, String> repository,
            String resourcePath,
            Class<T> type,
            Function<T, String> idGetter) throws Exception {
        List<T> items = readSeed(resourcePath, type);
        if (items.isEmpty()) return;

        Set<String> seedIds = new HashSet<>();
        for (T item : items) {
            String id = idGetter.apply(item);
            if (id != null) seedIds.add(id);
        }
        Set<String> existingIds = new HashSet<>();
        if (!seedIds.isEmpty()) {
            repository.findAllById(seedIds).forEach(e -> {
                String id = idGetter.apply(e);
                if (id != null) existingIds.add(id);
            });
        }

        List<T> toInsert = new ArrayList<>();
        for (T item : items) {
            String id = idGetter.apply(item);
            // Insert if no id (Mongo will generate one) or if id is not yet present.
            if (id == null || !existingIds.contains(id)) toInsert.add(item);
        }
        if (toInsert.isEmpty()) return;

        repository.saveAll(toInsert);
        System.out.println("Seeded " + toInsert.size() + " " + type.getSimpleName()
                + " records from " + resourcePath + " (" + (items.size() - toInsert.size()) + " skipped, already present)");
    }

    private <T> List<T> readSeed(String resourcePath, Class<T> type) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        return objectMapper.readValue(
                resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }
}
