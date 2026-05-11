/**
 * Seeds the dev database on startup. Behavior is gated by `app.seed.reset`:
 *
 * - `app.seed.reset=true` (default in dev): drops the showcase + deck collections
 *   on every boot so the latest seed data takes effect without manual cleanup.
 *   User-created decks are also wiped — accept the trade-off in exchange for
 *   never having stale schema versions hanging around during model churn.
 *
 * - `app.seed.reset=false`: only seeds when each target collection is empty.
 *
 * The Welcome Tour deck exercises every element kind so the runtime is demoable
 * end-to-end. A second General Knowledge deck provides a pure-trivia option.
 */
package cephadex.brainflex.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cephadex.brainflex.model.Deck;
import cephadex.brainflex.model.ShowcaseSettings;
import cephadex.brainflex.model.User;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.GridCellsConfig;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.McqOption;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingItem;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScaleStatement;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.enums.DeckPreset;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.PlaceScoring;
import cephadex.brainflex.model.enums.RankingScoring;
import cephadex.brainflex.model.enums.SlideKind;
import cephadex.brainflex.repository.AudienceSubmissionRepository;
import cephadex.brainflex.repository.BestAnswerVoteRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.ShowcaseRepository;
import cephadex.brainflex.repository.ShowcaseResultRepository;
import cephadex.brainflex.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Value("${app.seed.reset:true}")
    private boolean resetOnStartup;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Bean
    @SuppressWarnings("unused")
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            DeckRepository deckRepository,
            ShowcaseRepository showcaseRepository,
            ShowcaseResultRepository showcaseResultRepository,
            AudienceSubmissionRepository audienceSubmissionRepository,
            BestAnswerVoteRepository bestAnswerVoteRepository) {
        return args -> {
            seedIfEmpty(userRepository, "seed/users.json", User.class);

            if (resetOnStartup) {
                showcaseRepository.deleteAll();
                showcaseResultRepository.deleteAll();
                audienceSubmissionRepository.deleteAll();
                bestAnswerVoteRepository.deleteAll();
                deckRepository.deleteAll();
                System.out.println("Seed reset: cleared decks + showcases + showcase_results + submissions + votes");
            }

            if (deckRepository.count() == 0) {
                deckRepository.save(buildWelcomeTourDeck());
                deckRepository.save(buildGeneralKnowledgeDeck());
                System.out.println("Seeded Welcome Tour + General Knowledge decks");
            }
        };
    }

    private <T> void seedIfEmpty(
            MongoRepository<T, String> repository, String resourcePath, Class<T> type) throws Exception {
        if (repository.count() > 0) return;
        ClassPathResource resource = new ClassPathResource(resourcePath);
        List<T> items = objectMapper.readValue(
                resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        repository.saveAll(items);
        System.out.println("Seeded " + items.size() + " " + type.getSimpleName() + " records from " + resourcePath);
    }

    // ---- Welcome Tour deck ----

    private static Deck buildWelcomeTourDeck() {
        Deck deck = new Deck();
        deck.setId("6650000000000000000001");
        deck.setName("BrainFlex Welcome Tour");
        deck.setDescription("A quick tour through every kind of element you can put in a deck. Every type, one round each.");
        deck.setTags(List.of("welcome", "tour", "every-type"));
        deck.setSystem(true);
        
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setRecommendedPreset(DeckPreset.GAME);
        deck.setCoverImageUrl("https://picsum.photos/seed/brainflex-welcome-tour/480/280");
        deck.setBackgroundImageUrl("https://picsum.photos/seed/brainflex-welcome-tour-bg/1600/1000");
        deck.setEstimatedDurationMinutes(8);
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());

        ShowcaseSettings defaults = new ShowcaseSettings();
        defaults.setTotalRounds(16);
        defaults.setScoringEnabled(true);
        defaults.setSpeedBonus(true);
        deck.setDefaultSettings(defaults);

        List<DeckElement> els = new ArrayList<>();

        els.add(new Slide("wt-s-1", SlideKind.TITLE,
                "Welcome to BrainFlex",
                "A quick tour through every kind of element a deck can contain. Press the screen to begin.",
                6, null, null, null, null, null, MediaPosition.NONE));

        els.add(new Slide("wt-s-2", SlideKind.SECTION,
                "Trivia round",
                "Multiple choice, then free-text, then a number guess.",
                4, null, null, null, null, null, MediaPosition.NONE));

        // MCQ
        List<McqOption> mcqOpts = List.of(
                new McqOption("mars-opt-1", "Venus", null),
                new McqOption("mars-opt-2", "Jupiter", null),
                new McqOption("mars-opt-3", "Mars", null),
                new McqOption("mars-opt-4", "Saturn", null));
        els.add(new McqQuestion("wt-mcq-1",
                "Which planet is known as the Red Planet?",
                mcqOpts, "mars-opt-3",
                100, Difficulty.EASY,
                false, 0, "Mars looks red because of iron oxide (rust) on its surface.",
                15, null, null, null, null, null, MediaPosition.NONE));

        // TEXT
        els.add(new TextQuestion("wt-text-1",
                "What is the capital of France?",
                "Paris", List.of("paree"), false,
                150, Difficulty.EASY,
                false, 0, "Paris has been France's capital since 987 AD.",
                15, null, null, null, null, null, MediaPosition.NONE));

        // NUMBER
        els.add(new NumberQuestion("wt-num-1",
                "How many planets are in our solar system?",
                8.0, 0.0, " planets", 0,
                150, Difficulty.EASY,
                false, 0, "Pluto was reclassified as a dwarf planet in 2006.",
                15, null, null, null, null, null, MediaPosition.NONE));

        els.add(new Slide("wt-s-3", SlideKind.SECTION,
                "Order and rate",
                "Drag to reorder, then rate some statements.",
                4, null, null, null, null, null, MediaPosition.NONE));

        // RANKING
        List<RankingItem> planets = List.of(
                new RankingItem("planet-mercury", "Mercury", null),
                new RankingItem("planet-venus", "Venus", null),
                new RankingItem("planet-earth", "Earth", null),
                new RankingItem("planet-mars", "Mars", null));
        els.add(new RankingQuestion("wt-rank-1",
                "Order these planets from closest to farthest from the Sun.",
                planets,
                List.of("planet-mercury", "planet-venus", "planet-earth", "planet-mars"),
                RankingScoring.PARTIAL,
                200, Difficulty.MEDIUM,
                false, 0, "Distance order from the Sun outward.",
                20, null, null, null, null, null, MediaPosition.NONE));

        // SCALES (unscored — pulse style)
        List<ScaleStatement> features = List.of(
                new ScaleStatement("feat-realtime", "Real-time multiplayer gameplay"),
                new ScaleStatement("feat-pulse", "Audience polling (Pulse)"),
                new ScaleStatement("feat-slides", "Slides + media in decks"),
                new ScaleStatement("feat-bestanswer", "Best Answer voting"));
        els.add(new ScalesQuestion("wt-scales-1",
                "How excited are you about each of these features?",
                features, 1, 5, "Meh", "Hyped",
                false, List.of(),     // unscored: ignore correctRatings
                0, Difficulty.EASY,
                false, 0, null,
                25, null, null, null, null, null, MediaPosition.NONE));

        els.add(new Slide("wt-s-4", SlideKind.SECTION,
                "Audience interaction",
                "Vote on the funniest answer, then ask anything.",
                4, null, null, null, null, null, MediaPosition.NONE));

        // BEST ANSWER MODE on a TextQuestion
        els.add(new TextQuestion("wt-best-1",
                "If our next deck had a one-word theme, what would it be?",
                "open",                              // no canonical answer; bestAnswerMode picks winner
                List.of(), false,
                0, Difficulty.EASY,
                true, 100, "Best Answer mode — players vote on the most creative response.",
                30, null, null, null, null, null, MediaPosition.NONE));

        // Q&A
        els.add(new QAndAQuestion("wt-qanda-1",
                "Ask the host anything about how BrainFlex works.",
                3, true, false,
                0, Difficulty.EASY,
                false, 0, null,
                45, "Audience asks freely; you pin the ones you want to address.",
                null, null, null, null, MediaPosition.NONE));

        els.add(new Slide("wt-s-5", SlideKind.SECTION,
                "Visual round",
                "Tap cells, place a pin, pick an image.",
                4, null, null, null, null, null, MediaPosition.NONE));

        // GRID (3×3 — select primes)
        els.add(new GridQuestion("wt-grid-1",
                "Select all the prime numbers.",
                3, 3,
                new GridCellsConfig(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), null),
                Set.of(1, 2, 4, 6),                  // indexes of 2, 3, 5, 7
                true,
                200, Difficulty.MEDIUM,
                false, 0, "Primes: 2, 3, 5, 7.",
                20, null, null, null, null, null, MediaPosition.NONE));

        // PLACE ON IMAGE
        els.add(new PlaceOnImageQuestion("wt-place-1",
                "Click roughly where Italy would be on this map.",
                "https://picsum.photos/seed/brainflex-welcome-map/1200/800",
                0.55, 0.42, 0.08,
                PlaceScoring.LINEAR,
                200, Difficulty.MEDIUM,
                false, 0, "Lorem Picsum stands in for a real map until we wire one up.",
                25, null, null, null, null, null, MediaPosition.NONE));

        // IMAGE CHOICE (using Lorem Picsum stand-ins for each option)
        List<McqOption> landmarks = List.of(
                new McqOption("lm-eiffel", "Eiffel Tower",
                        "https://picsum.photos/seed/landmark-eiffel/400/300"),
                new McqOption("lm-pisa", "Leaning Tower of Pisa",
                        "https://picsum.photos/seed/landmark-pisa/400/300"),
                new McqOption("lm-bigben", "Big Ben",
                        "https://picsum.photos/seed/landmark-bigben/400/300"),
                new McqOption("lm-statue", "Statue of Liberty",
                        "https://picsum.photos/seed/landmark-statue/400/300"));
        els.add(new cephadex.brainflex.model.element.ImageChoiceQuestion("wt-img-1",
                "Which of these is the Eiffel Tower?",
                landmarks, "lm-eiffel",
                150, Difficulty.EASY,
                false, 0, "Image-choice variant of MCQ — options carry images.",
                20, null, null, null, null, null, MediaPosition.NONE));

        els.add(new Slide("wt-s-end", SlideKind.END,
                "Thanks for playing!",
                "That's every element type. Now go build your own deck.",
                8, null, null, null, null, null, MediaPosition.NONE));

        deck.setElements(els);
        return deck;
    }

    // ---- General Knowledge deck (pure trivia) ----

    private static Deck buildGeneralKnowledgeDeck() {
        Deck deck = new Deck();
        deck.setId("6650000000000000000002");
        deck.setName("General Knowledge");
        deck.setDescription("A mix of geography, history, science, and pop culture. MCQ + text-input only.");
        deck.setTags(List.of("general", "trivia"));
        deck.setSystem(true);
        
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setRecommendedPreset(DeckPreset.GAME);
        deck.setCoverImageUrl("https://picsum.photos/seed/brainflex-general-knowledge/480/280");
        deck.setBackgroundImageUrl("https://picsum.photos/seed/brainflex-general-knowledge-bg/1600/1000");
        deck.setEstimatedDurationMinutes(6);
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());

        ShowcaseSettings defaults = new ShowcaseSettings();
        defaults.setTotalRounds(8);
        deck.setDefaultSettings(defaults);

        List<DeckElement> els = new ArrayList<>();
        els.add(mcq("gk-1", "What is the capital city of Australia?",
                List.of("Sydney", "Melbourne", "Canberra", "Brisbane"), 2, 100, Difficulty.EASY));
        els.add(mcq("gk-2", "Who painted the Mona Lisa?",
                List.of("Michelangelo", "Raphael", "Leonardo da Vinci", "Donatello"), 2, 100, Difficulty.EASY));
        els.add(mcq("gk-3", "What language has the most native speakers worldwide?",
                List.of("English", "Spanish", "Hindi", "Mandarin Chinese"), 3, 200, Difficulty.MEDIUM));
        els.add(mcq("gk-4", "In what year did World War II end?",
                List.of("1943", "1944", "1945", "1946"), 2, 200, Difficulty.MEDIUM));
        els.add(mcq("gk-5", "Which element has the chemical symbol 'Au'?",
                List.of("Silver", "Copper", "Aluminum", "Gold"), 3, 200, Difficulty.MEDIUM));
        els.add(new TextQuestion("gk-6",
                "What is the capital of France?",
                "Paris", List.of("paree"), false,
                150, Difficulty.EASY,
                false, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE));
        els.add(new TextQuestion("gk-7",
                "Who wrote the play 'Hamlet'?",
                "Shakespeare", List.of("William Shakespeare"), false,
                200, Difficulty.MEDIUM,
                false, 0, null,
                20, null, null, null, null, null, MediaPosition.NONE));
        els.add(new NumberQuestion("gk-8",
                "How many planets are in our solar system?",
                8.0, 0.0, " planets", 0,
                150, Difficulty.EASY,
                false, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE));

        deck.setElements(els);
        return deck;
    }

    private static McqQuestion mcq(String id, String prompt, List<String> options, int correctIndex,
                                   int pointValue, Difficulty difficulty) {
        List<McqOption> opts = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            opts.add(new McqOption(id + "-opt-" + i, options.get(i), null));
        }
        return new McqQuestion(id, prompt, opts,
                opts.get(correctIndex).id(),
                pointValue, difficulty,
                false, 0, null,
                15, null, null, null, null, null, MediaPosition.NONE);
    }
}
