/**
 * Team-aggregated leaderboard (chunk 12).
 *
 * Rendered alongside ScoreBoard on PlayPage when teamMode is on. Reads the
 * authoritative {@code team.score} that the backend recomputes on every
 * answer and on Best Answer reveals — the slice keeps it in sync via
 * setSession on the lobby topic + teamUpdateReceived on the teams topic.
 *
 * Intentionally small: the per-player leaderboard (ScoreBoard) is the
 * primary view, and this is a complementary panel so the audience can see
 * how the teams stack up without flipping screens. Auto-alternating between
 * individual + team views is out of scope for this PR.
 */
import styles from "./TeamLeaderboard.module.css";
import type {
  InteractiveSessionPlayerDto,
  Team,
} from "../../../store/BrainFlexApi";

interface TeamLeaderboardProps {
  teams: Team[];
  players: InteractiveSessionPlayerDto[];
  // Session-scoped playerId of the viewer; their team gets the "me" highlight.
  currentPlayerId?: string;
  hideScores?: boolean;
}

const TeamLeaderboard = ({
  teams,
  players,
  currentPlayerId,
  hideScores,
}: TeamLeaderboardProps) => {
  if (teams.length === 0) return null;
  const myTeamId = currentPlayerId
    ? players.find((p) => p.playerId === currentPlayerId)?.teamId
    : undefined;
  const memberCountByTeam = new Map<string, number>();
  for (const p of players) {
    if (!p.teamId) continue;
    memberCountByTeam.set(p.teamId, (memberCountByTeam.get(p.teamId) ?? 0) + 1);
  }
  const sorted = hideScores
    ? teams
    : [...teams].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));

  return (
    <section className={styles.board} aria-label='Teams'>
      <h3 className={styles.title}>{hideScores ? "Teams" : "Teams"}</h3>
      <ol className={styles.list}>
        {sorted.map((team, i) => {
          const isMyTeam = !!myTeamId && team.id === myTeamId;
          const memberCount = memberCountByTeam.get(team.id ?? "") ?? 0;
          return (
            <li
              key={team.id}
              className={`${styles.row} ${isMyTeam ? styles.me : ""}`}
              style={
                {
                  "--team-color": team.color ?? "var(--bg-subtle)",
                } as React.CSSProperties
              }>
              {!hideScores && <span className={styles.rank}>{i + 1}</span>}
              <span className={styles.dot} aria-hidden='true' />
              <span className={styles.name}>{team.name}</span>
              <span className={styles.count}>{memberCount}</span>
              {!hideScores && (
                <span className={styles.score}>{team.score ?? 0}</span>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
};

export { TeamLeaderboard };
