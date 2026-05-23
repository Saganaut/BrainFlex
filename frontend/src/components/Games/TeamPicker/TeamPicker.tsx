/**
 * Lobby team picker (chunk 12).
 *
 * Renders a grid of team cards when {@code session.settings.teamMode} is on. Each
 * card shows the team's color, name, member list, and member count. Players
 * tap a card to move themselves via useMovePlayerToTeamMutation; the host
 * gets inline create / rename / recolor / delete controls plus an
 * "Auto-assign me" button when autoBalanceTeams is enabled and the player
 * doesn't yet have a team.
 *
 * The host can also move any member to a different team via a per-row
 * "Move to…" select on the member list — a native select keeps the
 * positioning / outside-click / a11y semantics for free, which matters
 * because the action is low-frequency and we don't want a bespoke menu
 * surface here.
 *
 * Team membership is sourced from the slice (which is fed by both the
 * authoritative session DTO and STOMP /teams broadcasts), so concurrent
 * joins from other players reflect live without polling.
 */
import { useState } from "react";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input/Input";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import {
  useCreateTeamMutation,
  useUpdateTeamMutation,
  useDeleteTeamMutation,
  useMovePlayerToTeamMutation,
  type Team,
  type InteractiveSessionPlayerResponse,
} from "../../../store/BrainFlexApi";
import styles from "./TeamPicker.module.css";

interface TeamPickerProps {
  roomCode: string;
  teams: Team[];
  players: InteractiveSessionPlayerResponse[];
  // Session-scoped playerId of the viewer.
  currentPlayerId?: string;
  isHost: boolean;
  autoBalanceTeams: boolean;
}

/**
 * Curated swatches the host can pick from when creating or recoloring a
 * team. Kept short on purpose — eight is enough to differentiate, more
 * would require a full color picker UI.
 */
const TEAM_COLOR_PRESETS = [
  "#e11d48",
  "#f97316",
  "#facc15",
  "#22c55e",
  "#14b8a6",
  "#3b82f6",
  "#8b5cf6",
  "#ec4899",
] as const;

const TeamPicker = ({
  roomCode,
  teams,
  players,
  currentPlayerId,
  isHost,
  autoBalanceTeams,
}: TeamPickerProps) => {
  const [movePlayerToTeam] = useMovePlayerToTeamMutation();
  const [createTeam, { isLoading: creating }] = useCreateTeamMutation();
  const confirm = useConfirm();

  // The current player's team membership, used to highlight the active card
  // and to gate the "Auto-assign me" affordance.
  const myTeamId = currentPlayerId
    ? players.find((p) => p.playerId === currentPlayerId)?.teamId
    : undefined;

  const handleJoin = (teamId: string | undefined) => {
    if (!currentPlayerId || !teamId || teamId === myTeamId) return;
    void movePlayerToTeam({
      roomCode,
      playerId: currentPlayerId,
      teamMoveRequest: { teamId },
    });
  };

  const handleAutoAssign = () => {
    if (!currentPlayerId) return;
    // Sentinel value the backend treats as "auto-balance me into the
    // smallest team" — mirrors the lobby join path's autoBalance branch.
    void movePlayerToTeam({
      roomCode,
      playerId: currentPlayerId,
      teamMoveRequest: { teamId: "__AUTO__" },
    });
  };

  const handleCreate = () => {
    const nextColor =
      TEAM_COLOR_PRESETS[teams.length % TEAM_COLOR_PRESETS.length];
    void createTeam({
      roomCode,
      teamCrudRequest: {
        name: `Team ${teams.length + 1}`,
        color: nextColor,
      },
    });
  };

  return (
    <div className={styles.wrap}>
      <div className={styles.headerRow}>
        <h2 className={styles.heading}>Teams ({teams.length})</h2>
        {isHost && (
          <Btn
            size='sm'
            type='button'
            onClick={handleCreate}
            disabled={creating}>
            + Add team
          </Btn>
        )}
        {!myTeamId && autoBalanceTeams && (
          <Btn size='sm' type='button' onClick={handleAutoAssign}>
            Auto-assign me
          </Btn>
        )}
      </div>
      <div className={styles.grid}>
        {teams.map((team) => (
          <TeamCard
            key={team.id}
            team={team}
            allTeams={teams}
            members={players.filter((p) => p.teamId === team.id)}
            isMyTeam={!!myTeamId && myTeamId === team.id}
            isHost={isHost}
            onJoin={() => {
              handleJoin(team.id);
            }}
            onMoveMember={(targetPlayerId, targetTeamId) => {
              void movePlayerToTeam({
                roomCode,
                playerId: targetPlayerId,
                teamMoveRequest: { teamId: targetTeamId },
              });
            }}
            onDeleteConfirm={confirm}
            roomCode={roomCode}
          />
        ))}
        {teams.length === 0 && (
          <p className={styles.empty}>
            {isHost
              ? "No teams yet. Add one to get started."
              : "Waiting for the host to set up teams…"}
          </p>
        )}
      </div>
    </div>
  );
};

interface TeamCardProps {
  team: Team;
  allTeams: Team[];
  members: InteractiveSessionPlayerResponse[];
  isMyTeam: boolean;
  isHost: boolean;
  onJoin: () => void;
  // Receives the session-scoped playerId of the target.
  onMoveMember: (playerId: string, teamId: string) => void;
  onDeleteConfirm: ReturnType<typeof useConfirm>;
  roomCode: string;
}

const TeamCard = ({
  team,
  allTeams,
  members,
  isMyTeam,
  isHost,
  onJoin,
  onMoveMember,
  onDeleteConfirm,
  roomCode,
}: TeamCardProps) => {
  const [editing, setEditing] = useState(false);
  const [draftName, setDraftName] = useState(team.name ?? "");
  const [draftColor, setDraftColor] = useState(
    team.color ?? TEAM_COLOR_PRESETS[0],
  );
  const [updateTeam, { isLoading: updating }] = useUpdateTeamMutation();
  const [deleteTeam, { isLoading: deleting }] = useDeleteTeamMutation();

  const startEdit = () => {
    setDraftName(team.name ?? "");
    setDraftColor(team.color ?? TEAM_COLOR_PRESETS[0]);
    setEditing(true);
  };

  const saveEdit = () => {
    if (!team.id) return;
    void updateTeam({
      roomCode,
      teamId: team.id,
      teamCrudRequest: {
        name: draftName.trim() || team.name,
        color: draftColor,
      },
    });
    setEditing(false);
  };

  const handleDelete = async () => {
    if (!team.id) return;
    const ok = await onDeleteConfirm({
      title: "Delete team",
      message: `Delete "${team.name ?? "this team"}"? Members will be reassigned to other teams.`,
      confirmLabel: "Delete",
      variant: "danger",
    });
    if (!ok) return;
    void deleteTeam({ roomCode, teamId: team.id });
  };

  const cardStyle: React.CSSProperties = {
    "--team-color": team.color ?? "var(--bg-subtle)",
  } as React.CSSProperties;

  return (
    <div
      className={`${styles.card} ${isMyTeam ? styles.cardMine : ""}`}
      style={cardStyle}>
      <div className={styles.cardHeader}>
        <span className={styles.colorDot} aria-hidden='true' />
        {editing && isHost ? (
          <Input
            type='text'
            value={draftName}
            maxLength={32}
            autoFocus
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setDraftName(e.target.value);
            }}
            className={styles.editInput}
          />
        ) : (
          <span className={styles.cardName}>{team.name}</span>
        )}
        <span className={styles.cardCount}>{members.length}</span>
      </div>

      {editing && isHost && (
        <div className={styles.swatches} role='group' aria-label='Team color'>
          {TEAM_COLOR_PRESETS.map((c) => (
            <button
              key={c}
              type='button'
              className={`${styles.swatch} ${draftColor === c ? styles.swatchSelected : ""}`}
              style={{ background: c }}
              aria-label={`Color ${c}`}
              onClick={() => {
                setDraftColor(c);
              }}
            />
          ))}
        </div>
      )}

      {members.length > 0 ? (
        <ul className={styles.memberList}>
          {members.map((m) => (
            <li key={m.playerId} className={styles.member}>
              <span className={styles.memberName}>{m.user.name ?? "?"}</span>
              {isHost && m.playerId && allTeams.length > 1 && (
                <select
                  className={styles.moveSelect}
                  value=''
                  aria-label={`Move ${m.user.name ?? "player"} to another team`}
                  onChange={(e) => {
                    const targetTeamId = e.target.value;
                    if (
                      !targetTeamId ||
                      targetTeamId === team.id ||
                      !m.playerId
                    )
                      return;
                    onMoveMember(m.playerId, targetTeamId);
                    // Reset to placeholder so the same destination can be
                    // picked again for the next member without re-renders
                    // freezing the bound value.
                    e.target.value = "";
                  }}>
                  <option value=''>Move to…</option>
                  {allTeams
                    .filter((t) => t.id && t.id !== team.id)
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                </select>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <p className={styles.memberEmpty}>No members yet.</p>
      )}

      <div className={styles.cardActions}>
        {!editing && !isMyTeam && (
          <Btn size='sm' type='button' onClick={onJoin}>
            Join
          </Btn>
        )}
        {!editing && isMyTeam && <span className={styles.youTag}>You</span>}
        {isHost && !editing && (
          <Btn size='sm' type='button' variant='secondary' onClick={startEdit}>
            Edit
          </Btn>
        )}
        {isHost && !editing && (
          <Btn
            size='sm'
            type='button'
            variant='error'
            disabled={deleting}
            onClick={() => {
              void handleDelete();
            }}>
            Delete
          </Btn>
        )}
        {editing && (
          <>
            <Btn size='sm' type='button' disabled={updating} onClick={saveEdit}>
              Save
            </Btn>
            <Btn
              size='sm'
              type='button'
              variant='secondary'
              onClick={() => {
                setEditing(false);
              }}>
              Cancel
            </Btn>
          </>
        )}
      </div>
    </div>
  );
};

export { TeamPicker };
