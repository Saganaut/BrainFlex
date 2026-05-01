import React, { useState, useEffect } from "react";
import { useLazyCheckUsernameQuery } from "../../store/BrainFlexApi";

export type RegisterSearch = {
  googleId?: string;
  email?: string;
  name?: string;
  picture?: string;
};

export type RegistrationPayload = RegisterSearch & {
  username: string;
  newsletter: boolean;
};

type UsernameStatus = "idle" | "checking" | "available" | "taken" | "invalid";

function validateUsernameFormat(value: string): string | null {
  if (value.length < 3) return "Must be at least 3 characters";
  if (value.length > 20) return "Must be 20 characters or less";
  if (!/^[a-zA-Z0-9_]+$/.test(value))
    return "Letters, numbers, and underscores only";
  return null;
}

interface RegistrationFormProps {
  registerSearchParams: RegisterSearch;
}

const RegistrationForm: React.FC<RegistrationFormProps> = ({
  registerSearchParams,
}) => {
  const [username, setUsername] = useState("");
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const [newsletter, setNewsletter] = useState(true);
  const [usernameStatus, setUsernameStatus] = useState<UsernameStatus>("idle");
  const [usernameMessage, setUsernameMessage] = useState("");

  const [checkUsername] = useLazyCheckUsernameQuery();

  useEffect(() => {
    setUsernameStatus("idle");
    setUsernameMessage("");

    if (!username) return;

    const formatError = validateUsernameFormat(username);
    if (formatError) {
      setUsernameStatus("invalid");
      setUsernameMessage(formatError);
      return;
    }

    setUsernameStatus("checking");

    const timer = setTimeout(async () => {
      try {
        const result = await checkUsername(username).unwrap();
        if (result.available) {
          setUsernameStatus("available");
        } else {
          setUsernameStatus("taken");
          setUsernameMessage("Username is already taken");
        }
      } catch {
        setUsernameStatus("idle");
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [username, checkUsername]);

  const canSubmit = agreedToTerms && usernameStatus === "available";

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    const payload: RegistrationPayload = {
      ...registerSearchParams,
      username,
      newsletter,
    };

    // TODO: dispatch registration mutation when backend route is ready
    console.log("Registration payload:", payload);
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor='username'>Username</label>
          <input
            id='username'
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            maxLength={20}
          />
          {usernameStatus === "checking" && <span>Checking...</span>}
          {usernameStatus === "available" && <span>Available</span>}
          {(usernameStatus === "invalid" || usernameStatus === "taken") && (
            <span>{usernameMessage}</span>
          )}
        </div>
        <div>
          <label htmlFor='terms'>Agree to our Terms &amp; Conditions</label>
          <input
            id='terms'
            type='checkbox'
            checked={agreedToTerms}
            onChange={(e) => setAgreedToTerms(e.target.checked)}
          />
        </div>
        <div>
          <label htmlFor='newsletter'>Newsletter</label>
          <input
            id='newsletter'
            type='checkbox'
            defaultChecked
            checked={newsletter}
            onChange={(e) => setNewsletter(e.target.checked)}
          />
        </div>
        <button type='submit' disabled={!canSubmit}>
          Submit
        </button>
      </form>
    </div>
  );
};

export { RegistrationForm };
