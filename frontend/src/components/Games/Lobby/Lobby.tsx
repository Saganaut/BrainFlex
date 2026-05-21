/**
 * Pre-game waiting room — shows joined players, the shareable room code,
 * and (for the host) the Start Game button. Real-time player-list updates
 * arrive via /topic/interactive-session/{roomCode}/lobby; the WebSocket is managed here.
 */
import { useEffect, useMemo } from "react";
import { useNavigate } from "@tanstack/react-router";
import { useAppDispatch } from "../../../store/hooks";
import { setSession } from "../../../store/interactiveSessionSlice";
import {
  useGetInteractiveSessionQuery,
  useListAvatarsQuery,
  useUpdateMyAvatarMutation,
} from "../../../store/BrainFlexApi";
import { useInteractiveSession } from "../../../hooks/useInteractiveSession";
import { useInteractiveSessionWebSocket } from "../../../hooks/useInteractiveSessionWebSocket";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { WsErrorBanner } from "../WsErrorBanner/WsErrorBanner";
import { TeamPicker } from "../TeamPicker/TeamPicker";
import { resolveInteractiveSessionBackground } from "../../../utils/deckImages";
import { resolveAvatarSrc } from "../../../utils/avatarUrl";
import {
  AvatarSelector,
  type AvatarOption,
} from "@/components/Common/Input/AvatarSelector/AvatarSelector";
import styles from "./Lobby.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { useAppSelector } from "../../../store/hooks";

interface LobbyProps {
  roomCode: string;
}

const Lobby = ({ roomCode }: LobbyProps) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const userState = useCurrentUser();
  const game = useInteractiveSession();
  const { sendStart, sendLeave, sendBoot } = useInteractiveSessionWebSocket(roomCode);
  const confirm = useConfirm();

  const { data: session } = useGetInteractiveSessionQuery({ roomCode });

  useEffect(() => {
    if (session) dispatch(setSession(session));
  }, [session, dispatch]);

  useEffect(() => {
    if (game.status === "IN_PROGRESS") {
      void navigate({ to: "/games/$roomCode/play", params: { roomCode } });
    }
  }, [game.status, navigate, roomCode]);

  const userId =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user.id
      : undefined;
  const isHost = !!userId && session?.hostUserId === userId;
  const players = game.players;
  const myPlayer = useMemo(
    () => (userId ? players.find((p) => p.userId === userId) : undefined),
    [players, userId],
  );

  // Chunk 13 — lobby avatar picker. Hosts don't pick a preset (their
  // pictureUrl drives the lobby header), but every joined player can. The
  // picker stays hidden until the player has actually joined (myPlayer set).
  const showAvatarPicker = !isHost && !!myPlayer;
  const { data: avatarPresets } = useListAvatarsQuery(undefined, {
    skip: !showAvatarPicker,
  });
  const [updateMyAvatar] = useUpdateMyAvatarMutation();
  const avatarOptions = useMemo<AvatarOption[]>(
    () =>
      (avatarPresets ?? []).map((preset) => ({
        value: preset.key ?? "",
        label: preset.displayName ?? preset.key ?? "Avatar",
        src: preset.imageUrl ?? "",
      })),
    [avatarPresets],
  );
  const myAvatarKey = myPlayer?.avatarKey ?? "";
  // Team-mode lobby (chunk 12). The slice keeps teams in sync with both the
  // setSession refresh and STOMP /teams broadcasts, so we read from there
  // rather than session.teams to also catch live joins.
  const teams = useAppSelector((s) => s.interactiveSession.teams);
  const teamMode = useAppSelector((s) => s.interactiveSession.teamMode);
  const autoBalanceTeams = useAppSelector(
    (s) => s.interactiveSession.autoBalanceTeams,
  );

  // If the host boots us (or anything else removes us from the player list),
  // navigate home rather than stranding the user on an empty lobby.
  useEffect(() => {
    if (!userId || players.length === 0) return;
    const stillInLobby = players.some((p) => p.userId === userId);
    if (!stillInLobby) {
      void navigate({ to: "/" });
    }
  }, [userId, players, navigate]);

  const handleAvatarChange = (avatarKey: string) => {
    const preset = avatarPresets?.find((p) => p.key === avatarKey);
    void updateMyAvatar({
      roomCode,
      updatePlayerAvatarRequest: {
        avatarKey,
        colorTag: preset?.colorTag,
      },
    });
  };

  const handleBoot = async (targetUserId: string) => {
    const ok = await confirm({
      title: "Remove player",
      message: "Remove this player from the lobby?",
      confirmLabel: "Remove",
      variant: "danger",
    });
    if (!ok) return;
    sendBoot(targetUserId);
  };

  const backgroundUrl = resolveInteractiveSessionBackground(
    session?.settings?.deckBackgroundImageUrl,
    session?.deckId,
  );

  return (
    <section
      className={styles.lobby}
      aria-label='Lobby'
      style={
        { "--interactive-session-bg": `url(${backgroundUrl})` } as React.CSSProperties
      }>
      <header className={styles.header}>
        <h1 className={styles.title}>Lobby</h1>
        {(session?.hostName ?? session?.hostAvatarUrl) && (
          <div className={styles.hostStrip}>
            {session.hostAvatarUrl && (
              <img
                src={resolveAvatarSrc(session.hostAvatarUrl)}
                alt=''
                className={styles.hostAvatar}
              />
            )}
            <span className={styles.hostLine}>
              <span className={styles.hostLabel}>Hosted by</span>
              <span className={styles.hostName}>
                {session.hostName ?? "Host"}
              </span>
            </span>
          </div>
        )}
        <div className={styles.codeBox}>
          <span className={styles.codeLabel}>Room Code</span>
          {/* Chunk 13 — when the host set a customRoomCode, surface that as
              the canonical big-text value. The auto-generated 6-char roomCode
              is still the URL path (and falls back here if no custom code is
              set), so this is purely a display swap. */}
          <span className={styles.codeValue}>
            {session?.customRoomCode ?? roomCode}
          </span>
          <span className={styles.codeHint}>Share this with friends</span>
        </div>
      </header>

      {showAvatarPicker && avatarOptions.length > 0 && (
        <section
          className={styles.avatarPickerSection}
          aria-label='Choose your avatar'>
          <h2 className={styles.avatarPickerHeading}>Pick your avatar</h2>
          <AvatarSelector
            name='lobby-avatar'
            value={myAvatarKey}
            options={avatarOptions}
            onChange={handleAvatarChange}
          />
        </section>
      )}

      {teamMode && (
        <TeamPicker
          roomCode={roomCode}
          teams={teams}
          players={players}
          currentUserId={userId}
          isHost={isHost}
          autoBalanceTeams={autoBalanceTeams}
        />
      )}

      <section className={styles.playerSection} aria-labelledby='lobby-players-heading'>
        <h2 id='lobby-players-heading' className={styles.playerHeading}>
          Players ({players.length} / {session?.settings?.maxPlayers ?? 8})
        </h2>
        <ul className={styles.playerList}>
          {players.map((p) => {
            const playerId = p.userId;
            const isPlayerHost = session?.hostUserId === playerId;
            const isOffline =
              !!playerId && game.offlineUserIds.includes(playerId);
            // Chunk 13 — preset avatar (avatarKey) wins over the player's
            // real pictureUrl when present. avatarPresets only loads once a
            // non-host viewer is in the lobby; on hosts we still resolve via
            // pictureUrl, so this just lights up after the picker fires.
            const presetUrl =
              p.avatarKey && avatarPresets
                ? avatarPresets.find((preset) => preset.key === p.avatarKey)
                    ?.imageUrl
                : undefined;
            const avatarSrc = presetUrl ?? p.pictureUrl;
            return (
              <li
                key={playerId}
                className={`${styles.player} ${isOffline ? styles.offline : ""}`}>
                {avatarSrc ? (
                  <img
                    src={resolveAvatarSrc(avatarSrc)}
                    alt=''
                    className={styles.avatar}
                  />
                ) : (
                  <div className={styles.avatarFallback}>
                    {(p.userName?.[0] ?? "?").toUpperCase()}
                  </div>
                )}
                <span className={styles.playerName}>{p.userName}</span>
                {p.isGuest && <span className={styles.guestBadge}>guest</span>}
                {isOffline && (
                  <span className={styles.offlineBadge} title='Disconnected'>
                    offline
                  </span>
                )}
                {isPlayerHost && <span className={styles.hostBadge}>host</span>}
                {isHost && !isPlayerHost && playerId && (
                  <Btn
                    size='sm'
                    variant='error'
                    type='button'
                    onClick={() => {
                      void handleBoot(playerId);
                    }}
                    aria-label={`Remove ${p.userName ?? "player"} from the lobby`}>
                    Boot
                  </Btn>
                )}
              </li>
            );
          })}
        </ul>
        {players.length === 0 && (
          <p className={styles.emptyState}>Waiting for players to join…</p>
        )}
      </section>

      <footer className={styles.actions}>
        <WsErrorBanner />
        {isHost ? (
          <Btn
            type='button'
            className={styles.startBtn}
            onClick={sendStart}
            disabled={players.length < 1}>
            Start Game
          </Btn>
        ) : (
          <p className={styles.waitingMsg}>Waiting for the host to start…</p>
        )}
        <Btn type='button' className={styles.leaveBtn} onClick={sendLeave}>
          Leave
        </Btn>
      </footer>
    </section>
  );
};

export { Lobby };
