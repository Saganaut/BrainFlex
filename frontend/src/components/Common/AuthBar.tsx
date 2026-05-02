import { useState } from "react";
import { useGuestLoginMutation } from "../../store/BrainFlexApi";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { apiBaseUrl } from "../../store/emptyApi";

export function AuthBar() {
  const { user, authenticated, isGuestSession, isLoading } = useCurrentUser();
  const [guestName, setGuestName] = useState("");
  const [guestError, setGuestError] = useState<string | null>(null);
  const [guestLogin, { isLoading: guestLoading }] = useGuestLoginMutation();

  const currentUrl = window.location.href;

  const handleLogin = () => {
    const loginUrl = new URL(`${apiBaseUrl}/api/auth/login`);
    loginUrl.searchParams.set("returnUrl", currentUrl);
    if (isGuestSession && user?.id) {
      loginUrl.searchParams.set("guestId", user.id);
    }
    window.location.href = loginUrl.toString();
  };

  const handleLogout = async () => {
    await fetch(`${apiBaseUrl}/api/auth/logout`, {
      method: "POST",
      credentials: "include",
    });
    window.location.reload();
  };

  const handleGuestLogin = async () => {
    setGuestError(null);
    const trimmedName = guestName.trim();

    if (trimmedName.length < 3 || trimmedName.length > 20) {
      setGuestError("Username must be between 3 and 20 characters.");
      return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(trimmedName)) {
      setGuestError("Letters, numbers, and underscores only.");
      return;
    }

    try {
      await guestLogin({
        guestLoginRequest: { username: trimmedName },
      }).unwrap();
      window.location.reload();
    } catch (error) {
      setGuestError("Unable to create guest session. Try another name.");
    }
  };

  if (isLoading) {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }

  return (
    <div
      style={{
        display: "flex",
        gap: "1rem",
        padding: "1rem",
        alignItems: "center",
        flexWrap: "wrap",
        borderBottom: "1px solid #ddd",
      }}>
      {authenticated ? (
        <>
          <span>Signed in as {user?.userName}</span>
          <button onClick={handleLogout}>Logout</button>
        </>
      ) : isGuestSession ? (
        <>
          <span>Guest mode: {user?.userName}</span>
          <button onClick={handleLogin}>Sign in with Google</button>
          <button onClick={handleLogout}>Logout guest</button>
        </>
      ) : (
        <>
          <button onClick={handleLogin}>Login with Google</button>
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <input
              value={guestName}
              onChange={(e) => setGuestName(e.target.value)}
              placeholder='Guest username'
              maxLength={20}
            />
            <button onClick={handleGuestLogin} disabled={guestLoading}>
              Play as guest
            </button>
          </div>
          {guestError ? (
            <span style={{ color: "red" }}>{guestError}</span>
          ) : null}
        </>
      )}
    </div>
  );
}
