import { Link } from "@tanstack/react-router";
import styles from "./DesignSystem.module.css";
import { ThemePicker } from "./ThemePicker";
import { Btn } from "../../components/Common/Buttons/Btn";
import { Card } from "../../components/Common/Cards/Card";
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
import { GameOver } from "../../components/Games/GameOver/GameOver";
import { ContentPackPicker } from "../../components/Games/ContentPackPicker/ContentPackPicker";

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

export function DesignSystem() {
  const { openModal } = useModal();
  const [roundResultIsOpen, setRoundResultIsOpen] = useState(false);
  const openDesignModal = () => {
    const modalConfig = {
      title: "Design Modal",
      content: <div>This is the content, it can be any React Node</div>,
    };

    openModal(modalConfig);
  };
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
        <Link to='/' className={styles.backLink}>
          Back to home
        </Link>
      </div>
      <ThemePicker />
      <section>
        <div className={styles.sectionTitle}>Example components</div>
        <div className={styles.examplesContainer}>
          <h4> Cards </h4>
          <div className={styles.cardComponentContainer}>
            <Card
              header={<h5>Header</h5>}
              body={
                <p>
                  Chuck Norris’ tears cure cancer. Too bad he has never cried.
                  Chuck Norris can have both feet on the ground and kick butt at
                  the same time.
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
                    Chuck Norris’ tears cure cancer. Too bad he has never cried.
                    Chuck Norris can have both feet on the ground and kick butt
                    at the same time.
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
          <h4> Modal </h4>
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
          <h4> Toasts </h4>
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
          <h4> Badges </h4>
          <div className={styles.badgeGroup}>
            <Badge label={"Error"} variant={"error"} />
            <Badge label={"Success"} variant={"success"} />
            <Badge label={"Warning"} variant={"warning"} />
            <Badge label={"Info"} variant={"info"} />
          </div>
          <div className={styles.badgeGroup}>
            <Badge label={"Info Lg"} size={"lg"} variant={"info"} />
          </div>
          <h4> Buttons </h4>
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
        </div>
      </section>
      <section>
        <div className={styles.sectionTitle}>Game Components</div>
        <div className={styles.examplesContainer}>
          <h4> Scoring </h4>
          <div className={styles.cardComponentContainer}>
            <Leaderboard />
            <ScoreBoard
              players={playersData.players}
              currentUserId={playersData.currentUserId}
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
          <h4> Question card </h4>
          <div className={styles.cardComponentContainer}>
            <QuestionCard {...questionCardData} />
          </div>
          <h4> Content Pack Picker </h4>
          <div className={styles.cardComponentContainer}>
            <ContentPackPicker
              selectedPackId={"1"}
              onSelect={() => {
                console.log("selected 1");
              }}
            />{" "}
          </div>
        </div>
      </section>
      <section>
        <div className={styles.sectionTitle}>Color palette</div>
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
        </div>
      </section>{" "}
      <section>
        <div className={styles.sectionTitle}>Semantic tokens</div>
        <div className={styles.tokensContainer}>
          {semanticTokens.map((token) => (
            <TokenRow key={token} token={token} />
          ))}
        </div>
      </section>
    </div>
  );
}
