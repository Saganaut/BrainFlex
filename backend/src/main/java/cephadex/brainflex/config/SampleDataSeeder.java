/**
 * Manual sample-data seeder, triggered by `scripts/seed-sample-data.sh`.
 *
 * Gated on `--seed.run=true` so it never runs during a normal boot. The script
 * runs the Spring Boot app with that flag, which fires this ApplicationRunner
 * once and then exits.
 *
 * Idempotency is per-collection: a user that already has decks won't get more
 * decks, but they'll still receive a theme or an org slot if those are missing.
 * Nothing is ever deleted — re-running the script tops up missing pieces.
 *
 * Faction grouping (Fellowship of the Ring, White Council, Free Peoples of
 * Rohan, Shire Folk, Independent Adventurers) is keyed off email/name; users
 * that don't match a faction land in Independent Adventurers.
 */
package cephadex.brainflex.config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.deck.Tag;
import cephadex.brainflex.model.element.DeckElement;
import cephadex.brainflex.model.element.ElementChrome;
import cephadex.brainflex.model.element.GridQuestion;
import cephadex.brainflex.model.element.McqQuestion;
import cephadex.brainflex.model.element.NumberQuestion;
import cephadex.brainflex.model.element.PlaceOnImageQuestion;
import cephadex.brainflex.model.element.QAndAQuestion;
import cephadex.brainflex.model.element.RankingQuestion;
import cephadex.brainflex.model.element.ScalesQuestion;
import cephadex.brainflex.model.element.Slide;
import cephadex.brainflex.model.element.TextQuestion;
import cephadex.brainflex.model.element.block.BodyBlock;
import cephadex.brainflex.model.element.block.SlideBlock;
import cephadex.brainflex.model.element.parts.GridCellsConfig;
import cephadex.brainflex.model.element.parts.McqOption;
import cephadex.brainflex.model.element.parts.RankingItem;
import cephadex.brainflex.model.element.parts.ScaleStatement;
import cephadex.brainflex.model.enums.AchievementTrigger;
import cephadex.brainflex.model.enums.BestAnswerScoring;
import cephadex.brainflex.model.enums.DeckVisibility;
import cephadex.brainflex.model.enums.Difficulty;
import cephadex.brainflex.model.enums.JoinType;
import cephadex.brainflex.model.enums.License;
import cephadex.brainflex.model.enums.MediaPosition;
import cephadex.brainflex.model.enums.PlaceScoring;
import cephadex.brainflex.model.enums.PublishStatus;
import cephadex.brainflex.model.enums.RankingScoring;
import cephadex.brainflex.model.enums.ResponseMode;
import cephadex.brainflex.model.enums.ResultsDisplayType;
import cephadex.brainflex.model.enums.SessionFormat;
import cephadex.brainflex.model.enums.ShowResponsesMode;
import cephadex.brainflex.model.enums.SlideKind;
import cephadex.brainflex.model.image.Image;
import cephadex.brainflex.model.org.Organization;
import cephadex.brainflex.model.session.InteractiveSessionSettings;
import cephadex.brainflex.model.theme.Theme;
import cephadex.brainflex.model.user.Achievement;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.repository.AchievementRepository;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.OrganizationRepository;
import cephadex.brainflex.repository.TagRepository;
import cephadex.brainflex.repository.ThemeRepository;
import cephadex.brainflex.repository.UserRepository;
import cephadex.brainflex.service.DeckCollaboratorService;
import cephadex.brainflex.service.TagService;

@Configuration
@ConditionalOnProperty(name = "seed.run", havingValue = "true")
public class SampleDataSeeder {

        private static final String WELCOME_TOUR_ID = "00000000-0000-4000-8000-000000000001";
        private static final String GENERAL_KNOWLEDGE_ID = "00000000-0000-4000-8000-000000000002";

        private final ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Autowired
        private ApplicationContext applicationContext;

        @Bean
        @SuppressWarnings("unused")
        ApplicationRunner runSampleDataSeed(
                        UserRepository userRepository,
                        DeckRepository deckRepository,
                        ThemeRepository themeRepository,
                        OrganizationRepository organizationRepository,
                        TagRepository tagRepository,
                        AchievementRepository achievementRepository,
                        TagService tagService,
                        DeckCollaboratorService deckCollaboratorService,
                        MongoTemplate mongoTemplate) {
                return (ApplicationArguments args) -> {
                        try {
                                System.out.println("=== Sample data seed: starting ===");

                                if (clearRequested(args)) {
                                        clearCollections(mongoTemplate);
                                }

                                ensureUsers(userRepository);
                                int tagsAdded = ensureCuratedTags(tagRepository);
                                ensureSystemDecks(deckRepository);
                                int achievementsAdded = ensureAchievementCatalog(achievementRepository);

                                List<User> users = userRepository.findAll();
                                System.out.println(
                                                "Found " + users.size() + " users — populating sample data per user…");

                                Map<String, Organization> orgsByFaction = ensureFactionOrganizations(
                                                users, userRepository, organizationRepository);

                                int themeCount = 0;
                                int deckCount = 0;
                                for (User user : users) {
                                        themeCount += ensureThemesForUser(user, userRepository, themeRepository);
                                        deckCount += ensureDecksForUser(user, deckRepository, deckCollaboratorService);
                                }

                                int recounted = tagService.recomputeDeckCounts(deckRepository.findAll());

                                System.out.println("=== Sample data seed: done ===");
                                System.out.println("  organizations:    " + orgsByFaction.size() + " factions");
                                System.out.println("  themes added:     " + themeCount);
                                System.out.println("  decks added:      " + deckCount);
                                System.out.println("  curated tags new: " + tagsAdded);
                                System.out.println("  achievements new: " + achievementsAdded);
                                System.out.println("  tag counts dirty: " + recounted);
                        } finally {
                                int exit = SpringApplication.exit(applicationContext, () -> 0);
                                System.exit(exit);
                        }
                };
        }

        // -------------------------------------------------------------- clearing

        /**
         * Honors `--seed.clear=true` from the script. The flag is a separate switch
         * from `--seed.run` so the normal idempotent top-up path stays the default.
         * Use it when a schema migration leaves stale documents that Spring Data
         * can no longer deserialize (e.g. a record gains a new primitive field and
         * the existing rows have nulls).
         */
        private static boolean clearRequested(ApplicationArguments args) {
                List<String> values = args.getOptionValues("seed.clear");
                return values != null && values.contains("true");
        }

        private static void clearCollections(MongoTemplate mongoTemplate) {
                List<String> collections = List.of(
                                "users",
                                "organizations",
                                "themes",
                                "decks",
                                "deck_collaborators",
                                "tags",
                                "gallery_images",
                                "interactive_sessions",
                                "interactive_session_results",
                                "audience_submissions",
                                "best_answer_votes",
                                "achievements",
                                "user_achievements");
                System.out.println("--seed.clear=true → dropping collections");
                for (String name : collections) {
                        long count = mongoTemplate.getCollection(name).countDocuments();
                        mongoTemplate.dropCollection(name);
                        System.out.println("  dropped " + name + " (" + count + " docs)");
                }
        }

        // ---------------------------------------------------------------- users

        private void ensureUsers(UserRepository userRepository) throws Exception {
                if (userRepository.count() > 0) {
                        System.out.println("Users already present — skipping users.json load.");
                        return;
                }
                ClassPathResource resource = new ClassPathResource("seed/users.json");
                List<User> users = objectMapper.readValue(
                                resource.getInputStream(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
                userRepository.saveAll(users);
                System.out.println("Seeded " + users.size() + " users from seed/users.json");
        }

        // ----------------------------------------------------- curated tags

        /**
         * Seeds the curated subject taxonomy used by the Explore page filters.
         * Idempotent per tag id — a re-run only inserts missing rows. Existing
         * tags (admin-edited names, descriptions) are never overwritten.
         */
        private int ensureCuratedTags(TagRepository tagRepository) {
                record CuratedTag(String id, String displayName, String description) {
                }
                List<CuratedTag> curated = List.of(
                                new CuratedTag("general-knowledge", "General Knowledge",
                                                "Mixed trivia spanning multiple subjects."),
                                new CuratedTag("math", "Math",
                                                "Arithmetic, algebra, geometry, and beyond."),
                                new CuratedTag("history", "History",
                                                "World history, eras, and notable events."),
                                new CuratedTag("science", "Science",
                                                "Biology, chemistry, physics, and earth science."),
                                new CuratedTag("sports", "Sports",
                                                "Teams, athletes, rules, and game-day facts."),
                                new CuratedTag("pop-culture", "Pop Culture",
                                                "Film, TV, music, and internet phenomena."),
                                new CuratedTag("trivia", "Trivia",
                                                "Catch-all bucket for grab-bag question decks."));
                int added = 0;
                for (CuratedTag entry : curated) {
                        if (tagRepository.existsById(entry.id()))
                                continue;
                        Tag tag = new Tag();
                        tag.setId(entry.id());
                        tag.setDisplayName(entry.displayName());
                        tag.setDescription(entry.description());
                        tag.setCurated(true);
                        tag.setDeckCount(0);
                        tagRepository.save(tag);
                        added++;
                }
                return added;
        }

        // ------------------------------------------------------- achievements

        /**
         * Seeds the chunk-17 starter achievement catalog. Slugs are stable so the
         * loader is idempotent — re-running tops up missing rows but never edits
         * or deletes existing ones (admin-tweaked names/descriptions stick).
         *
         * Categories: starter (introductory milestones), scoring (point/score
         * thresholds), games (volume), social (favorites, hidden surprises),
         * creator (decks created/published), host (games hosted).
         */
        private int ensureAchievementCatalog(AchievementRepository achievementRepository) {
                record Seed(String id, String name, String description, String category,
                                AchievementTrigger trigger, int threshold, int rewardPoints,
                                boolean hidden, int displayOrder) {
                }
                List<Seed> catalog = List.of(
                                // -------- starter --------
                                new Seed("first-game", "First Steps",
                                                "Finish your first game.", "starter",
                                                AchievementTrigger.FIRST_GAME, 1, 50, false, 10),
                                new Seed("first-deck", "Author",
                                                "Create your first deck.", "starter",
                                                AchievementTrigger.DECKS_CREATED, 1, 50, false, 20),
                                new Seed("first-publish", "Shared with the World",
                                                "Publish your first deck.", "starter",
                                                AchievementTrigger.DECKS_PUBLISHED, 1, 100, false, 30),

                                // -------- scoring --------
                                new Seed("points-1k", "Rising Star",
                                                "Earn 1,000 lifetime points.", "scoring",
                                                AchievementTrigger.TOTAL_POINTS, 1_000, 50, false, 40),
                                new Seed("points-10k", "Veteran",
                                                "Earn 10,000 lifetime points.", "scoring",
                                                AchievementTrigger.TOTAL_POINTS, 10_000, 250, false, 50),
                                new Seed("points-100k", "Legend",
                                                "Earn 100,000 lifetime points.", "scoring",
                                                AchievementTrigger.TOTAL_POINTS, 100_000, 1_000, false, 60),
                                new Seed("high-score-2k", "Big Round",
                                                "Score 2,000 in a single game.", "scoring",
                                                AchievementTrigger.HIGH_SCORE, 2_000, 100, false, 70),

                                // -------- games --------
                                new Seed("games-10", "Regular",
                                                "Finish 10 games.", "scoring",
                                                AchievementTrigger.GAMES_PLAYED, 10, 100, false, 80),
                                new Seed("games-100", "Frequent Player",
                                                "Finish 100 games.", "scoring",
                                                AchievementTrigger.GAMES_PLAYED, 100, 500, false, 90),
                                new Seed("games-1000", "Devotee",
                                                "Finish 1,000 games.", "scoring",
                                                AchievementTrigger.GAMES_PLAYED, 1_000, 2_500, false, 100),

                                // -------- streaks & perfect --------
                                new Seed("streak-5", "On a Roll",
                                                "Answer 5 questions correctly in a row.", "scoring",
                                                AchievementTrigger.STREAK, 5, 100, false, 110),
                                new Seed("streak-10", "Unstoppable",
                                                "Answer 10 questions correctly in a row.", "scoring",
                                                AchievementTrigger.STREAK, 10, 250, false, 120),
                                new Seed("perfect-5", "Flawless",
                                                "Answer every question right in a 5+ question game.", "scoring",
                                                AchievementTrigger.PERFECT_GAME, 5, 250, true, 130),

                                // -------- host --------
                                new Seed("host-10", "Quizmaster",
                                                "Host 10 games.", "host",
                                                AchievementTrigger.HOST_GAMES, 10, 200, false, 140),
                                new Seed("host-100", "Master of Ceremonies",
                                                "Host 100 games.", "host",
                                                AchievementTrigger.HOST_GAMES, 100, 1_000, false, 150),

                                // -------- creator & social --------
                                new Seed("decks-10", "Prolific Author",
                                                "Create 10 decks.", "creator",
                                                AchievementTrigger.DECKS_CREATED, 10, 250, false, 160),
                                new Seed("decks-published-5", "Curator",
                                                "Publish 5 decks.", "creator",
                                                AchievementTrigger.DECKS_PUBLISHED, 5, 250, false, 170),
                                new Seed("favorites-10", "Crowd Favorite",
                                                "Have one of your decks favorited 10 times.", "social",
                                                AchievementTrigger.FAVORITES_RECEIVED, 10, 200, false, 180));

                int added = 0;
                Instant now = Instant.now();
                for (Seed seed : catalog) {
                        if (achievementRepository.existsById(seed.id()))
                                continue;
                        Achievement a = new Achievement();
                        a.setId(seed.id());
                        a.setName(seed.name());
                        a.setDescription(seed.description());
                        a.setCategory(seed.category());
                        a.setTrigger(seed.trigger());
                        a.setThreshold(seed.threshold());
                        a.setRewardPoints(seed.rewardPoints());
                        a.setHidden(seed.hidden());
                        a.setDisplayOrder(seed.displayOrder());
                        a.setCreatedAt(now);
                        achievementRepository.save(a);
                        added++;
                }
                return added;
        }

        // ------------------------------------------------------- system decks

        private void ensureSystemDecks(DeckRepository deckRepository) {
                if (!deckRepository.existsById(WELCOME_TOUR_ID)) {
                        deckRepository.save(buildWelcomeTourDeck());
                        System.out.println("Seeded system deck: Welcome Tour");
                }
                if (!deckRepository.existsById(GENERAL_KNOWLEDGE_ID)) {
                        deckRepository.save(buildGeneralKnowledgeDeck());
                        System.out.println("Seeded system deck: General Knowledge");
                }
        }

        // ----------------------------------------------------- organizations

        /**
         * Groups users into LOTR factions, creating one Organization per faction
         * (owned by a representative member). Each user's `organizationId` is set
         * if currently null. Users already in an org are left alone.
         */
        private Map<String, Organization> ensureFactionOrganizations(
                        List<User> users,
                        UserRepository userRepository,
                        OrganizationRepository organizationRepository) {

                Map<String, List<User>> byFaction = new LinkedHashMap<>();
                for (User user : users) {
                        String faction = factionFor(user);
                        byFaction.computeIfAbsent(faction, k -> new ArrayList<>()).add(user);
                }

                Map<String, Organization> result = new HashMap<>();
                for (Map.Entry<String, List<User>> entry : byFaction.entrySet()) {
                        String factionName = entry.getKey();
                        List<User> members = entry.getValue();
                        if (members.isEmpty())
                                continue;

                        User owner = members.getFirst();
                        Organization org = organizationRepository.findByOwnerId(owner.getId()).orElse(null);
                        if (org == null) {
                                // Owner has no org yet — check if any member already established this faction's
                                // org.
                                org = organizationRepository.findAll().stream()
                                                .filter(o -> factionName.equals(o.getName()))
                                                .findFirst()
                                                .orElse(null);
                        }
                        if (org == null) {
                                org = new Organization();
                                org.setName(factionName);
                                org.setOwnerId(owner.getId());
                                org = organizationRepository.save(org);
                                System.out.println("  Org created: " + factionName + " (owner=" + owner.getUserName()
                                                + ")");
                        }
                        result.put(factionName, org);

                        for (User member : members) {
                                var orgIds = member.getOrganizationIds();
                                if (orgIds == null) {
                                        orgIds = new ArrayList<>();
                                        member.setOrganizationIds(orgIds);
                                }
                                if (!orgIds.contains(org.getId())) {
                                        orgIds.add(org.getId());
                                        userRepository.save(member);
                                }
                        }
                }
                return result;
        }

        /** First org in the user's memberships, or null if they belong to none. */
        private static String firstOrgId(User user) {
                var orgIds = user.getOrganizationIds();
                return (orgIds == null || orgIds.isEmpty()) ? null : orgIds.getFirst();
        }

        private static String factionFor(User user) {
                String userName = user.getUserName() == null ? "" : user.getUserName();
                String name = user.getName() == null ? "" : user.getName();
                Set<String> fellowship = Set.of(
                                "RingBearer99", "Mithrandir", "Strider", "PrinceOfMirkwood",
                                "AxeMaster", "GardenerOfTheYear", "OneDoesNotSimply",
                                "Merry_Buck", "FoolOfATook");
                if (fellowship.contains(userName))
                        return "Fellowship of the Ring";

                Set<String> whiteCouncil = Set.of("LightOfEarendel", "CouncilChairman", "Sharkey");
                if (whiteCouncil.contains(userName))
                        return "White Council";

                Set<String> rohan = Set.of("IAmNoMan", "QualityCaptain");
                if (rohan.contains(userName))
                        return "Free Peoples of Rohan";

                if (userName.equals("Precious_Slinker") || name.equals("Sméagol")) {
                        return "Misty Mountains Loners";
                }
                return "Independent Adventurers";
        }

        // ----------------------------------------------------------- themes

        /**
         * Gives each user a personal LOTR-flavored theme if they don't already have
         * any custom themes. Sets the user's `activeThemeId` only when the field is
         * currently null (avoids overriding a real user preference).
         */
        private int ensureThemesForUser(User user, UserRepository userRepository, ThemeRepository themeRepository) {
                if (!themeRepository.findByOwnerId(user.getId()).isEmpty()) {
                        return 0;
                }
                FactionPalette palette = paletteFor(user);
                Theme theme = new Theme();
                theme.setName(palette.themeName);
                theme.setOwnerId(user.getId());
                theme.setOrganizationId(firstOrgId(user));
                theme.setHuePrimary(palette.huePrimary);
                theme.setHueAccent(palette.hueAccent);
                theme.setMode(cephadex.brainflex.model.enums.ThemeMode.valueOf(palette.mode.toUpperCase()));
                theme = themeRepository.save(theme);

                if (user.getActiveThemeId() == null) {
                        user.setActiveThemeId(theme.getId());
                        userRepository.save(user);
                }
                return 1;
        }

        private record FactionPalette(String themeName, int huePrimary, int hueAccent, String mode) {
        }

        private static FactionPalette paletteFor(User user) {
                String faction = factionFor(user);
                return switch (faction) {
                        case "Fellowship of the Ring" -> new FactionPalette("Elven Twilight", 230, 100, "dark");
                        case "White Council" -> new FactionPalette("Wizard's Counsel", 270, 50, "light");
                        case "Free Peoples of Rohan" -> new FactionPalette("Plains of Rohan", 35, 145, "light");
                        case "Misty Mountains Loners" -> new FactionPalette("Caves of Moria", 200, 110, "dark");
                        default -> new FactionPalette("Shire Morning", 95, 30, "light");
                };
        }

        // ------------------------------------------------------------ decks

        /**
         * For each user, creates 1-2 LOTR-themed personal decks if they don't yet
         * own any. Picks decks deterministically from a small library keyed on the
         * user's faction so the same user always gets the same starter content.
         */
        private int ensureDecksForUser(
                        User user,
                        DeckRepository deckRepository,
                        DeckCollaboratorService deckCollaboratorService) {
                if (!deckRepository.findByCreatorUserId(user.getId()).isEmpty()) {
                        return 0;
                }
                List<Deck> decks = startersFor(user);
                for (Deck deck : decks) {
                        deck.setCreatorUserId(user.getId());
                        deck.setOrganizationId(firstOrgId(user));
                        Deck saved = deckRepository.save(deck);
                        deckCollaboratorService.addInitialOwner(saved, user);
                }
                return decks.size();
        }

        private static List<Deck> startersFor(User user) {
                String faction = factionFor(user);
                return switch (faction) {
                        case "Fellowship of the Ring" -> List.of(
                                        buildFellowshipTriviaDeck(user),
                                        buildSecondBreakfastPulseDeck(user));
                        case "White Council" -> List.of(
                                        buildAncientLoreDeck(user));
                        case "Free Peoples of Rohan" -> List.of(
                                        buildRohirrimRideDeck(user));
                        case "Misty Mountains Loners" -> List.of(
                                        buildRiddlesInTheDarkDeck(user));
                        default -> List.of(
                                        buildShireFolkDeck(user));
                };
        }

        // ---------- per-user decks (LOTR-themed) -------------------------------

        private static Deck buildFellowshipTriviaDeck(User user) {
                Deck deck = baseDeck(
                                "Fellowship Trivia — " + user.getUserName(),
                                "A grab-bag of trivia about the nine walkers, from Bag End to Mount Doom.",
                                List.of("lotr", "trivia", "fellowship"),
                                "fellowship-trivia");
                deck.getContent().setFormat(SessionFormat.GAME);
                deck.setEstimatedDurationMinutes(7);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("ftd-s-1", "Fellowship Trivia",
                                "Nine walkers set out from Rivendell. How well do you know them?"));

                els.add(mcq("ftd-mcq-1", "Who carried the One Ring from the Shire to Mount Doom?",
                                List.of("Aragorn", "Frodo Baggins", "Samwise Gamgee", "Boromir"), 1,
                                100, Difficulty.EASY));

                els.add(textQ("ftd-text-1", "What is the name of Aragorn's sword, reforged at Rivendell?",
                                "Andúril", List.of("Anduril", "Flame of the West"),
                                200, Difficulty.MEDIUM));

                els.add(new NumberQuestion("ftd-num-1",
                                "How many members were there in the Fellowship of the Ring?",
                                9.0, 0.0, " members", 0,
                                150, Difficulty.EASY,
                                "Four hobbits, two men, an elf, a dwarf, and a wizard.",
                                null, null, true,
                                seedChrome("ftd-num-1", "How many members were there in the Fellowship of the Ring?",
                                                true, false, 15)));

                els.add(mcq("ftd-mcq-2", "Who breaks the Fellowship by attempting to take the Ring from Frodo?",
                                List.of("Aragorn", "Legolas", "Boromir", "Pippin"), 2,
                                150, Difficulty.MEDIUM));

                els.add(endSlide("ftd-s-end", "Well done, traveller.",
                                "Even the smallest person can change the course of the future."));
                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildSecondBreakfastPulseDeck(User user) {
                Deck deck = baseDeck(
                                "Second Breakfast — " + user.getUserName(),
                                "A Pulse-style poll deck — no scoring, just hobbit hot takes.",
                                List.of("lotr", "pulse", "hobbits"),
                                "second-breakfast");
                deck.getContent().setFormat(SessionFormat.PRESENTATION);
                deck.setEstimatedDurationMinutes(4);
                InteractiveSessionSettings settings = deck.getContent().getSettings();
                settings.setScoringEnabled(false);
                settings.setTotalRounds(4);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("sb-s-1", "Second Breakfast",
                                "All polls. No wrong answers. Pass the cram."));

                List<ScaleStatement> meals = List.of(
                                new ScaleStatement("meal-breakfast", "Breakfast"),
                                new ScaleStatement("meal-second", "Second breakfast"),
                                new ScaleStatement("meal-elevenses", "Elevenses"),
                                new ScaleStatement("meal-luncheon", "Luncheon"),
                                new ScaleStatement("meal-tea", "Afternoon tea"),
                                new ScaleStatement("meal-supper", "Supper"));
                els.add(new ScalesQuestion("sb-scales-1",
                                "Rate how essential each meal is to a proper hobbit day.",
                                meals, 1, 5, "Skip it", "Sacred", List.of(),
                                0, Difficulty.EASY, null,
                                seedChrome("sb-scales-1", "Rate how essential each meal is to a proper hobbit day.",
                                                false, true, 30)));

                els.add(new QAndAQuestion("sb-qanda-1",
                                "What's your strongest hobbit hot take? Submit anything.",
                                3, true, false,
                                0, Difficulty.EASY, null,
                                false, 0,
                                seedChrome("sb-qanda-1", "What's your strongest hobbit hot take? Submit anything.",
                                                false, true, null, 45, "Pin the best ones to share.",
                                                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE)));

                els.add(endSlide("sb-s-end", "Mind your taters.",
                                "PO-TA-TOES. Boil 'em, mash 'em, stick 'em in a stew."));
                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildAncientLoreDeck(User user) {
                Deck deck = baseDeck(
                                "Ancient Lore — " + user.getUserName(),
                                "For wizards, lore-masters, and anyone who reads the appendices.",
                                List.of("lotr", "lore", "advanced"),
                                "ancient-lore");
                deck.getContent().setFormat(SessionFormat.GAME);
                deck.setEstimatedDurationMinutes(8);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("al-s-1", "Ancient Lore",
                                "The deep cuts. The First Age. The forging."));

                els.add(mcq("al-mcq-1", "Which ring of power was held by Galadriel?",
                                List.of("Narya", "Nenya", "Vilya", "The One Ring"), 1,
                                200, Difficulty.HARD));

                els.add(textQ("al-text-1", "What is the name of the smith who forged the Rings of Power?",
                                "Celebrimbor", List.of(),
                                250, Difficulty.HARD));

                List<RankingItem> ages = List.of(
                                new RankingItem("age-first", "First Age", null),
                                new RankingItem("age-second", "Second Age", null),
                                new RankingItem("age-third", "Third Age", null),
                                new RankingItem("age-fourth", "Fourth Age", null));
                els.add(new RankingQuestion("al-rank-1",
                                "Order the Ages of Middle-earth from earliest to latest.",
                                ages,
                                List.of("age-first", "age-second", "age-third", "age-fourth"),
                                RankingScoring.PARTIAL,
                                250, Difficulty.MEDIUM,
                                "An Age ends with a great war or sundering.",
                                true,
                                seedChrome("al-rank-1", "Order the Ages of Middle-earth from earliest to latest.",
                                                true, false, 25)));

                els.add(new GridQuestion("al-grid-1",
                                "Select all the Valar (not Maiar) below.",
                                3, 2,
                                new GridCellsConfig(
                                                List.of("Manwë", "Sauron", "Varda", "Gandalf", "Aulë", "Saruman"),
                                                null),
                                Set.of(0, 2, 4),
                                true,
                                300, Difficulty.HARD,
                                "Valar are the greater powers; Sauron, Gandalf, and Saruman are all Maiar.",
                                seedChrome("al-grid-1", "Select all the Valar (not Maiar) below.",
                                                true, false, 3, 30, null,
                                                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE)));

                els.add(endSlide("al-s-end", "Even the wise cannot see all ends.",
                                "Thank you for studying with us."));
                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildRohirrimRideDeck(User user) {
                Deck deck = baseDeck(
                                "Riders of Rohan — " + user.getUserName(),
                                "Honor, horses, and the long ride to Minas Tirith.",
                                List.of("lotr", "rohan", "trivia"),
                                "rohirrim");
                deck.setEstimatedDurationMinutes(6);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("rr-s-1", "Riders of Rohan",
                                "Where now the horse and the rider?"));

                els.add(mcq("rr-mcq-1", "Who slew the Witch-king of Angmar?",
                                List.of("Aragorn", "Éowyn", "Théoden", "Faramir"), 1,
                                200, Difficulty.MEDIUM));

                els.add(textQ("rr-text-1", "What is the name of Gandalf's horse, Lord of all horses?",
                                "Shadowfax", List.of(),
                                200, Difficulty.MEDIUM));

                els.add(new PlaceOnImageQuestion("rr-place-1",
                                "Click roughly where Edoras would be on this map of Rohan.",
                                Image.external("https://picsum.photos/seed/middle-earth-rohan/1200/800"),
                                0.5, 0.5, 0.1,
                                PlaceScoring.LINEAR,
                                200, Difficulty.MEDIUM,
                                "Lorem Picsum placeholder until a real map ships.",
                                seedChrome("rr-place-1", "Click roughly where Edoras would be on this map of Rohan.",
                                                true, false, 25)));

                els.add(endSlide("rr-s-end", "Forth Eorlingas!",
                                "Ride now, ride now! Ride to ruin and the world's ending!"));
                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildRiddlesInTheDarkDeck(User user) {
                Deck deck = baseDeck(
                                "Riddles in the Dark — " + user.getUserName(),
                                "Old riddles, traded over a cold stone. Guess the answer.",
                                List.of("lotr", "riddles", "text"),
                                "riddles-dark");
                deck.setEstimatedDurationMinutes(6);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("rd-s-1", "Riddles in the Dark",
                                "Begin. And no cheating, precious."));

                els.add(textQ("rd-text-1",
                                "Thirty white horses on a red hill. First they champ, then they stamp, then they stand still. What are they?",
                                "teeth", List.of("teeth and gums"),
                                150, Difficulty.MEDIUM));

                els.add(textQ("rd-text-2",
                                "Voiceless it cries, wingless flutters, toothless bites, mouthless mutters. What is it?",
                                "wind", List.of("the wind"),
                                200, Difficulty.HARD));

                els.add(textQ("rd-text-3",
                                "This thing all things devours: birds, beasts, trees, flowers; gnaws iron, bites steel; grinds hard stones to meal. What is it?",
                                "time", List.of(),
                                250, Difficulty.HARD));

                els.add(endSlide("rd-s-end", "Gollum, gollum.",
                                "Lost, lost! My precious is lost!"));
                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildShireFolkDeck(User user) {
                Deck deck = baseDeck(
                                "Shire Folk — " + user.getUserName(),
                                "An easy starter deck. Hobbits, pipe-weed, and the Green Dragon.",
                                List.of("lotr", "shire", "beginner"),
                                "shire-folk");
                deck.setEstimatedDurationMinutes(5);

                List<DeckElement> els = new ArrayList<>();
                els.add(titleSlide("sf-s-1", "Shire Folk",
                                "An introduction to the gentle countryside."));

                els.add(mcq("sf-mcq-1", "What is the name of Bilbo Baggins's home?",
                                List.of("Brandy Hall", "Bag End", "The Prancing Pony", "Crickhollow"), 1,
                                100, Difficulty.EASY));

                List<McqOption> pipes = List.of(
                                new McqOption("pipe-leaf", "Old Toby (pipe-weed)",
                                                Image.external("https://picsum.photos/seed/lotr-pipe-toby/400/300"),
                                                null),
                                new McqOption("pipe-mug", "A mug of ale at the Green Dragon",
                                                Image.external("https://picsum.photos/seed/lotr-green-dragon/400/300"),
                                                null),
                                new McqOption("pipe-mathom", "A mathom shelved in the Mathom-house",
                                                Image.external("https://picsum.photos/seed/lotr-mathom/400/300"), null),
                                new McqOption("pipe-pony", "A Brandywine pony",
                                                Image.external("https://picsum.photos/seed/lotr-pony/400/300"), null));
                els.add(new McqQuestion("sf-img-1",
                                "Which of these would you find Gandalf enjoying outside Bag End?",
                                pipes, Set.of("pipe-leaf"),
                                150, Difficulty.EASY,
                                "Old Toby is from the Southfarthing — Gandalf's favorite.",
                                true, false, 0,
                                seedChrome("sf-img-1",
                                                "Which of these would you find Gandalf enjoying outside Bag End?",
                                                true, false, 20)));

                els.add(new NumberQuestion("sf-num-1",
                                "What birthday was Bilbo celebrating when he disappeared at his party?",
                                111.0, 0.0, " years", 0,
                                150, Difficulty.EASY,
                                "His eleventy-first birthday.",
                                null, null, true,
                                seedChrome("sf-num-1",
                                                "What birthday was Bilbo celebrating when he disappeared at his party?",
                                                true, false, 15)));

                els.add(endSlide("sf-s-end", "Don't keep them waiting.",
                                "It's a dangerous business, going out your door."));
                deck.getContent().setElements(els);
                return deck;
        }

        // ---------- builders + helpers ----------------------------------------

        private static Deck baseDeck(String name, String description, List<String> tags, String seedSlug) {
                Deck deck = new Deck();
                deck.setId(UUID.randomUUID().toString());
                deck.getContent().setName(name);
                deck.getContent().setDescription(description);
                deck.setTags(new java.util.LinkedHashSet<>(tags));
                // Every LOTR-themed starter is fan-trivia — bucket under Pop Culture.
                deck.setSubjectTagId("pop-culture");
                deck.setTagIds(new java.util.LinkedHashSet<>(List.of("pop-culture", "trivia")));
                deck.setVisibility(DeckVisibility.PRIVATE);
                deck.getContent().setFormat(SessionFormat.GAME);
                deck.getContent().setCover(Image.external("https://picsum.photos/seed/" + seedSlug + "/480/280"));
                deck.getContent().setBackground(Image.external("https://picsum.photos/seed/" + seedSlug + "-bg/1600/1000"));
                // Sample LOTR decks ship as PUBLISHED with CC_BY so the Explore feed
                // has something to render against a fresh DB.
                deck.setPublishStatus(PublishStatus.PUBLISHED);
                deck.setPublishedAt(Instant.now());
                deck.setLanguage("en");
                deck.setDifficulty(Difficulty.MEDIUM);
                deck.setLicense(License.CC_BY);
                return deck;
        }

        private static String pub(String id) {
                return "pub_" + id;
        }

        private static String prv(String id) {
                return "prv_" + id;
        }

        // Shared-metadata block (chunk 10b) appended to every seeded element.
        // Seeds carry null user ids (system credit), null timestamps (the document
        // load path stamps them on first save), empty tagIds, reactions on, v1.
        private static final String SEED_USER = null;
        private static final Instant SEED_TIME = null;
        private static final List<String> SEED_TAGS = List.of();
        private static final String SEED_CAPTION = null;
        private static final String SEED_ALT = null;
        private static final boolean SEED_REACTIONS = true;
        private static final Integer SEED_VERSION = 1;

        /**
         * Wrap a non-empty seed body string into the canonical single-BodyBlock
         * list. Mirrors the runtime migration shape so seed-loaded decks match
         * what SlideBlocksMigrationRunner would produce for legacy content.
         */
        private static List<SlideBlock> seedBlocks(String id, String body) {
                if (body == null || body.isBlank())
                        return List.of();
                return List.of(new BodyBlock(id + "-block-1", body));
        }

        /**
         * Build an {@link ElementChrome} populated with the seeder's standard
         * defaults. Callers pass the small set of fields that vary by element
         * (title, scored/survey, displaySeconds, best-answer toggles); everything
         * else (response mode, media slots, audit fields, version) lands on the
         * shared seed constants. Eliminates the ~14-arg "chrome tail" that every
         * element constructor used to repeat.
         */
        private static ElementChrome seedChrome(String id, String title,
                        boolean scored, boolean survey,
                        Integer multipleSelections,
                        int displaySeconds, String speakerNotes,
                        boolean bestAnswerMode, String bestAnswerTitle,
                        int bestAnswerPoints, BestAnswerScoring bestAnswerScoring) {
                return new ElementChrome(
                                pub(id), prv(id), title, null, null,
                                scored, survey, multipleSelections, ResponseMode.ACCEPTING_RESPONSES,
                                displaySeconds, speakerNotes,
                                null, null, null, null, null, null, MediaPosition.NONE,
                                bestAnswerMode, bestAnswerTitle, bestAnswerPoints, bestAnswerScoring,
                                SEED_USER, SEED_USER, SEED_TIME, SEED_TIME, SEED_TAGS,
                                SEED_CAPTION, SEED_ALT, SEED_REACTIONS, SEED_VERSION);
        }

        /** Shorthand for the common "no best-answer, single-select" case. */
        private static ElementChrome seedChrome(String id, String title, boolean scored, boolean survey,
                        int displaySeconds) {
                return seedChrome(id, title, scored, survey, null, displaySeconds, null,
                                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE);
        }

        private static Slide titleSlide(String id, String title, String body) {
                return new Slide(id, SlideKind.TITLE, body, seedBlocks(id, body),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.INSTRUCTIONS_BAR, true, false, ShowResponsesMode.INSTANT,
                                null, null,
                                null,
                                seedChrome(id, title, false, false, 6));
        }

        private static Slide endSlide(String id, String title, String body) {
                return new Slide(id, SlideKind.END, body, seedBlocks(id, body),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.INSTRUCTIONS_BAR, true, false, ShowResponsesMode.INSTANT,
                                null, null,
                                null,
                                seedChrome(id, title, false, false, 8));
        }

        private static McqQuestion mcq(String id, String prompt, List<String> options, int correctIndex,
                        int pointValue, Difficulty difficulty) {
                List<McqOption> opts = new ArrayList<>();
                for (int i = 0; i < options.size(); i++) {
                        opts.add(new McqOption(id + "-opt-" + i, options.get(i), null, null));
                }
                return new McqQuestion(id, prompt, opts, Set.of(opts.get(correctIndex).id()),
                                pointValue, difficulty, null,
                                true, false, 0,
                                seedChrome(id, prompt, true, false, 15));
        }

        private static TextQuestion textQ(String id, String prompt, String correct,
                        List<String> variants, int pointValue, Difficulty difficulty) {
                return new TextQuestion(id, prompt, correct, variants, false,
                                pointValue, difficulty, null,
                                80, true, false, 1,
                                seedChrome(id, prompt, true, false, 20));
        }

        // ---------- system decks (preserved from the old startup seeder) ------

        private static Deck buildWelcomeTourDeck() {
                Deck deck = new Deck();
                deck.setId(WELCOME_TOUR_ID);
                deck.getContent().setName("BrainFlex Welcome Tour");
                deck.getContent().setDescription(
                                "A quick tour through every kind of element you can put in a deck. Every type, one round each.");
                deck.setTags(new java.util.LinkedHashSet<>(List.of("welcome", "tour", "every-type")));
                deck.setSubjectTagId("general-knowledge");
                deck.setTagIds(new java.util.LinkedHashSet<>(List.of("general-knowledge", "trivia")));
                deck.setSystem(true);
                deck.setVisibility(DeckVisibility.PUBLIC);
                deck.getContent().setFormat(SessionFormat.GAME);
                deck.getContent().setCover(Image.external("https://picsum.photos/seed/brainflex-welcome-tour/480/280"));
                deck.getContent().setBackground(Image.external("https://picsum.photos/seed/brainflex-welcome-tour-bg/1600/1000"));
                deck.setEstimatedDurationMinutes(8);
                deck.setPublishStatus(PublishStatus.PUBLISHED);
                deck.setPublishedAt(Instant.now());
                deck.setLanguage("en");
                deck.setDifficulty(Difficulty.EASY);
                deck.setLicense(License.CC_BY);

                InteractiveSessionSettings defaults = new InteractiveSessionSettings();
                defaults.setTotalRounds(16);
                defaults.setScoringEnabled(true);
                defaults.setSpeedBonus(true);
                deck.getContent().setSettings(defaults);

                List<DeckElement> els = new ArrayList<>();

                els.add(new Slide("wt-s-1", SlideKind.TITLE,
                                "A quick tour through every kind of element a deck can contain. Press the screen to begin.",
                                seedBlocks("wt-s-1",
                                                "A quick tour through every kind of element a deck can contain. Press the screen to begin."),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.QR_CODE, true, true, ShowResponsesMode.INSTANT,
                                "Join the tour",
                                null,
                                null,
                                seedChrome("wt-s-1", "Welcome to BrainFlex", false, false, 6)));

                els.add(new Slide("wt-s-2", SlideKind.SECTION,
                                "Multiple choice, then free-text, then a number guess.",
                                seedBlocks("wt-s-2", "Multiple choice, then free-text, then a number guess."),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.INSTRUCTIONS_BAR, false, false, ShowResponsesMode.INSTANT,
                                null, null,
                                null,
                                seedChrome("wt-s-2", "Trivia round", false, false, 4)));

                List<McqOption> mcqOpts = List.of(
                                new McqOption("mars-opt-1", "Venus", null, null),
                                new McqOption("mars-opt-2", "Jupiter", null, null),
                                new McqOption("mars-opt-3", "Mars", null, null),
                                new McqOption("mars-opt-4", "Saturn", null, null));
                els.add(new McqQuestion("wt-mcq-1",
                                "Which planet is known as the Red Planet?",
                                mcqOpts, Set.of("mars-opt-3"),
                                100, Difficulty.EASY,
                                "Mars looks red because of iron oxide (rust) on its surface.",
                                true, false, 0,
                                seedChrome("wt-mcq-1", "Which planet is known as the Red Planet?",
                                                true, false, 15)));

                els.add(new TextQuestion("wt-text-1",
                                "What is the capital of France?",
                                "Paris", List.of("paree"), false,
                                150, Difficulty.EASY,
                                "Paris has been France's capital since 987 AD.",
                                80, true, false, 1,
                                seedChrome("wt-text-1", "What is the capital of France?",
                                                true, false, 15)));

                els.add(new NumberQuestion("wt-num-1",
                                "How many planets are in our solar system?",
                                8.0, 0.0, " planets", 0,
                                150, Difficulty.EASY,
                                "Pluto was reclassified as a dwarf planet in 2006.",
                                null, null, true,
                                seedChrome("wt-num-1", "How many planets are in our solar system?",
                                                true, false, 15)));

                els.add(new Slide("wt-s-3", SlideKind.SECTION,
                                "Drag to reorder, then rate some statements.",
                                seedBlocks("wt-s-3", "Drag to reorder, then rate some statements."),
                                ResultsDisplayType.BAR_VERTICAL, true, 3, true,
                                JoinType.INSTRUCTIONS_BAR, false, false, ShowResponsesMode.INSTANT,
                                null, null,
                                null,
                                seedChrome("wt-s-3", "Order and rate", false, false, 4)));

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
                                "Distance order from the Sun outward.",
                                true,
                                seedChrome("wt-rank-1", "Order these planets from closest to farthest from the Sun.",
                                                true, false, 20)));

                List<ScaleStatement> features = List.of(
                                new ScaleStatement("feat-realtime", "Real-time multiplayer gameplay"),
                                new ScaleStatement("feat-pulse", "Audience polling (Pulse)"),
                                new ScaleStatement("feat-slides", "Slides + media in decks"),
                                new ScaleStatement("feat-bestanswer", "Best Answer voting"));
                els.add(new ScalesQuestion("wt-scales-1",
                                "How excited are you about each of these features?",
                                features, 1, 5, "Meh", "Hyped", List.of(),
                                0, Difficulty.EASY, null,
                                seedChrome("wt-scales-1", "How excited are you about each of these features?",
                                                false, true, 25)));

                els.add(new Slide("wt-s-4", SlideKind.SECTION,
                                "Vote on the funniest answer, then ask anything.",
                                seedBlocks("wt-s-4", "Vote on the funniest answer, then ask anything."),
                                ResultsDisplayType.PIE_CHART, false, 1, true,
                                JoinType.INSTRUCTIONS_BAR, false, false, ShowResponsesMode.ON_CLICK,
                                null, null,
                                null,
                                seedChrome("wt-s-4", "Audience interaction", false, false, 4)));

                els.add(new TextQuestion("wt-best-1",
                                "If our next deck had a one-word theme, what would it be?",
                                "open",
                                List.of(), false,
                                0, Difficulty.EASY,
                                "Best Answer mode — players vote on the most creative response.",
                                80, true, false, 1,
                                seedChrome("wt-best-1", "If our next deck had a one-word theme, what would it be?",
                                                false, true, null, 30, null,
                                                true, "Which one-word theme is the most creative?", 100,
                                                BestAnswerScoring.POINTS_PER_VOTE)));

                els.add(new QAndAQuestion("wt-qanda-1",
                                "Ask the host anything about how BrainFlex works.",
                                3, true, false,
                                0, Difficulty.EASY, null,
                                false, 0,
                                seedChrome("wt-qanda-1", "Ask the host anything about how BrainFlex works.",
                                                false, true, null, 45,
                                                "Audience asks freely; you pin the ones you want to address.",
                                                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE)));

                els.add(new Slide("wt-s-5", SlideKind.SECTION,
                                "Tap cells, place a pin, pick an image.",
                                seedBlocks("wt-s-5", "Tap cells, place a pin, pick an image."),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.INSTRUCTIONS_BAR, false, false, ShowResponsesMode.INSTANT,
                                null, null,
                                null,
                                seedChrome("wt-s-5", "Visual round", false, false, 4)));

                els.add(new GridQuestion("wt-grid-1",
                                "Select all the prime numbers.",
                                3, 3,
                                new GridCellsConfig(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), null),
                                Set.of(1, 2, 4, 6),
                                true,
                                200, Difficulty.MEDIUM,
                                "Primes: 2, 3, 5, 7.",
                                seedChrome("wt-grid-1", "Select all the prime numbers.",
                                                true, false, 4, 20, null,
                                                false, null, 0, BestAnswerScoring.POINTS_PER_VOTE)));

                els.add(new PlaceOnImageQuestion("wt-place-1",
                                "Click roughly where Italy would be on this map.",
                                Image.external("https://picsum.photos/seed/brainflex-welcome-map/1200/800"),
                                0.55, 0.42, 0.08,
                                PlaceScoring.LINEAR,
                                200, Difficulty.MEDIUM,
                                "Lorem Picsum stands in for a real map until we wire one up.",
                                seedChrome("wt-place-1", "Click roughly where Italy would be on this map.",
                                                true, false, 25)));

                List<McqOption> landmarks = List.of(
                                new McqOption("lm-eiffel", "Eiffel Tower",
                                                Image.external("https://picsum.photos/seed/landmark-eiffel/400/300"),
                                                null),
                                new McqOption("lm-pisa", "Leaning Tower of Pisa",
                                                Image.external("https://picsum.photos/seed/landmark-pisa/400/300"),
                                                null),
                                new McqOption("lm-bigben", "Big Ben",
                                                Image.external("https://picsum.photos/seed/landmark-bigben/400/300"),
                                                null),
                                new McqOption("lm-statue", "Statue of Liberty",
                                                Image.external("https://picsum.photos/seed/landmark-statue/400/300"),
                                                null));
                els.add(new McqQuestion("wt-img-1",
                                "Which of these is the Eiffel Tower?",
                                landmarks, Set.of("lm-eiffel"),
                                150, Difficulty.EASY,
                                "MCQ option carries images.",
                                true, false, 0,
                                seedChrome("wt-img-1", "Which of these is the Eiffel Tower?",
                                                true, false, 20)));

                els.add(new Slide("wt-s-end", SlideKind.END,
                                "That's every element type. Now go build your own deck.",
                                seedBlocks("wt-s-end", "That's every element type. Now go build your own deck."),
                                ResultsDisplayType.DEFAULT, false, 1, false,
                                JoinType.INSTRUCTIONS_BAR, false, false, ShowResponsesMode.PRIVATE,
                                null, null,
                                null,
                                seedChrome("wt-s-end", "Thanks for playing!", false, false, 8)));

                deck.getContent().setElements(els);
                return deck;
        }

        private static Deck buildGeneralKnowledgeDeck() {
                Deck deck = new Deck();
                deck.setId(GENERAL_KNOWLEDGE_ID);
                deck.getContent().setName("General Knowledge");
                deck.getContent().setDescription("A mix of geography, history, science, and pop culture. MCQ + text-input only.");
                deck.setTags(new java.util.LinkedHashSet<>(List.of("general", "trivia")));
                deck.setSubjectTagId("general-knowledge");
                deck.setTagIds(new java.util.LinkedHashSet<>(List.of("general-knowledge", "trivia")));
                deck.setSystem(true);
                deck.setVisibility(DeckVisibility.PUBLIC);
                deck.getContent().setFormat(SessionFormat.GAME);
                deck.getContent().setCover(Image.external("https://picsum.photos/seed/brainflex-general-knowledge/480/280"));
                deck.getContent().setBackground(
                                Image.external("https://picsum.photos/seed/brainflex-general-knowledge-bg/1600/1000"));
                deck.setEstimatedDurationMinutes(6);
                deck.setPublishStatus(PublishStatus.PUBLISHED);
                deck.setPublishedAt(Instant.now());
                deck.setLanguage("en");
                deck.setDifficulty(Difficulty.MEDIUM);
                deck.setLicense(License.CC_BY);

                InteractiveSessionSettings defaults = new InteractiveSessionSettings();
                defaults.setTotalRounds(8);
                deck.getContent().setSettings(defaults);

                List<DeckElement> els = new ArrayList<>();
                els.add(mcq("gk-1", "What is the capital city of Australia?",
                                List.of("Sydney", "Melbourne", "Canberra", "Brisbane"), 2, 100, Difficulty.EASY));
                els.add(mcq("gk-2", "Who painted the Mona Lisa?",
                                List.of("Michelangelo", "Raphael", "Leonardo da Vinci", "Donatello"), 2, 100,
                                Difficulty.EASY));
                els.add(mcq("gk-3", "What language has the most native speakers worldwide?",
                                List.of("English", "Spanish", "Hindi", "Mandarin Chinese"), 3, 200, Difficulty.MEDIUM));
                els.add(mcq("gk-4", "In what year did World War II end?",
                                List.of("1943", "1944", "1945", "1946"), 2, 200, Difficulty.MEDIUM));
                els.add(mcq("gk-5", "Which element has the chemical symbol 'Au'?",
                                List.of("Silver", "Copper", "Aluminum", "Gold"), 3, 200, Difficulty.MEDIUM));
                els.add(new TextQuestion("gk-6",
                                "What is the capital of France?",
                                "Paris", List.of("paree"), false,
                                150, Difficulty.EASY, null,
                                80, true, false, 1,
                                seedChrome("gk-6", "What is the capital of France?", true, false, 15)));
                els.add(new TextQuestion("gk-7",
                                "Who wrote the play 'Hamlet'?",
                                "Shakespeare", List.of("William Shakespeare"), false,
                                200, Difficulty.MEDIUM, null,
                                80, true, false, 1,
                                seedChrome("gk-7", "Who wrote the play 'Hamlet'?", true, false, 20)));
                els.add(new NumberQuestion("gk-8",
                                "How many planets are in our solar system?",
                                8.0, 0.0, " planets", 0,
                                150, Difficulty.EASY, null,
                                null, null, true,
                                seedChrome("gk-8", "How many planets are in our solar system?", true, false, 15)));

                deck.getContent().setElements(els);
                return deck;
        }
}
