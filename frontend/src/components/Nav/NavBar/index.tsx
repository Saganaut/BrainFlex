import { useState, useRef, useEffect } from "react";
import { Link } from "@tanstack/react-router";
import { UserCircleIcon } from "@heroicons/react/24/solid";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { useTheme } from "../../../hooks/useTheme";
import { useGuestLoginMutation } from "../../../store/BrainFlexApi";
import { apiBaseUrl } from "../../../store/emptyApi";
import styles from "./NavBar.module.css";

export function NavBar() {
  const { user, authenticated, isGuestSession, isLoading } = useCurrentUser();
  const { theme, toggleTheme } = useTheme();
  const [guestName, setGuestName] = useState("");
  const [guestError, setGuestError] = useState<string | null>(null);
  const [guestLogin, { isLoading: guestLoading }] = useGuestLoginMutation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [showGuestInput, setShowGuestInput] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const currentUrl = window.location.href;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setDropdownOpen(false);
        setShowGuestInput(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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
    } catch {
      setGuestError("Unable to create guest session. Try another name.");
    }
  };

  const avatarContent = () => {
    if (user?.pictureUrl) {
      return (
        <img
          src={user.pictureUrl}
          alt={user.userName}
          className={styles.avatarImage}
        />
      );
    }
    if (user?.userName) {
      return (
        <div className={styles.avatarInitial}>
          {user.userName[0].toUpperCase()}
        </div>
      );
    }
    return <UserCircleIcon className={styles.avatarIcon} />;
  };

  if (isLoading) {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }

  return (
    <div className={styles.navContainer}>
      <div className={styles.userMenuWrapper} ref={dropdownRef}>
        <Link to='/design-system' viewTransition>
          Design system
        </Link>
        <button
          type='button'
          className={styles.avatarButton}
          onClick={() => setDropdownOpen((prev) => !prev)}
          aria-label='User menu'>
          {avatarContent()}
        </button>

        {dropdownOpen && (
          <div className={styles.dropdown}>
            {authenticated ? (
              <>
                <span className={styles.dropdownLabel}>
                  Signed in as {user?.userName}
                </span>
                <Link
                  to='/account'
                  className={styles.dropdownItem}
                  onClick={() => setDropdownOpen(false)}>
                  Account
                </Link>
                <button className={styles.dropdownItem} onClick={handleLogout}>
                  Logout
                </button>
              </>
            ) : isGuestSession ? (
              <>
                <span className={styles.dropdownLabel}>
                  Guest: {user?.userName}
                </span>
                <button className={styles.dropdownItem} onClick={handleLogin}>
                  Sign in with Google
                </button>
                <button className={styles.dropdownItem} onClick={handleLogout}>
                  Logout guest
                </button>
              </>
            ) : (
              <>
                <button className={styles.dropdownItem} onClick={handleLogin}>
                  Login with Google
                </button>
                <button
                  className={styles.dropdownItem}
                  onClick={() => setShowGuestInput((prev) => !prev)}>
                  Play as guest
                </button>
                {showGuestInput && (
                  <div className={styles.guestInputWrapper}>
                    <input
                      className={styles.guestInput}
                      value={guestName}
                      onChange={(e) => setGuestName(e.target.value)}
                      placeholder='Guest username'
                      maxLength={20}
                    />
                    <button
                      className={styles.dropdownItem}
                      onClick={handleGuestLogin}
                      disabled={guestLoading}>
                      Confirm
                    </button>
                    {guestError && (
                      <span className={styles.guestError}>{guestError}</span>
                    )}
                  </div>
                )}
              </>
            )}
            <div className={styles.dropdownDivider} />
            <button className={styles.dropdownItem} onClick={toggleTheme}>
              {theme === "dark" ? "Switch to light" : "Switch to dark"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
