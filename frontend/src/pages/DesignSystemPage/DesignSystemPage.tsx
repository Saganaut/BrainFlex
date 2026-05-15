import { Link } from "@tanstack/react-router";
import styles from "./DesignSystem.module.css";
import "../../tokens.css";
import { ThemePicker } from "./ThemePicker";
import { FormsSection } from "./FormsSection";
import { Btn } from "../../components/Common/Buttons/Btn";
import { IconBtn } from "../../components/Common/Buttons/IconBtn";
import { CollapseBtn } from "../../components/Common/Buttons/CollapseBtn";
import {
  BellIcon,
  StarIcon,
  TrashIcon,
  PencilSquareIcon,
  UserIcon,
  Cog6ToothIcon,
  MagnifyingGlassIcon,
  HeartIcon,
  EllipsisVerticalIcon,
  PencilIcon,
  ArrowRightStartOnRectangleIcon,
  InboxIcon,
  PhotoIcon,
  PuzzlePieceIcon,
  RocketLaunchIcon,
} from "@heroicons/react/24/outline";
import { Divider } from "../../components/Common/Divider/Divider";
import { Tag } from "../../components/Common/Tag/Tag";
import { Avatar } from "../../components/Common/Avatar/Avatar";
import { EmptyState } from "../../components/Common/EmptyState/EmptyState";
import { ProgressBar } from "../../components/Common/ProgressBar/ProgressBar";
import { Skeleton } from "../../components/Common/Skeleton/Skeleton";
import { Tooltip } from "../../components/Common/Tooltip/Tooltip";
import { Tabs } from "../../components/Common/Tabs/Tabs";
import { useConfirm } from "../../components/Common/ConfirmDialog/useConfirm";
import { SelectableTile } from "../../components/Common/SelectableTile/SelectableTile";
import { Card } from "../../components/Common/Cards/Card";
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuDivider,
  DropdownMenuLabel,
} from "../../components/Menus/DropdownMenu";
import { PlayerInfo } from "../../components/PlayerInfo/PlayerInfo";
import { CephadexLogo } from "../../components/Graphic/CephadexLogo";
import { PricingCard } from "../../components/Pricing/PricingCard/PricingCard";
import { PricingGrid } from "../../components/Pricing/PricingGrid/PricingGrid";
import { BillingToggle } from "../../components/Pricing/BillingToggle/BillingToggle";
import type { BillingCycle } from "../../components/Pricing/BillingToggle/BillingToggle";
import { FeatureList } from "../../components/Pricing/FeatureList/FeatureList";
import { PRICING_TIERS } from "../PricingPage/data";
import { ActionCard } from "../../components/Common/ActionCard/ActionCard";
import { Badge } from "../../components/Common/Badge";
import {
  colorPalette,
  gameOverData,
  playersData,
  questionCardData,
  RoundResultData,
  semanticTokens,
} from "./data";
import { Toast } from "../../components/Common/Toast/Toast";
import { useModal } from "../../context/useModal";
import { Leaderboard } from "../../components/Leaderboard/Leaderboard";
import { ScoreBoard } from "../../components/Games/ScoreBoard/ScoreBoard";
import { RoundResult } from "../../components/Games/RoundResult/RoundResult";
import { useEffect, useState } from "react";
import { QuestionCard } from "../../components/Games/QuestionCard/QuestionCard";
import { TextAnswerInput } from "../../components/Games/TextAnswerInput/TextAnswerInput";
import { VotePanel } from "../../components/Games/VotePanel/VotePanel";
import { SlideView } from "../../components/Games/SlideView/SlideView";
import { WsErrorBanner } from "../../components/Games/WsErrorBanner/WsErrorBanner";
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { BarChart } from "../../components/Common/Charts/BarChart/BarChart";
import { FrequencyList } from "../../components/Common/Charts/FrequencyList/FrequencyList";
import { ReviewPanel } from "../../components/Games/ReviewPanel/ReviewPanel";
import { reviewSampleData } from "./reviewSampleData";
import { useAppDispatch } from "../../store/hooks";
import { wsErrorReceived } from "../../store/gameSlice";
import { ContentDeckPicker } from "../../components/Games/ContentDeckPicker/ContentDeckPicker";
import { Accordion } from "../../components/Containers/Accordion";
import { slideTypeGraphics } from "../../components/Common/Slides/SlideTypeGraphics/slideTypeGraphics";
import { Loader } from "../../components/Common/Loader/Loader";
import {
  NotFoundPage,
  ServerErrorPage,
  ServiceUnavailablePage,
} from "../ErrorPage/ErrorPage";

function ColorSwatch({ token }: { token: string }) {
  return (
    <div className={styles.colorSwatch}>
      <div
        className={styles.colorBox}
        style={{ background: `var(${token})` }}
      />
      <div className={styles.colorToken}>{token}</div>
    </div>
  );
}

const experimentPalettes: { label: string; varBase: string }[] = [
  { label: "Violet", varBase: "--violet" },
  { label: "Orange", varBase: "--orange" },
  { label: "Tolopea", varBase: "--tolopea" },
  { label: "cyan", varBase: "--cyan" },
  { label: "Concrete", varBase: "--concrete" },
  { label: "White", varBase: "--white" },
  { label: "Black Russian", varBase: "--black-russian" },
  { label: "Ultraviolet", varBase: "--ultraviolet" },
];

const experimentShades = [100, 200, 300, 400, 500, 600, 700, 800, 900];

const ExperimentPaletteSection = () => {
  return (
    <section>
      <div className={styles.sectionTitle}> Brand color scales</div>

      <div className={styles.experimentPalettes}>
        {experimentPalettes.map(({ label, varBase }) => (
          <div key={varBase} className={styles.experimentPaletteRow}>
            <div className={styles.experimentPaletteLabel}>
              <div className={styles.experimentPaletteName}>{label}</div>
              <div className={styles.experimentPaletteVar}>
                {`var(${varBase})`}
              </div>
            </div>
            <div className={styles.experimentScale}>
              {experimentShades.map((shade) => {
                const token = `${varBase}-${shade}`;
                return (
                  <div key={token} className={styles.experimentShade}>
                    <div
                      className={styles.experimentShadeBox}
                      style={{ background: `var(${token})` }}
                    />
                    <div className={styles.experimentShadeNumber}>{shade}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};

function TokenRow({ token }: { token: string }) {
  return (
    <div className={styles.tokenRow}>
      <div>
        <div className={styles.tokenLabel}>{token}</div>
        <div className={styles.tokenVar}>{`var(${token})`}</div>
      </div>
      <div
        className={styles.tokenSample}
        style={{ background: `var(${token})` }}>
        sample
      </div>
    </div>
  );
}

// Small demo wrapper: dispatches a fake wsError so the banner is visible in the
// design system. In production the banner reads its state from real STOMP traffic.
function WsErrorBannerDemo() {
  const dispatch = useAppDispatch();
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: "var(--space-3)",
      }}>
      <WsErrorBanner />
      <Btn
        onClick={() => {
          dispatch(
            wsErrorReceived({
              operation: "start",
              roomCode: "DEMO00",
              status: 422,
              message: "Content deck has no questions",
            }),
          );
        }}>
        Trigger sample error
      </Btn>
    </div>
  );
}

const DesignSystemPage = () => {
  const { openModal } = useModal();
  const confirm = useConfirm();
  const [roundResultIsOpen, setRoundResultIsOpen] = useState(false);
  const [demoCollapsed, setDemoCollapsed] = useState(false);
  const [billingCycle, setBillingCycle] = useState<BillingCycle>("monthly");
  const [activeTab, setActiveTab] = useState("overview");
  const [progressValue, setProgressValue] = useState(40);
  const [confirmResult, setConfirmResult] = useState<string | null>(null);
  const [tileChoice, setTileChoice] = useState<string | null>("alpha");
  const openDesignModal = () => {
    const modalConfig = {
      title: "Design Modal",
      content: <div>This is the content, it can be any React Node</div>,
    };

    openModal(modalConfig);
  };

  /**The round result screen cant be closed manually so adding this
   *
   * TODO:should add option to close manually **/
  useEffect(() => {
    console.log("use effect triggered");
    if (roundResultIsOpen) {
      setTimeout(() => {
        console.log("time's up");
        setRoundResultIsOpen(false);
      }, 2000);
    }
  }, [roundResultIsOpen]);

  return (
    <div className={styles.container}>
      <div>
        <div className={styles.sectionTitle}>Design System</div>
        <p className={styles.sectionDescription}>
          A quick view of the app palette, semantic token set, and simple
          component examples.
        </p>
        <Link to='/' className={styles.backLink} viewTransition>
          Back to home
        </Link>
      </div>
      <ExperimentPaletteSection />
      <section>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Theme Picker'>
            <ThemePicker />
          </Accordion>
        </div>
      </section>
      <FormsSection />
      <section>
        <div className={styles.sectionTitle}>Common components</div>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Cards'>
            <div className={styles.cardComponentContainer}>
              <Card
                header={<h5>Header</h5>}
                body={
                  <p>
                    Chuck Norris’ tears cure cancer. Too bad he has never cried.
                    Chuck Norris can have both feet on the ground and kick butt
                    at the same time.
                  </p>
                }
                footer={<span>Footer</span>}
                onClick={() => {
                  console.log("this is a footer");
                }}
              />
              <Card
                header={<h5>Header</h5>}
                body={
                  <div>
                    <img src='https://picsum.photos/200/200' />
                    <p>
                      Chuck Norris’ tears cure cancer. Too bad he has never
                      cried. Chuck Norris can have both feet on the ground and
                      kick butt at the same time.
                    </p>{" "}
                  </div>
                }
                footer={<span>Footer</span>}
                onClick={() => {
                  console.log("this is a footer");
                }}
              />
              <Card
                header={<h5>Header</h5>}
                body={
                  <div>
                    <img src='https://picsum.photos/200/200' />
                  </div>
                }
                footer={<span>Footer</span>}
                onClick={() => {
                  console.log("this is a footer");
                }}
              />
            </div>
          </Accordion>
          <Accordion titleBar='Action Cards'>
            <div className={styles.cardComponentContainer}>
              <ActionCard
                onClick={() => {
                  console.log("clicked template");
                }}
                icon='*'
                title='Template'
                description='One click to start. Pre-built question decks ready to play.'
              />
              <ActionCard
                onClick={() => {
                  console.log("clicked custom");
                }}
                icon='#'
                title='Custom'
                description='Use a deck you built yourself. Full control over settings.'
                selected
              />
              <ActionCard
                onClick={() => {
                  console.log("clicked auto");
                }}
                icon='~'
                title='Auto-Generate'
                description='Type a topic or upload a document. We make the questions.'
                badge='Soon'
                disabled
              />
            </div>
          </Accordion>
          <Accordion titleBar='Modal'>
            <div className={styles.modalGroup}>
              <Btn
                onClick={() => {
                  openDesignModal();
                }}
                size={"sm"}
                shape={"pill"}>
                Open Modal{" "}
              </Btn>
            </div>
          </Accordion>
          <Accordion titleBar='Toasts'>
            <div className={styles.toastGroup}>
              <Toast
                id={"0"}
                onDismiss={() => {
                  console.log("dismissed");
                }}
                duration={10}
                variant={"error"}
                message={"error"}
              />
              <Toast
                id={"0"}
                onDismiss={() => {
                  console.log("dismissed");
                }}
                duration={10}
                variant={"success"}
                message={"success"}
              />{" "}
              <Toast
                id={"0"}
                onDismiss={() => {
                  console.log("dismissed");
                }}
                duration={10}
                variant={"warning"}
                message={"warning"}
              />
              <Toast
                id={"0"}
                onDismiss={() => {
                  console.log("dismissed");
                }}
                duration={10}
                variant={"info"}
                message={"info"}
              />
            </div>
          </Accordion>
          <Accordion titleBar='Badges'>
            <div className={styles.badgeGroup}>
              <Badge label={"Error"} variant={"error"} />
              <Badge label={"Success"} variant={"success"} />
              <Badge label={"Warning"} variant={"warning"} />
              <Badge label={"Info"} variant={"info"} />
            </div>
            <div className={styles.badgeGroup}>
              <Badge label={"Info Lg"} size={"lg"} variant={"info"} />
            </div>
          </Accordion>
          <Accordion titleBar='Buttons'>
            <div className={styles.buttonGroup}>
              <Btn children={<span>Primary md</span>} />
              <Btn size={"sm"} children={<span>Primary sm</span>} />
              <Btn size={"lg"} children={<span>Primary lg</span>} />
              <Btn variant={"error"} children={<span>Error </span>} />
              <Btn variant={"success"} children={<span>Success </span>} />
              <Btn variant={"warning"} children={<span>Warning </span>} />
              <Btn variant={"info"} children={<span>Info</span>} />
              <Btn disabled={true} children={<span>Disabled </span>} />
              <Btn shape={"pill"} children={<span>Pill</span>} />
              <Btn shape={"pill"} size={"sm"} children={<span>Pill sm</span>} />
              <Btn shape={"pill"} size={"lg"} children={<span>Pill lg</span>} />
              <Btn children={<span>Primary lg</span>} />
            </div>
          </Accordion>
          <Accordion titleBar='Loader'>
            <div className={styles.buttonGroup}>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                  minWidth: "160px",
                }}>
                <Loader />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  Default
                </span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                  minWidth: "160px",
                }}>
                <Loader message='Summoning ents…' />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  Custom message
                </span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                  minWidth: "160px",
                }}>
                <Loader withMessage={false} />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  Spinner only
                </span>
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Error pages'>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-6)",
              }}>
              <div
                style={{
                  height: "600px",
                  overflow: "auto",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                }}>
                <NotFoundPage />
              </div>
              <div
                style={{
                  height: "600px",
                  overflow: "auto",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                }}>
                <ServerErrorPage />
              </div>
              <div
                style={{
                  height: "600px",
                  overflow: "auto",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                }}>
                <ServiceUnavailablePage />
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Slide type graphics'>
            <div className={styles.buttonGroup}>
              {Object.entries(slideTypeGraphics).map(([kind, Graphic]) => (
                <div
                  key={kind}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: "var(--space-2)",
                    minWidth: "96px",
                  }}>
                  <Graphic />
                  <span
                    style={{
                      fontSize: "var(--font-size-sm)",
                      color: "var(--text-secondary)",
                    }}>
                    {kind}
                  </span>
                </div>
              ))}
            </div>
          </Accordion>
          <Accordion titleBar='Icon Buttons'>
            <div className={styles.iconBtnSection}>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>Type</span>
                <div className={styles.iconBtnGroup}>
                  <IconBtn type='close' />
                  <IconBtn type='default' icon={<BellIcon />} />
                  <IconBtn type='avatar' icon={<UserIcon />} />
                </div>
              </div>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>Size</span>
                <div className={styles.iconBtnGroup}>
                  <IconBtn type='default' icon={<StarIcon />} size='xs' />
                  <IconBtn type='default' icon={<StarIcon />} size='sm' />
                  <IconBtn type='default' icon={<StarIcon />} size='md' />
                  <IconBtn type='default' icon={<StarIcon />} size='lg' />
                </div>
              </div>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>Shape</span>
                <div className={styles.iconBtnGroup}>
                  <IconBtn
                    type='default'
                    icon={<Cog6ToothIcon />}
                    shape='default'
                    backgroundColor
                  />
                  <IconBtn
                    type='default'
                    icon={<Cog6ToothIcon />}
                    shape='round'
                    backgroundColor
                  />
                </div>
              </div>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>Background</span>
                <div className={styles.iconBtnGroup}>
                  <IconBtn type='default' icon={<MagnifyingGlassIcon />} />
                  <IconBtn
                    type='default'
                    icon={<MagnifyingGlassIcon />}
                    backgroundColor
                  />
                  <IconBtn type='default' icon={<HeartIcon />} />
                  <IconBtn
                    type='default'
                    icon={<HeartIcon />}
                    backgroundColor
                  />
                  <IconBtn type='default' icon={<PencilSquareIcon />} />
                  <IconBtn
                    type='default'
                    icon={<PencilSquareIcon />}
                    backgroundColor
                  />
                  <IconBtn type='default' icon={<TrashIcon />} />
                  <IconBtn
                    type='default'
                    icon={<TrashIcon />}
                    backgroundColor
                  />
                </div>
              </div>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>Disabled</span>
                <div className={styles.iconBtnGroup}>
                  <IconBtn type='default' icon={<BellIcon />} disabled />
                  <IconBtn
                    type='default'
                    icon={<TrashIcon />}
                    disabled
                    backgroundColor
                  />
                  <IconBtn type='close' disabled />
                </div>
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Collapse Button'>
            <div className={styles.iconBtnSection}>
              <div className={styles.iconBtnRow}>
                <span className={styles.iconBtnRowLabel}>State</span>
                <div className={styles.iconBtnGroup}>
                  <CollapseBtn
                    isCollapsed={demoCollapsed}
                    collapse={setDemoCollapsed}
                  />
                  <span
                    style={{
                      fontSize: "var(--font-size-sm)",
                      color: "var(--text-secondary)",
                    }}>
                    {demoCollapsed ? "Collapsed" : "Expanded"} — click to toggle
                  </span>
                </div>
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Dropdown Menu'>
            <div className={styles.buttonGroup}>
              <DropdownMenu
                position='bottom-left'
                trigger={(toggle) => (
                  <IconBtn
                    type='default'
                    icon={<EllipsisVerticalIcon />}
                    onClick={toggle}
                    aria-label='Open menu'
                  />
                )}>
                <DropdownMenuLabel>Account</DropdownMenuLabel>
                <DropdownMenuItem
                  onClick={() => {
                    console.log("view profile");
                  }}>
                  <UserIcon width={16} height={16} /> View profile
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => {
                    console.log("edit");
                  }}>
                  <PencilIcon width={16} height={16} /> Edit settings
                </DropdownMenuItem>
                <DropdownMenuDivider />
                <DropdownMenuItem
                  onClick={() => {
                    console.log("delete");
                  }}>
                  <TrashIcon width={16} height={16} /> Delete account
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => {
                    console.log("logout");
                  }}>
                  <ArrowRightStartOnRectangleIcon width={16} height={16} /> Log
                  out
                </DropdownMenuItem>
              </DropdownMenu>
              <DropdownMenu
                position='bottom-right'
                trigger={(toggle) => (
                  <Btn onClick={toggle} size='sm'>
                    Open menu ▾
                  </Btn>
                )}>
                <DropdownMenuItem centered>One</DropdownMenuItem>
                <DropdownMenuItem centered>Two</DropdownMenuItem>
                <DropdownMenuItem centered>Three</DropdownMenuItem>
              </DropdownMenu>
            </div>
          </Accordion>
          <Accordion titleBar='Cephadex Logo'>
            <div className={styles.buttonGroup}>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                }}>
                <CephadexLogo size='sm' />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  sm
                </span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                }}>
                <CephadexLogo size='md' />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  md
                </span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "var(--space-2)",
                }}>
                <CephadexLogo size='lg' />
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  lg
                </span>
              </div>
            </div>
          </Accordion>
        </div>
      </section>
      <section>
        <div className={styles.sectionTitle}>Game Components</div>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Scoring'>
            <div className={styles.cardComponentContainer}>
              <Leaderboard />
              <ScoreBoard
                players={playersData.players}
                currentUserId={playersData.currentUserId}
              />
              <ScoreBoard
                players={playersData.players}
                currentUserId={playersData.currentUserId}
                answeredUserIds={playersData.players
                  .slice(0, Math.ceil(playersData.players.length / 2))
                  .map((p) => p.userId ?? "")
                  .filter(Boolean)}
                offlineUserIds={playersData.players
                  .slice(-1)
                  .map((p) => p.userId ?? "")
                  .filter(Boolean)}
                isHost
                onBootPlayer={(uid) => {
                  console.log("boot demo:", uid);
                }}
              />

              <Btn
                onClick={() => {
                  setRoundResultIsOpen(!roundResultIsOpen);
                }}>
                {" "}
                Trigger Round Result{" "}
              </Btn>

              {roundResultIsOpen && (
                <RoundResult
                  result={RoundResultData}
                  isHost={true}
                  isTurnBased={false}
                  onNextRound={() => {
                    console.log("next round");
                  }}
                />
              )}
              <GameOver {...gameOverData} />
            </div>
          </Accordion>
          <Accordion titleBar=' Question card'>
            <div className={styles.cardComponentContainer}>
              <QuestionCard {...questionCardData} />
            </div>
          </Accordion>
          <Accordion titleBar=' Text answer input (TEXT_INPUT gameplay)'>
            <div className={styles.cardComponentContainer}>
              <TextAnswerInput
                questionId='design-system-text'
                submittedAnswer={null}
                onSubmit={(answer) => {
                  console.log("text answer:", answer);
                }}
                disabled={false}
              />
            </div>
          </Accordion>
          <Accordion titleBar=' Vote panel (Best Answer VOTE phase)'>
            <div className={styles.cardComponentContainer}>
              <VotePanel
                element={{
                  kind: "TextQuestion",
                  id: "design-system-ba",
                  prompt: "Coin a new name for our Mars colony.",
                  pointValue: 0,
                  difficulty: "EASY",
                  bestAnswerMode: true,
                  bestAnswerBonus: 100,
                  caseSensitive: false,
                  displaySeconds: 30,
                  mediaPosition: "NONE",
                }}
                submissions={[
                  {
                    submissionId: "s1",
                    payload: { kind: "TextAnswer", text: "New Phobos" },
                  },
                  {
                    submissionId: "s2",
                    payload: { kind: "TextAnswer", text: "Olympus Prime" },
                  },
                  {
                    submissionId: "s3",
                    payload: { kind: "TextAnswer", text: "Red Haven" },
                  },
                ]}
                myVote={null}
                totalPlayers={4}
                votedCount={2}
                timeRemaining={18}
                unlimited={false}
                onVote={(id) => {
                  console.log("vote demo:", id);
                }}
              />
            </div>
          </Accordion>
          <Accordion titleBar=' Slide view (SLIDE element gameplay)'>
            <div className={styles.cardComponentContainer}>
              <SlideView
                round={2}
                totalRounds={10}
                timeRemaining={4}
                slide={{
                  kind: "Slide",
                  id: "design-system-slide",
                  slideKind: "SECTION",
                  title: "Section 2 — Arts & History",
                  body: "Now we'll switch from geography to paintings, plays, and the past.",
                  displaySeconds: 5,
                  mediaPosition: "NONE",
                }}
              />
            </div>
          </Accordion>
          <Accordion titleBar=' WebSocket error banner'>
            <div className={styles.cardComponentContainer}>
              <WsErrorBannerDemo />
            </div>
          </Accordion>
          <Accordion titleBar=' Bar chart'>
            <div className={styles.cardComponentContainer}>
              <BarChart
                caption='Sample MCQ distribution'
                total={6}
                items={[
                  { label: "A — Venus", value: 1 },
                  { label: "B — Jupiter", value: 0 },
                  { label: "C — Mars", value: 4, highlight: true },
                  { label: "D — Saturn", value: 1 },
                ]}
              />
            </div>
          </Accordion>
          <Accordion titleBar=' Frequency list'>
            <div className={styles.cardComponentContainer}>
              <FrequencyList
                caption='Sample text-input submissions'
                total={6}
                items={[
                  { text: "Paris", count: 4, correct: true },
                  { text: "paris", count: 1, correct: true },
                  { text: "Lyon", count: 1 },
                ]}
              />
            </div>
          </Accordion>
          <Accordion titleBar=' Review panel (post-showcase)'>
            <div className={styles.cardComponentContainer}>
              <ReviewPanel review={reviewSampleData} />
            </div>
          </Accordion>
          <Accordion titleBar=' Content Deck Picker'>
            <h4> </h4>
            <div className={styles.cardComponentContainer}>
              <ContentDeckPicker
                selectedDeckId={"1"}
                onSelect={() => {
                  console.log("selected 1");
                }}
              />
            </div>
          </Accordion>
          <Accordion titleBar='Player Info'>
            <div className={styles.cardComponentContainer}>
              <PlayerInfo />
            </div>
          </Accordion>
        </div>
      </section>
      <section>
        <div className={styles.sectionTitle}>New primitives</div>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Divider'>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-3)",
                maxWidth: "480px",
              }}>
              <span style={{ color: "var(--text-secondary)" }}>Above</span>
              <Divider />
              <span style={{ color: "var(--text-secondary)" }}>Below</span>
            </div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "var(--space-3)",
                marginTop: "var(--space-4)",
                height: "40px",
              }}>
              <span style={{ color: "var(--text-secondary)" }}>Left</span>
              <Divider orientation='vertical' />
              <span style={{ color: "var(--text-secondary)" }}>Right</span>
            </div>
          </Accordion>
          <Accordion titleBar='Tag'>
            <div className={styles.buttonGroup}>
              <Tag>Geography</Tag>
              <Tag>Trivia</Tag>
              <Tag size='sm'>sm</Tag>
              <Tag size='md'>md</Tag>
              <Tag
                onRemove={() => {
                  console.log("removed");
                }}>
                Removable
              </Tag>
            </div>
          </Accordion>
          <Accordion titleBar='Avatar'>
            <div className={styles.buttonGroup}>
              <Avatar size='xs' name='Frodo Baggins' />
              <Avatar size='sm' name='Samwise Gamgee' />
              <Avatar size='md' name='Aragorn' />
              <Avatar size='lg' name='Legolas' />
              <Avatar size='xl' name='Gimli' />
              <Avatar size='lg' src='https://i.pravatar.cc/96?img=12' />
              <Avatar size='lg' />
            </div>
          </Accordion>
          <Accordion titleBar='Empty state'>
            <div
              style={{
                display: "flex",
                gap: "var(--space-4)",
                flexWrap: "wrap",
              }}>
              <div
                style={{
                  flex: "1 1 280px",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                }}>
                <EmptyState
                  icon={<InboxIcon />}
                  title='No decks yet'
                  message='Create your first content deck to get started.'
                  action={<Btn size='sm'>+ New deck</Btn>}
                />
              </div>
              <div
                style={{
                  flex: "1 1 280px",
                  border: "1px solid var(--border-subtle)",
                  borderRadius: "var(--radius-md)",
                }}>
                <EmptyState
                  size='sm'
                  icon={<RocketLaunchIcon />}
                  title='Nothing here'
                />
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Progress bar'>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-4)",
                maxWidth: "480px",
              }}>
              <ProgressBar value={progressValue} showLabel label='Loading' />
              <ProgressBar value={progressValue} variant='brand' size='sm' />
              <ProgressBar value={progressValue} variant='success' size='lg' />
              <ProgressBar value={70} variant='warning' />
              <ProgressBar value={90} variant='error' />
              <ProgressBar value={0} indeterminate />
              <div style={{ display: "flex", gap: "var(--space-2)" }}>
                <Btn
                  size='sm'
                  onClick={() => {
                    setProgressValue((v) => Math.max(0, v - 10));
                  }}>
                  −10
                </Btn>
                <Btn
                  size='sm'
                  onClick={() => {
                    setProgressValue((v) => Math.min(100, v + 10));
                  }}>
                  +10
                </Btn>
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Skeleton'>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-3)",
                maxWidth: "320px",
              }}>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "var(--space-3)",
                }}>
                <Skeleton variant='circle' />
                <Skeleton variant='text' count={2} />
              </div>
              <Skeleton variant='rect' height={120} />
              <Skeleton variant='text' count={3} />
            </div>
          </Accordion>
          <Accordion titleBar='Tooltip'>
            <div className={styles.buttonGroup}>
              <Tooltip label='Tooltip above'>
                <Btn size='sm'>Hover me (top)</Btn>
              </Tooltip>
              <Tooltip label='Tooltip below' position='bottom'>
                <Btn size='sm'>Hover me (bottom)</Btn>
              </Tooltip>
              <Tooltip label='Tooltip right' position='right'>
                <Btn size='sm'>Hover me (right)</Btn>
              </Tooltip>
              <Tooltip label='Delete forever'>
                <IconBtn
                  type='default'
                  icon={<TrashIcon />}
                  aria-label='Delete'
                />
              </Tooltip>
            </div>
          </Accordion>
          <Accordion titleBar='Tabs'>
            <Tabs
              ariaLabel='Design-system demo'
              value={activeTab}
              onChange={setActiveTab}
              items={[
                {
                  id: "overview",
                  label: "Overview",
                  panel: (
                    <p style={{ color: "var(--text-secondary)" }}>
                      The overview tab. Use ← / → on the tab strip to move focus
                      + selection.
                    </p>
                  ),
                },
                {
                  id: "details",
                  label: "Details",
                  panel: (
                    <p style={{ color: "var(--text-secondary)" }}>
                      Detail body. Each panel is mounted but only the active one
                      is visible, so internal state survives a switch.
                    </p>
                  ),
                },
                {
                  id: "history",
                  label: "History",
                  panel: (
                    <p style={{ color: "var(--text-secondary)" }}>
                      A history panel.
                    </p>
                  ),
                },
                {
                  id: "disabled",
                  label: "Disabled",
                  panel: null,
                  disabled: true,
                },
              ]}
            />
            <div style={{ marginTop: "var(--space-6)" }}>
              <span
                style={{
                  fontSize: "var(--font-size-sm)",
                  color: "var(--text-secondary)",
                }}>
                Pill variant:
              </span>
              <div style={{ marginTop: "var(--space-2)" }}>
                <Tabs
                  variant='pill'
                  ariaLabel='Pill variant'
                  value={activeTab}
                  onChange={setActiveTab}
                  items={[
                    {
                      id: "overview",
                      label: "Overview",
                      panel: <span />,
                    },
                    { id: "details", label: "Details", panel: <span /> },
                    { id: "history", label: "History", panel: <span /> },
                  ]}
                />
              </div>
            </div>
          </Accordion>
          <Accordion titleBar='Confirm dialog'>
            <div className={styles.buttonGroup}>
              <Btn
                onClick={() => {
                  void confirm({
                    title: "Save changes?",
                    message: "Your edits will be saved to the deck.",
                    confirmLabel: "Save",
                  }).then((ok) => {
                    setConfirmResult(ok ? "Confirmed (save)" : "Cancelled");
                  });
                }}>
                Open default
              </Btn>
              <Btn
                variant='error'
                onClick={() => {
                  void confirm({
                    title: "Delete this deck?",
                    message:
                      "This will permanently remove the deck and its questions.",
                    confirmLabel: "Delete",
                    cancelLabel: "Keep it",
                    variant: "danger",
                  }).then((ok) => {
                    setConfirmResult(ok ? "Confirmed (delete)" : "Cancelled");
                  });
                }}>
                Open danger
              </Btn>
              {confirmResult && (
                <span
                  style={{
                    fontSize: "var(--font-size-sm)",
                    color: "var(--text-secondary)",
                  }}>
                  Last result: {confirmResult}
                </span>
              )}
            </div>
          </Accordion>
          <Accordion titleBar='Selectable tile'>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
                gap: "var(--space-3)",
                maxWidth: "720px",
              }}>
              <SelectableTile
                media={<PuzzlePieceIcon />}
                title='Alpha'
                meta='12 elements'
                description='A tile with media, title, meta, and description.'
                selected={tileChoice === "alpha"}
                onClick={() => {
                  setTileChoice("alpha");
                }}
              />
              <SelectableTile
                media={<PhotoIcon />}
                title='Beta'
                meta='8 elements · Geography'
                description='Another tile in the same grid.'
                selected={tileChoice === "beta"}
                onClick={() => {
                  setTileChoice("beta");
                }}
              />
              <SelectableTile
                media={<RocketLaunchIcon />}
                title='Gamma'
                badge='New'
                meta='3 elements'
                selected={tileChoice === "gamma"}
                onClick={() => {
                  setTileChoice("gamma");
                }}
              />
              <SelectableTile
                media={<InboxIcon />}
                title='Disabled'
                meta='—'
                disabled
                onClick={() => {
                  /* no-op */
                }}
              />
            </div>
          </Accordion>
        </div>
      </section>
      <section>
        <div className={styles.sectionTitle}>Pricing components</div>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Billing toggle'>
            <div className={styles.buttonGroup}>
              <BillingToggle
                value={billingCycle}
                onChange={setBillingCycle}
                options={[
                  { value: "monthly", label: "Monthly" },
                  {
                    value: "annual",
                    label: "Annual",
                    savingsLabel: "Save 20%",
                  },
                ]}
              />
            </div>
          </Accordion>
          <Accordion titleBar='Feature list'>
            <div
              style={{
                maxWidth: "320px",
                padding: "var(--space-4)",
                border: "1px solid var(--border-subtle)",
                borderRadius: "var(--radius-md)",
              }}>
              <FeatureList
                items={[
                  { label: "Unlimited public games" },
                  { label: "Stats and streaks" },
                  { label: "Custom decks" },
                  { label: "Private matches", included: false },
                  { label: "Org-wide branding", included: false },
                ]}
              />
            </div>
          </Accordion>
          <Accordion titleBar='Pricing grid'>
            <PricingGrid columns={3}>
              {PRICING_TIERS.map((tier) => {
                const cycle = tier.prices[billingCycle];
                return (
                  <PricingCard
                    key={tier.key}
                    name={tier.name}
                    tagline={tier.tagline}
                    price={cycle.amount}
                    priceUnit={cycle.unit}
                    features={tier.features}
                    ctaLabel={tier.ctaLabel}
                    ctaTo={tier.ctaTo}
                    featured={tier.featured}
                    badge={tier.badge}
                    footnote={tier.footnote}
                  />
                );
              })}
            </PricingGrid>
          </Accordion>
        </div>
      </section>
      <section>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Color palette'>
            <div className={styles.paletteContainer}>
              {colorPalette.map((group) => (
                <div key={group.label} className={styles.paletteGroup}>
                  <div className={styles.paletteGroupTitle}>{group.label}</div>
                  <div className={styles.colorGrid}>
                    {group.tokens.map((token) => (
                      <ColorSwatch key={token} token={token} />
                    ))}
                  </div>
                </div>
              ))}
            </div>{" "}
          </Accordion>
        </div>
      </section>{" "}
      <section>
        <div className={styles.examplesContainer}>
          <Accordion titleBar='Semantic tokens'>
            <div className={styles.tokensContainer}>
              {semanticTokens.map((token) => (
                <TokenRow key={token} token={token} />
              ))}
            </div>
          </Accordion>
        </div>
      </section>
    </div>
  );
};

export { DesignSystemPage };
