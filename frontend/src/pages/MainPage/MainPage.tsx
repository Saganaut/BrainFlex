// MainPage: the app's entry point. Prompts users to create a game, create a poll,
// or join an existing session with a room code. Per GAMES.md, this is the quick-start
// surface — no customization shown up front; deeper options live behind the actions.
// import { useState } from "react";
import { useCurrentUser } from "../../hooks/useCurrentUser";
// import { useJoinByRoomCodeMutation } from "../../store/BrainFlexApi";
import { ActionCard } from "@/components/Common/Cards/ActionCard";
// import { Btn } from "@/components/Common/Buttons/Btn";
// import { Input } from "@/components/Common/Input/Input/Input";
// import { extractErrorMessage } from "../../utils/utils";
import styles from "./MainPage.module.css";

const MainPage = () => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";

  // const [code, setCode] = useState("");
  // const [joinByRoomCode, { isLoading: joining, error: joinError }] =
  //   useJoinByRoomCodeMutation();

  // const handleJoin = async (e: React.SubmitEvent) => {
  //   e.preventDefault();
  //   const trimmed = code.trim().toUpperCase();
  //   if (trimmed.length !== 6) return;
  //   try {
  //     await joinByRoomCode({ roomCode: trimmed }).unwrap();
  //     await navigate({
  //       to: "/games/$roomCode/lobby",
  //       params: { roomCode: trimmed },
  //     });
  //   } catch {
  //     // join error surfaced via joinError
  //   }
  // };

  return (
    <div className={styles.page}>
      <header className={styles.hero}>
        <p className={styles.eyebrow}>brainflex</p>
        <h1 className={styles.heroTitle}>What do you want to do?</h1>
        <p className={styles.heroSubtitle}>
          Start a game, run a poll, or jump into a session with a code.
        </p>
      </header>

      <section className={styles.cards} ariaLabel='Primary actions'>
        <ActionCard
          to='/games/create'
          icon='+'
          title='Create a Game'
          description='Play head-to-head. Pick a template, your own deck, or auto-generate.'
          disabled={!isRegistered}
        />
        <ActionCard
          to='/pulse/create'
          icon='~'
          title='Create a Poll'
          description='Run an audience poll — capture answers, no scoring.'
          badge='Soon'
          disabled={!isRegistered}
        />
        <ActionCard
          to='/games/join'
          icon='->'
          title='Join with a Code'
          description='Got a 6-character room code? Hop straight into the lobby.'
        />
      </section>

      {/* <section className={styles.quickJoin} ariaLabel='Quick join'>
        <form
          className={styles.quickJoinForm}
          onSubmit={(e) => {
            void handleJoin(e);
          }}>
          <label htmlFor='room-code' className={styles.quickJoinLabel}>
            Have a code?
          </label>
          <Input
            id='room-code'
            className={styles.codeInput}
            value={code}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setCode(e.target.value.toUpperCase());
            }}
            placeholder='ABCD12'
            maxLength={6}
            autoComplete='off'
            spellCheck={false}
            ariaLabel='Room code'
          />
          <Btn
            type='submit'
            disabled={code.trim().length !== 6 || joining}>
            {joining ? "Joining…" : "Join"}
          </Btn>
        </form>
        {joinError && (
          <p className={styles.errorMsg} role='alert'>
            {extractErrorMessage(
              joinError,
              "Could not join — check the code and try again.",
            )}
          </p>
        )}
      </section> */}

      {!isRegistered && userState.state !== "loading" && (
        <p className={styles.signinHint}>Sign in to create games and polls.</p>
      )}
    </div>
  );
};

export { MainPage };
