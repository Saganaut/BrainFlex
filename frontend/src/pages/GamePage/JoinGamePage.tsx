import { useNavigate, Link } from "@tanstack/react-router";
import { useState } from "react";
import { useJoinByRoomCodeMutation } from "../../store/BrainFlexApi";
import { extractErrorMessage } from "../../utils/utils";
import styles from "./GameHub.module.css";
import { Alert } from "@/components/Common/Alert/Alert";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input/Input";
const JoinGamePage = () => {
  const navigate = useNavigate();
  const [code, setCode] = useState("");
  const [joinGame, { isLoading, error }] = useJoinByRoomCodeMutation();

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    const trimmed = code.trim().toUpperCase();
    if (trimmed.length !== 6) return;
    try {
      await joinGame({ roomCode: trimmed }).unwrap();
      await navigate({
        to: "/games/$roomCode/lobby",
        params: { roomCode: trimmed },
      });
    } catch {
      // error shown via `error` state
    }
  };

  return (
    <>
      <div className={styles.page}>
        <h1 className={styles.title}>Join Game</h1>
        <p className={styles.subtitle}>
          Enter the 6-character room code from your host.
        </p>

        <form className={styles.form} onSubmit={(e) => void handleSubmit(e)}>
          <Input
            className={styles.codeInput}
            value={code}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setCode(e.target.value.toUpperCase());
            }}
            placeholder='ABCD12'
            maxLength={6}
            autoFocus
            autoComplete='off'
            spellCheck={false}
          />
          <Btn
            type='submit'
            className={styles.joinBtn}
            disabled={code.trim().length !== 6 || isLoading}>
            {isLoading ? "Joining…" : "Join Game"}
          </Btn>
          {error != null && (
            <Alert severity='error'>
              {extractErrorMessage(
                error,
                "Could not join — check the room code and try again.",
              )}
            </Alert>
          )}
        </form>

        <Link to='/' className={styles.backLink} viewTransition>
          Back to home
        </Link>
      </div>
    </>
  );
};

export { JoinGamePage };
