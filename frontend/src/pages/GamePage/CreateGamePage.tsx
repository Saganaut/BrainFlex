// CreateGamePage — three-mode fork for starting a game per GAMES.md.
// Template: pick a system pack, defaults applied, one click to lobby.
// Custom: pick one of the user's packs, edit settings, then create.
// Auto: AI question generation (stubbed; backend not yet implemented).
import { useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import {
  useCreateShowcaseMutation,
  useListDecksQuery,
  useListMyDecksQuery,
} from "../../store/BrainFlexApi";
import type { DeckDto } from "../../store/BrainFlexApi";
import { ActionCard } from "@/components/Common/ActionCard/ActionCard";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input";
import { Checkbox } from "@/components/Common/Input/Checkbox";
import { RadioGroup } from "@/components/Common/Input/RadioGroup";
import { SelectableTile } from "@/components/Common/SelectableTile/SelectableTile";
import { extractErrorMessage } from "../../utils/utils";
import { resolveDeckCover } from "../../utils/deckImages";
import styles from "./Game.module.css";

type Mode = "template" | "custom" | "auto";
type GameMode = "SIMULTANEOUS" | "TURN_BASED";

const DEFAULT_ROUNDS = 10;
const DEFAULT_TIME = 15;
const DEFAULT_SPEED_BONUS = true;
const DEFAULT_GAME_MODE: GameMode = "SIMULTANEOUS";
const DEFAULT_MAX_PLAYERS = 8;
const DEFAULT_ALLOW_GUESTS = true;
const DEFAULT_ALLOW_LATE_JOIN = false;
const DEFAULT_SHOW_SCORES_IMMEDIATELY = true;

// ─── Pack grid ────────────────────────────────────────────────────────────────

interface PackGridProps {
  packs: DeckDto[];
  selectedPackId: string | null;
  onSelect: (id: string) => void;
  emptyMessage: string;
}

const PackGrid = ({
  packs,
  selectedPackId,
  onSelect,
  emptyMessage,
}: PackGridProps) => {
  if (packs.length === 0) {
    return <p className={styles.authMsg}>{emptyMessage}</p>;
  }
  return (
    <div className={styles.packGrid}>
      {packs.map((pack) => (
        <SelectableTile
          key={pack.id}
          media={
            <img
              src={resolveDeckCover(pack.coverImageUrl, pack.id)}
              alt=''
              loading='lazy'
            />
          }
          title={pack.name ?? ""}
          meta={`${(pack.elementCount ?? 0).toString()} elements${
            pack.tags && pack.tags.length > 0 ? ` · ${pack.tags[0]}` : ""
          }`}
          description={pack.description}
          selected={selectedPackId === pack.id}
          onClick={() => {
            if (pack.id) onSelect(pack.id);
          }}
        />
      ))}
    </div>
  );
};

// ─── Mode tabs ────────────────────────────────────────────────────────────────

interface ModeTabsProps {
  mode: Mode;
  onChange: (mode: Mode) => void;
}

const ModeTabs = ({ mode, onChange }: ModeTabsProps) => (
  <div className={styles.modeTabs} role='tablist' aria-label='Create mode'>
    <ActionCard
      onClick={() => {
        onChange("template");
      }}
      selected={mode === "template"}
      icon='*'
      title='Template'
      description='One click to start. Pre-built question packs ready to play.'
    />
    <ActionCard
      onClick={() => {
        onChange("custom");
      }}
      selected={mode === "custom"}
      icon='#'
      title='Custom'
      description='Use a pack you built yourself. Full control over settings.'
    />
    <ActionCard
      onClick={() => {
        onChange("auto");
      }}
      selected={mode === "auto"}
      icon='~'
      title='Auto-Generate'
      description='Type a topic or upload a document. We make the questions.'
      badge='Soon'
    />
  </div>
);

// ─── Settings ─────────────────────────────────────────────────────────────────

interface SettingsState {
  totalRounds: number;
  // 0 = unlimited (no countdown). Any positive value enables the timer.
  timePerQuestion: number;
  speedBonus: boolean;
  gameMode: GameMode;
  maxPlayers: number;
  allowGuests: boolean;
  allowLateJoin: boolean;
  showScoresImmediately: boolean;
}

const DEFAULT_SETTINGS: SettingsState = {
  totalRounds: DEFAULT_ROUNDS,
  timePerQuestion: DEFAULT_TIME,
  speedBonus: DEFAULT_SPEED_BONUS,
  gameMode: DEFAULT_GAME_MODE,
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
          <span className={styles.settingHint}> — 0 = unlimited</span>
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
            name='gameMode'
            legend='Game mode'
            options={[
              { value: "SIMULTANEOUS", label: "Simultaneous — everyone answers at once" },
              { value: "TURN_BASED", label: "Turn-based — host advances each round" },
            ]}
            value={settings.gameMode}
            onChange={(value) => {
              patch({ gameMode: value as GameMode });
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

// ─── Page ─────────────────────────────────────────────────────────────────────

const CreateGamePage = () => {
  const navigate = useNavigate();
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";

  const [mode, setMode] = useState<Mode>("template");
  const [selectedPackId, setSelectedPackId] = useState<string | null>(null);
  const [settings, setSettings] = useState<SettingsState>(DEFAULT_SETTINGS);

  const { data: allPacks = [], isLoading: loadingPublic } = useListDecksQuery();
  const { data: myPacks = [], isLoading: loadingMine } = useListMyDecksQuery(
    undefined,
    { skip: !isRegistered },
  );
  const systemPacks = allPacks.filter((p) => p.isSystem);

  const [createGame, { isLoading: creating, error: createError }] =
    useCreateShowcaseMutation();

  if (userState.state !== "loading" && !isRegistered) {
    return (
      <div className={styles.page}>
        <p className={styles.authMsg}>
          You must be signed in to create a game.
        </p>
        <Link to='/' className={styles.backLink} viewTransition>
          Back to home
        </Link>
      </div>
    );
  }

  const startGame = async (
    packId: string,
    overrides?: Partial<SettingsState>,
  ) => {
    const cfg = { ...settings, ...overrides };
    try {
      const session = await createGame({
        createShowcaseRequest: {
          deckId: packId,
          totalRounds: cfg.totalRounds,
          timePerQuestion: cfg.timePerQuestion,
          speedBonus: cfg.speedBonus,
          gameMode: cfg.gameMode,
          maxPlayers: cfg.maxPlayers,
          allowGuests: cfg.allowGuests,
          allowLateJoin: cfg.allowLateJoin,
          showScoresImmediately: cfg.showScoresImmediately,
        },
      }).unwrap();
      if (session.roomCode) {
        await navigate({
          to: "/games/$roomCode/lobby",
          params: { roomCode: session.roomCode },
        });
      }
    } catch (e) {
      console.error("Failed to create game", e);
    }
  };

  const handleTemplatePick = (packId: string) => {
    setSelectedPackId(packId);
    void startGame(packId, DEFAULT_SETTINGS);
  };

  const handleCustomSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (!selectedPackId) return;
    void startGame(selectedPackId);
  };

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Create Game</h1>
      <p className={styles.authMsg}>How do you want to start?</p>

      <ModeTabs mode={mode} onChange={setMode} />

      {mode === "template" && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Pick a template — one click to play</h2>
          {createError && (
            <p className={styles.errorMsg} role='alert'>
              {extractErrorMessage(createError, "Failed to create game.")}
            </p>
          )}
          {loadingPublic ? (
            <p className={styles.authMsg}>Loading templates…</p>
          ) : (
            <PackGrid
              packs={systemPacks}
              selectedPackId={selectedPackId}
              onSelect={handleTemplatePick}
              emptyMessage='No templates available yet.'
            />
          )}
          {creating && <p className={styles.authMsg}>Creating game…</p>}
          <p className={styles.helperText}>
            Want different settings?{" "}
            <button
              type='button'
              className={styles.linkBtn}
              onClick={() => {
                setMode("custom");
              }}>
              Switch to Custom
            </button>
            .
          </p>
        </section>
      )}

      {mode === "custom" && (
        <form
          className={styles.form}
          onSubmit={handleCustomSubmit}>
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Choose Your Pack</h2>
            {loadingMine ? (
              <p className={styles.authMsg}>Loading your packs…</p>
            ) : myPacks.length === 0 ? (
              <div className={styles.emptyPacks}>
                <p className={styles.authMsg}>
                  You haven&apos;t created any packs yet.
                </p>
                <Link
                  to='/my-packs/create'
                  search={{ returnTo: "/games/create" }}
                  viewTransition>
                  <Btn type='button'>+ Create Your First Pack</Btn>
                </Link>
              </div>
            ) : (
              <>
                <PackGrid
                  packs={myPacks}
                  selectedPackId={selectedPackId}
                  onSelect={setSelectedPackId}
                  emptyMessage='No packs yet.'
                />
                <Link
                  to='/my-packs/create'
                  search={{ returnTo: "/games/create" }}
                  className={styles.helperText}
                  viewTransition>
                  + Create a new pack
                </Link>
              </>
            )}
          </section>

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
            disabled={!selectedPackId || creating}>
            {creating ? "Creating…" : "Create Game"}
          </Btn>
        </form>
      )}

      {mode === "auto" && (
        <section className={styles.section}>
          <div className={styles.comingSoon}>
            <span className={styles.comingIcon} aria-hidden='true'>
              ~
            </span>
            <h2 className={styles.sectionTitle}>Auto-Generate — Coming Soon</h2>
            <p className={styles.authMsg}>
              Soon you&apos;ll be able to type a topic, paste a webpage, or upload
              a PDF, and we&apos;ll build a question pack for you automatically.
            </p>
            <Btn
              type='button'
              onClick={() => {
                setMode("template");
              }}>
              Use a Template Instead
            </Btn>
          </div>
        </section>
      )}

      <Link to='/' className={styles.backLink} viewTransition>
        Back to home
      </Link>
    </div>
  );
};

export { CreateGamePage };
