// CreateGamePage — customize-before-start screen for a known deck. Reached
// via the chevron menu on a deck's Quick Start button (carries ?deckId=<id>).
// The deck-picker step lives on /decks now; without a deckId we bounce there.
import { useEffect, useState } from "react";
import { getRouteApi, useNavigate, Link } from "@tanstack/react-router";

import { useCreateInteractiveSessionMutation, useGetDeckQuery } from "../../store/BrainFlexApi";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input/Input";
import { Checkbox } from "@/components/Common/Input/Checkbox/Checkbox";
import { RadioGroup } from "@/components/Common/Input/RadioGroup/RadioGroup";
import { extractErrorMessage } from "../../utils/utils";
import styles from "./Game.module.css";

const routeApi = getRouteApi("/_authenticated/games/create");

type SessionMode = "SIMULTANEOUS" | "TURN_BASED";

const DEFAULT_ROUNDS = 10;
const DEFAULT_TIME = 15;
const DEFAULT_SPEED_BONUS = true;
const DEFAULT_MODE: SessionMode = "SIMULTANEOUS";
const DEFAULT_MAX_PLAYERS = 8;
const DEFAULT_ALLOW_GUESTS = true;
const DEFAULT_ALLOW_LATE_JOIN = false;
const DEFAULT_SHOW_SCORES_IMMEDIATELY = true;
const DEFAULT_REACTIONS_ENABLED = true;
const DEFAULT_CHAT_ENABLED = true;
const DEFAULT_TEAM_MODE = false;
const DEFAULT_TEAM_COUNT = 4;
const DEFAULT_AUTO_BALANCE_TEAMS = true;
const DEFAULT_ANONYMOUS_MODE = false;
const TEAM_COUNT_MIN = 2;
const TEAM_COUNT_MAX = 8;

interface SettingsState {
  totalRounds: number;
  // 0 = unlimited (no countdown). Any positive value enables the timer.
  timePerQuestion: number;
  speedBonus: boolean;
  mode: SessionMode;
  maxPlayers: number;
  allowGuests: boolean;
  allowLateJoin: boolean;
  showScoresImmediately: boolean;
  reactionsEnabled: boolean;
  chatEnabled: boolean;
  teamMode: boolean;
  teamCount: number;
  autoBalanceTeams: boolean;
  anonymousMode: boolean;
  customRoomCode: string;
}

const PLATFORM_DEFAULTS: SettingsState = {
  totalRounds: DEFAULT_ROUNDS,
  timePerQuestion: DEFAULT_TIME,
  speedBonus: DEFAULT_SPEED_BONUS,
  mode: DEFAULT_MODE,
  maxPlayers: DEFAULT_MAX_PLAYERS,
  allowGuests: DEFAULT_ALLOW_GUESTS,
  allowLateJoin: DEFAULT_ALLOW_LATE_JOIN,
  showScoresImmediately: DEFAULT_SHOW_SCORES_IMMEDIATELY,
  reactionsEnabled: DEFAULT_REACTIONS_ENABLED,
  chatEnabled: DEFAULT_CHAT_ENABLED,
  teamMode: DEFAULT_TEAM_MODE,
  teamCount: DEFAULT_TEAM_COUNT,
  autoBalanceTeams: DEFAULT_AUTO_BALANCE_TEAMS,
  anonymousMode: DEFAULT_ANONYMOUS_MODE,
  customRoomCode: "",
};

interface SettingsFormProps {
  settings: SettingsState;
  onChange: (next: SettingsState) => void;
}

const SettingsForm = ({ settings, onChange }: SettingsFormProps) => {
  const patch = (next: Partial<SettingsState>) => {
    onChange({ ...settings, ...next });
  };

  return (
    <div className={styles.settings}>
      <label className={styles.setting}>
        <span>Rounds</span>
        <Input
          type='number'
          min={3}
          max={30}
          value={settings.totalRounds}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            patch({ totalRounds: e.target.valueAsNumber });
          }}
          className={styles.numberInput}
        />
      </label>
      <label className={styles.setting}>
        <span>
          Seconds per question
          <span className={styles.settingHint}> (0 = unlimited )</span>
        </span>
        <Input
          type='number'
          min={0}
          max={120}
          value={settings.timePerQuestion}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            patch({ timePerQuestion: Number(e.target.value) });
          }}
          className={styles.numberInput}
        />
      </label>
      <label className={styles.setting}>
        <span>Speed bonus</span>
        <Checkbox
          checked={settings.speedBonus}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            patch({ speedBonus: e.target.checked });
          }}
          disabled={settings.timePerQuestion === 0}
        />
      </label>

      <details className={styles.moreOptions}>
        <summary className={styles.moreOptionsSummary}>More options</summary>
        <div className={styles.moreOptionsBody}>
          <RadioGroup
            name='mode'
            legend='Session mode'
            options={[
              {
                value: "SIMULTANEOUS",
                label: "Simultaneous — everyone answers at once",
              },
              {
                value: "TURN_BASED",
                label: "Turn-based — host advances each round",
              },
            ]}
            value={settings.mode}
            onChange={(value) => {
              patch({ mode: value as SessionMode });
            }}
          />

          <label className={styles.setting}>
            <span>Max players</span>
            <Input
              type='number'
              min={2}
              max={20}
              value={settings.maxPlayers}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ maxPlayers: e.target.valueAsNumber });
              }}
              className={styles.numberInput}
            />
          </label>

          <label className={styles.setting}>
            <span>Allow guests (no sign-in required)</span>
            <Checkbox
              checked={settings.allowGuests}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ allowGuests: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Allow players to join after the game starts</span>
            <Checkbox
              checked={settings.allowLateJoin}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ allowLateJoin: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Show scores during the game</span>
            <Checkbox
              checked={settings.showScoresImmediately}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ showScoresImmediately: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Enable emoji reactions</span>
            <Checkbox
              checked={settings.reactionsEnabled}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ reactionsEnabled: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Enable audience chat</span>
            <Checkbox
              checked={settings.chatEnabled}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ chatEnabled: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Team mode</span>
            <Checkbox
              checked={settings.teamMode}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ teamMode: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>Number of teams</span>
            <Input
              type='number'
              min={TEAM_COUNT_MIN}
              max={TEAM_COUNT_MAX}
              value={settings.teamCount}
              disabled={!settings.teamMode}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ teamCount: e.target.valueAsNumber });
              }}
              className={styles.numberInput}
            />
          </label>

          <label className={styles.setting}>
            <span>Auto-balance teams as players join</span>
            <Checkbox
              checked={settings.autoBalanceTeams}
              disabled={!settings.teamMode}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ autoBalanceTeams: e.target.checked });
              }}
            />
          </label>

          <label className={styles.setting}>
            <span>
              Anonymous mode
              <span className={styles.settingHint}>
                {" "}
                — hide player names on the scoreboard
              </span>
            </span>
            <Checkbox
              checked={settings.anonymousMode}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ anonymousMode: e.target.checked });
              }}
            />
          </label>

          {/* TODO(backend): the request DTO accepts customRoomCode but the
              service doesn't currently validate uniqueness — a memorable code
              that collides with an existing live session will be rejected
              with a generic 400. Surface that explicitly once the server
              owns the collision check. */}
          <label className={styles.setting}>
            <span>
              Custom room code
              <span className={styles.settingHint}>
                {" "}
                — leave blank for a random 6-char code
              </span>
            </span>
            <Input
              type='text'
              maxLength={12}
              value={settings.customRoomCode}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                patch({ customRoomCode: e.target.value.toUpperCase() });
              }}
              className={styles.numberInput}
            />
          </label>
        </div>
      </details>
    </div>
  );
};

const CreateGamePage = () => {
  const navigate = useNavigate();
  const { deckId } = routeApi.useSearch();

  // No deck → pick one. /decks is My Decks, which has Quick Start buttons.
  useEffect(() => {
    if (!deckId) {
      void navigate({ to: "/decks", replace: true });
    }
  }, [deckId, navigate]);

  const { data: deck, isLoading: loadingDeck } = useGetDeckQuery(
    { id: deckId ?? "" },
    { skip: !deckId },
  );

  // Seed the form with the deck's defaultSettings; fall back per-field to
  // platform defaults. The backend would also cascade nulls if we sent them,
  // but pre-filling the visible form is what makes "customize" useful.
  const [settings, setSettings] = useState<SettingsState>(PLATFORM_DEFAULTS);
  const [seededFromDeckId, setSeededFromDeckId] = useState<string | null>(null);
  if (deck?.id && deck.id !== seededFromDeckId) {
    const d = deck.defaultSettings ?? {};
    setSettings({
      totalRounds: d.totalRounds ?? DEFAULT_ROUNDS,
      timePerQuestion: d.timePerQuestion ?? DEFAULT_TIME,
      speedBonus: d.speedBonus ?? DEFAULT_SPEED_BONUS,
      mode: d.mode ?? DEFAULT_MODE,
      maxPlayers: d.maxPlayers ?? DEFAULT_MAX_PLAYERS,
      allowGuests: d.allowGuests ?? DEFAULT_ALLOW_GUESTS,
      allowLateJoin: d.allowLateJoin ?? DEFAULT_ALLOW_LATE_JOIN,
      showScoresImmediately:
        d.showScoresImmediately ?? DEFAULT_SHOW_SCORES_IMMEDIATELY,
      reactionsEnabled: d.reactionsEnabled ?? DEFAULT_REACTIONS_ENABLED,
      chatEnabled: d.chatEnabled ?? DEFAULT_CHAT_ENABLED,
      teamMode: d.teamMode ?? DEFAULT_TEAM_MODE,
      teamCount: d.teamCount ?? DEFAULT_TEAM_COUNT,
      autoBalanceTeams: d.autoBalanceTeams ?? DEFAULT_AUTO_BALANCE_TEAMS,
      anonymousMode: DEFAULT_ANONYMOUS_MODE,
      customRoomCode: "",
    });
    setSeededFromDeckId(deck.id);
  }

  const [createGame, { isLoading: creating, error: createError }] =
    useCreateInteractiveSessionMutation();

  const submit = async () => {
    if (!deckId) return;
    try {
      const trimmedRoomCode = settings.customRoomCode.trim();
      const session = await createGame({
        createInteractiveSessionRequest: {
          deckId,
          totalRounds: settings.totalRounds,
          timePerQuestion: settings.timePerQuestion,
          speedBonus: settings.speedBonus,
          mode: settings.mode,
          maxPlayers: settings.maxPlayers,
          allowGuests: settings.allowGuests,
          allowLateJoin: settings.allowLateJoin,
          showScoresImmediately: settings.showScoresImmediately,
          reactionsEnabled: settings.reactionsEnabled,
          chatEnabled: settings.chatEnabled,
          teamMode: settings.teamMode,
          teamCount: settings.teamMode ? settings.teamCount : undefined,
          autoBalanceTeams: settings.teamMode
            ? settings.autoBalanceTeams
            : undefined,
          anonymousMode: settings.anonymousMode,
          customRoomCode: trimmedRoomCode.length > 0 ? trimmedRoomCode : undefined,
        },
      }).unwrap();
      if (session.roomCode) {
        await navigate({
          to: "/games/$roomCode/lobby",
          params: { roomCode: session.roomCode },
        });
      }
    } catch (err) {
      console.error("Failed to create game", err);
    }
  };

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    void submit();
  };

  if (!deckId) return null;

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Customize your game</h1>
      <p className={styles.authMsg}>
        {loadingDeck
          ? "Loading deck…"
          : deck?.name
            ? `Starting "${deck.name}". Adjust the settings, then start.`
            : "Adjust the settings, then start."}
      </p>

      <form className={styles.form} onSubmit={handleSubmit}>
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Settings</h2>
          <SettingsForm settings={settings} onChange={setSettings} />
        </section>

        {createError && (
          <p className={styles.errorMsg} role='alert'>
            {extractErrorMessage(createError, "Failed to create game.")}
          </p>
        )}

        <Btn
          type='submit'
          className={styles.createBtn}
          disabled={creating}>
          {creating ? "Creating…" : "Start Game"}
        </Btn>
      </form>

      <Link to='/decks' className={styles.backLink} viewTransition>
        Back to My Decks
      </Link>
    </div>
  );
};

export { CreateGamePage };
