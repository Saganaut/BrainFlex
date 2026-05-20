/**
 * Team podium for the final results page (chunk 12).
 *
 * Groups placements by teamId, sums finalScore for each team to derive the
 * team standing, and crowns a per-team MVP (the highest-scoring individual
 * on that team). Team metadata (name + color) is sourced from the
 * authoritative team list on the session DTO; placement.teamId joins the
 * two.
 *
 * Rendered ABOVE the existing individual podium on GameOver. If team mode
 * was disabled for the session — or no placements have a teamId — the
 * component returns null so individual mode is unaffected.
 */
import styles from "./TeamPodium.module.css";
import type { PlayerPlacement, Team } from "../../../store/BrainFlexApi";

interface TeamPodiumProps {
  placements: PlayerPlacement[];
  teams: Team[];
  currentUserId?: string;
}

interface TeamStanding {
  team: Team;
  totalScore: number;
  members: PlayerPlacement[];
  mvp: PlayerPlacement | null;
}

const PLACE_LABELS = ["1st", "2nd", "3rd"];

const TeamPodium = ({
  placements,
  teams,
  currentUserId,
}: TeamPodiumProps) => {
  const teamById = new Map(teams.map((t) => [t.id ?? "", t]));
  const byTeam = new Map<string, PlayerPlacement[]>();
  for (const p of placements) {
    if (!p.teamId) continue;
    const list = byTeam.get(p.teamId) ?? [];
    list.push(p);
    byTeam.set(p.teamId, list);
  }
  if (byTeam.size === 0) return null;

  const standings: TeamStanding[] = [];
  for (const [teamId, members] of byTeam.entries()) {
    const team = teamById.get(teamId);
    if (!team) continue;
    const totalScore = members.reduce(
      (acc, m) => acc + (m.finalScore ?? 0),
      0,
    );
    const mvp = members.reduce<PlayerPlacement | null>(
      (best, m) =>
        !best || (m.finalScore ?? 0) > (best.finalScore ?? 0) ? m : best,
      null,
    );
    standings.push({ team, totalScore, members, mvp });
  }
  standings.sort((a, b) => b.totalScore - a.totalScore);
  const top3 = standings.slice(0, 3);

  return (
    <div className={styles.wrap}>
      <h2 className={styles.heading}>Team Results</h2>
      <div className={styles.podium}>
        {top3.map((s, i) => {
          const containsMe =
            !!currentUserId && s.members.some((m) => m.userId === currentUserId);
          return (
            <div
              key={s.team.id}
              className={`${styles.place} ${styles[`place${i + 1}`]} ${containsMe ? styles.me : ""}`}
              style={
                {
                  "--team-color": s.team.color ?? "var(--bg-subtle)",
                } as React.CSSProperties
              }>
              <span className={styles.placeLabel}>{PLACE_LABELS[i]}</span>
              <span className={styles.teamName}>
                <span className={styles.dot} aria-hidden='true' />
                {s.team.name}
              </span>
              <span className={styles.placeScore}>{s.totalScore} pts</span>
              {s.mvp && (
                <span className={styles.mvp}>
                  MVP: {s.mvp.userName}
                  {s.mvp.finalScore != null && ` · ${s.mvp.finalScore}`}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export { TeamPodium };
