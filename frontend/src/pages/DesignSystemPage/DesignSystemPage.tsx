import { Link } from "@tanstack/react-router";
import styles from "./DesignSystem.module.css";
import { ThemePicker } from "./ThemePicker";
import { FormsSection } from "./FormsSection";
import { Btn } from "../../components/Common/Buttons/Btn";
import { IconBtn } from "../../components/Common/Buttons/IconBtn";
import {
  BellIcon,
  StarIcon,
  TrashIcon,
  PencilSquareIcon,
  UserIcon,
  Cog6ToothIcon,
  MagnifyingGlassIcon,
  HeartIcon,
} from "@heroicons/react/24/outline";
import { Card } from "../../components/Common/Cards/Card";
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
import { WsErrorBanner } from "../../components/Games/WsErrorBanner/WsErrorBanner";
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { BarChart } from "../../components/Common/Charts/BarChart/BarChart";
import { FrequencyList } from "../../components/Common/Charts/FrequencyList/FrequencyList";
import { ReviewPanel } from "../../components/Games/ReviewPanel/ReviewPanel";
import { reviewSampleData } from "./reviewSampleData";
import { useAppDispatch } from "../../store/hooks";
import { wsErrorReceived } from "../../store/gameSlice";
import { ContentPackPicker } from "../../components/Games/ContentPackPicker/ContentPackPicker";
import { Accordion } from "../../components/Containers/Accordion";

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
    <div style={{ display: "flex", flexDirection: "column", gap: "var(--space-3)" }}>
      <WsErrorBanner />
      <Btn
        onClick={() => {
          dispatch(
            wsErrorReceived({
              operation: "start",
              roomCode: "DEMO00",
              status: 422,
              message: "Content pack has no questions",
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
  const [roundResultIsOpen, setRoundResultIsOpen] = useState(false);
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
                description='One click to start. Pre-built question packs ready to play.'
              />
              <ActionCard
                onClick={() => {
                  console.log("clicked custom");
                }}
                icon='#'
                title='Custom'
                description='Use a pack you built yourself. Full control over settings.'
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
          <Accordion titleBar=' Content Pack Picker'>
            <h4> </h4>
            <div className={styles.cardComponentContainer}>
              <ContentPackPicker
                selectedPackId={"1"}
                onSelect={() => {
                  console.log("selected 1");
                }}
              />
            </div>
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
