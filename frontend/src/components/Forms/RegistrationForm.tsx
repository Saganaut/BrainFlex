import React from "react";
import { useRegister } from "./useRegister";
import type { RegisterSearch } from "./useRegister";
import styles from "./Forms.module.css";
import { Link } from "@tanstack/react-router";
export type { RegisterSearch };

interface RegistrationFormProps {
  registerSearchParams: RegisterSearch;
}

const RegistrationForm: React.FC<RegistrationFormProps> = ({
  registerSearchParams,
}) => {
  const {
    username,
    setUsername,
    agreedToTerms,
    setAgreedToTerms,
    newsletter,
    setNewsletter,
    usernameStatus,
    usernameMessage,
    submitError,
    canSubmit,
    isLoading,
    handleSubmit,
  } = useRegister(registerSearchParams);

  return (
    <div className={styles.registrationFormContainer}>
      <h1>What shall we call you?</h1>
      <form className={styles.registrationForm} onSubmit={handleSubmit}>
        <div>
          <div>
            <label htmlFor='username'>Username</label>
            <input
              id='username'
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              maxLength={20}
            />
          </div>
          <div className={[styles.inputInfo, "usernameInfo"].join(" ")}>
            {usernameStatus === "checking" && <span>Checking...</span>}
            {usernameStatus === "available" && <span>Available</span>}
            {(usernameStatus === "invalid" || usernameStatus === "taken") && (
              <span>{usernameMessage}</span>
            )}
          </div>
        </div>
        <div>
          <div>
            <label htmlFor='terms'>
              Agree to our{" "}
              <Link to='/terms-and-conditions'>Terms &amp; Conditions</Link>
            </label>
            <input
              id='terms'
              type='checkbox'
              checked={agreedToTerms}
              onChange={(e) => setAgreedToTerms(e.target.checked)}
            />{" "}
          </div>
          <div></div>
        </div>
        <div>
          <div>
            <label htmlFor='newsletter'>Stay informed</label>
            <input
              id='newsletter'
              type='checkbox'
              checked={newsletter}
              onChange={(e) => setNewsletter(e.target.checked)}
            />
          </div>
          <div></div>
        </div>
        <div>
          <div className={styles.finalRow}>
            {submitError && <p>{submitError}</p>}{" "}
            <button type='submit' disabled={!canSubmit || isLoading}>
              Submit
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export { RegistrationForm };
