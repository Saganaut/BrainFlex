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
    });
    setSeededFromDeckId(deck.id);
  }

  const [createGame, { isLoading: creating, error: createError }] =
    useCreateInteractiveSessionMutation();

  const submit = async () => {
    if (!deckId) return;
    try {
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
