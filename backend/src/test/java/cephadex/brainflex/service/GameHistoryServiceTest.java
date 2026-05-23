/**
 * Unit tests for {@link GameHistoryService}.
 *
 * The repository, DeckRepository, and UserRepository are mocked. The behaviors
 * worth pinning down are:
 *   - one row per player; an extra host-only row only when the host did not
 *     play, with {@code wasHost=true} either way
 *   - duplicate-key on (userId, sessionId) is swallowed and the host
 *     monthly counter is not bumped on a replay
 *   - the host monthly counter rolls over on a calendar-month boundary and
 *     refuses to bump for guest hosts
 */
package cephadex.brainflex.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import cephadex.brainflex.model.deck.Deck;
import cephadex.brainflex.model.session.GameHistoryEntry;
import cephadex.brainflex.model.session.InteractiveSession;
import cephadex.brainflex.model.session.InteractiveSessionPlayer;
import cephadex.brainflex.model.org.Membership;
import cephadex.brainflex.model.session.PlayerPlacement;
import cephadex.brainflex.model.org.Team;
import cephadex.brainflex.model.user.User;
import cephadex.brainflex.model.shared.UserSnapshot;
import cephadex.brainflex.repository.DeckRepository;
import cephadex.brainflex.repository.GameHistoryRepository;
import cephadex.brainflex.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GameHistoryServiceTest {

    @Mock private GameHistoryRepository historyRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private UserRepository userRepository;
    @Mock private AchievementService achievementService;

    @InjectMocks private GameHistoryService gameHistoryService;

    @Test
    void recordFinish_WritesOneRowPerPlacement_AndTagsTheHostRow() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        // Host is also a player — so we expect exactly two rows, one tagged
        // wasHost=true and one untagged.
        session.getPlayers().add(player("host-1", "Kevin", false));
        session.getPlayers().add(player("p-2", "Other", false));

        List<PlayerPlacement> placements = List.of(
                placement("host-1", "Kevin", 1, 50, false),
                placement("p-2", "Other", 2, 30, false));

        Deck deck = new Deck();
        deck.setId("deck-1");
        deck.getContent().setName("LOTR Trivia");
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        User host = registeredUser("host-1");
        when(userRepository.findById("host-1")).thenReturn(Optional.of(host));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, placements);

        ArgumentCaptor<GameHistoryEntry> captor = ArgumentCaptor.forClass(GameHistoryEntry.class);
        verify(historyRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<GameHistoryEntry> rows = captor.getAllValues();

        GameHistoryEntry hostRow = rows.stream().filter(r -> "host-1".equals(r.getUserId()))
                .findFirst().orElseThrow();
        GameHistoryEntry playerRow = rows.stream().filter(r -> "p-2".equals(r.getUserId()))
                .findFirst().orElseThrow();

        assertTrue(hostRow.isWasHost(), "host's row must carry wasHost=true");
        assertFalse(playerRow.isWasHost(), "non-host player must not be tagged");
        assertEquals("LOTR Trivia", hostRow.getDeckName(), "deckName denorm'd from Deck");
        assertEquals(50, hostRow.getFinalScore());
        assertEquals(1, hostRow.getPlacement());
        assertEquals("session-1", hostRow.getInteractiveSessionId());
    }

    @Test
    void recordFinish_HostDidNotPlay_WritesStandaloneHostRow() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        // No player matches host-1.
        session.getPlayers().add(player("p-2", "Other", false));
        List<PlayerPlacement> placements = List.of(placement("p-2", "Other", 1, 80, false));

        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());
        when(userRepository.findById("host-1")).thenReturn(Optional.of(registeredUser("host-1")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, placements);

        ArgumentCaptor<GameHistoryEntry> captor = ArgumentCaptor.forClass(GameHistoryEntry.class);
        verify(historyRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<GameHistoryEntry> rows = captor.getAllValues();

        GameHistoryEntry hostRow = rows.stream().filter(r -> "host-1".equals(r.getUserId()))
                .findFirst().orElseThrow();
        assertTrue(hostRow.isWasHost());
        assertEquals(0, hostRow.getFinalScore(), "standalone host row carries no score");
        assertEquals(0, hostRow.getPlacement());
    }

    @Test
    void recordFinish_DuplicateInsert_IsSwallowed_AndCounterNotBumped() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        session.getPlayers().add(player("host-1", "Kevin", false));
        List<PlayerPlacement> placements = List.of(placement("host-1", "Kevin", 1, 50, false));

        // Every insert collides with an existing row — the service must not
        // throw and must NOT bump the Membership counter.
        when(historyRepository.insert(any(GameHistoryEntry.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());

        gameHistoryService.recordFinish(session, placements);

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void recordFinish_NewMonth_ResetsCounterToOne() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        session.setEndedAt(Instant.parse("2026-06-01T09:00:00Z"));
        session.getPlayers().add(player("host-1", "Kevin", false));
        List<PlayerPlacement> placements = List.of(placement("host-1", "Kevin", 1, 50, false));

        User host = registeredUser("host-1");
        host.getMembership().setMonthlyInteractiveSessionCount(7);
        host.getMembership().setMonthlyCountPeriodStart(Instant.parse("2026-05-01T00:00:00Z"));
        when(userRepository.findById("host-1")).thenReturn(Optional.of(host));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, placements);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        Membership after = captor.getValue().getMembership();
        assertEquals(1, after.getMonthlyInteractiveSessionCount(), "new month resets to 1");
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), after.getMonthlyCountPeriodStart());
    }

    @Test
    void recordFinish_SameMonth_IncrementsCounter() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        session.setEndedAt(Instant.parse("2026-05-20T09:00:00Z"));
        session.getPlayers().add(player("host-1", "Kevin", false));
        List<PlayerPlacement> placements = List.of(placement("host-1", "Kevin", 1, 50, false));

        User host = registeredUser("host-1");
        host.getMembership().setMonthlyInteractiveSessionCount(3);
        host.getMembership().setMonthlyCountPeriodStart(Instant.parse("2026-05-01T00:00:00Z"));
        when(userRepository.findById("host-1")).thenReturn(Optional.of(host));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, placements);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getMembership().getMonthlyInteractiveSessionCount());
    }

    @Test
    void recordFinish_GuestHost_DoesNotBumpCounter() {
        InteractiveSession session = sessionWithHost("guest-h", "Anon");
        session.getPlayers().add(player("guest-h", "Anon", true));
        List<PlayerPlacement> placements = List.of(placement("guest-h", "Anon", 1, 0, true));

        User guest = new User();
        guest.setId("guest-h");
        guest.setGuest(true);
        guest.setMembership(new Membership());
        when(userRepository.findById("guest-h")).thenReturn(Optional.of(guest));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());

        gameHistoryService.recordFinish(session, placements);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void recordFinish_DeckDeleted_StillWritesRowsWithNullName() {
        InteractiveSession session = sessionWithHost("host-1", "Kevin");
        session.getPlayers().add(player("p-1", "Other", false));
        List<PlayerPlacement> placements = List.of(placement("p-1", "Other", 1, 30, false));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());
        when(userRepository.findById("host-1")).thenReturn(Optional.of(registeredUser("host-1")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, placements);

        ArgumentCaptor<GameHistoryEntry> captor = ArgumentCaptor.forClass(GameHistoryEntry.class);
        verify(historyRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        for (GameHistoryEntry row : captor.getAllValues()) {
            assertEquals(null, row.getDeckName(), "deck deletion → deckName null");
        }
    }

    @Test
    void recordFinish_TeamMode_DenormsTeamName() {
        // Same user is host + player so only one row is written, simplifying
        // the assertion.
        InteractiveSession session = sessionWithHost("p-1", "Other");
        Team team = new Team();
        team.setId("team-red");
        team.setName("Red Crew");
        session.setTeams(new ArrayList<>(List.of(team)));
        InteractiveSessionPlayer p = player("p-1", "Other", false);
        p.setTeamId("team-red");
        session.getPlayers().add(p);
        PlayerPlacement placement = placement("p-1", "Other", 1, 30, false);
        placement.setTeamId("team-red");

        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());
        when(userRepository.findById("p-1")).thenReturn(Optional.of(registeredUser("p-1")));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        gameHistoryService.recordFinish(session, List.of(placement));

        ArgumentCaptor<GameHistoryEntry> captor = ArgumentCaptor.forClass(GameHistoryEntry.class);
        verify(historyRepository).insert(captor.capture());
        GameHistoryEntry saved = captor.getValue();
        assertEquals("team-red", saved.getTeamId());
        assertEquals("Red Crew", saved.getTeamName());
    }

    @Test
    void listForUser_DelegatesToRepository() {
        org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<GameHistoryEntry> empty =
                new org.springframework.data.domain.PageImpl<>(List.of());
        when(historyRepository.findAllByUserId(eq("u-1"), eq(pageable))).thenReturn(empty);
        assertEquals(empty, gameHistoryService.listForUser("u-1", pageable));
    }

    // ---- fixtures ----

    private InteractiveSession sessionWithHost(String hostUserId, String hostName) {
        InteractiveSession s = new InteractiveSession();
        s.setId("session-1");
        s.setHostUserId(hostUserId);
        s.setHostName(hostName);
        s.setDeckId("deck-1");
        s.setPlayers(new ArrayList<>());
        s.setStartedAt(Instant.parse("2026-05-20T08:00:00Z"));
        s.setEndedAt(Instant.parse("2026-05-20T08:30:00Z"));
        return s;
    }

    private InteractiveSessionPlayer player(String userId, String userName, boolean guest) {
        InteractiveSessionPlayer p = new InteractiveSessionPlayer();
        p.setUser(UserSnapshot.of(userId, userName, null, guest));
        return p;
    }

    private PlayerPlacement placement(String userId, String userName, int placement,
            int finalScore, boolean guest) {
        PlayerPlacement p = new PlayerPlacement();
        p.setUser(UserSnapshot.of(userId, userName, null, guest));
        p.setPlacement(placement);
        p.setFinalScore(finalScore);
        p.setTotalQuestions(10);
        p.setCorrectAnswers(5);
        return p;
    }

    private User registeredUser(String id) {
        User u = new User();
        u.setId(id);
        u.setGuest(false);
        u.setMembership(new Membership());
        return u;
    }
}
