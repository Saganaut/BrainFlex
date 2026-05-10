import { useState, useRef, useEffect } from "react";
import { Link } from "@tanstack/react-router";
import { UserCircleIcon } from "@heroicons/react/24/solid";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { useTheme } from "../../../hooks/useTheme";
import { useGuestLoginMutation } from "../../../store/BrainFlexApi";
import { apiBaseUrl } from "../../../store/emptyApi";
import styles from "./NavBar.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import lightModeIcon from "@/assets/nav/LightModeIcon.svg";
import darkModeIcon from "@/assets/nav/DarkModeIcon.svg";

export function NavBar() {
  const userState = useCurrentUser();
  const user =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user
      : undefined;
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
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleLogin = () => {
    const loginUrl = new URL(`${apiBaseUrl}/api/auth/login`);
    loginUrl.searchParams.set("returnUrl", currentUrl);
    if (userState.state === "guest" && userState.user.id) {
      loginUrl.searchParams.set("guestId", userState.user.id);
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
      return <img src={user.pictureUrl} alt={user.userName} />;
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

  if (userState.state === "loading") {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }
  console.log("user picture", user?.pictureUrl);
  return (
    <div className={styles.navContainer}>
      <div className={styles.userMenuWrapper} ref={dropdownRef}>
        <Link to='/design-system' viewTransition>
          Design system
        </Link>
        <IconBtn
          type='avatar'
          className={styles.avatarBtn}
          onClick={() => {
            setDropdownOpen((prev) => !prev);
          }}
          shape='round'
          bordered={true}
          backgroundColor={true}
          size='lg'
          aria-label='User menu'
          icon={avatarContent()}
        />

        {dropdownOpen && (
          <div className={styles.dropdown}>
            {userState.state === "registered" ? (
              <>
                <span className={styles.dropdownLabel}>
                  Signed in as {user?.userName}
                </span>
                <Link
                  to='/account'
                  className={styles.dropdownItem}
                  onClick={() => {
                    setDropdownOpen(false);
                  }}>
                  Account
                </Link>
                <Btn
                  className={styles.dropdownItem}
                  onClick={void handleLogout}>
                  Logout
                </Btn>
              </>
            ) : userState.state === "guest" ? (
              <>
                <span className={styles.dropdownLabel}>
                  Guest: {user?.userName}
                </span>
                <Btn className={styles.dropdownItem} onClick={handleLogin}>
                  Sign in with Google
                </Btn>
                <Btn
                  className={styles.dropdownItem}
                  onClick={void handleLogout}>
                  Logout guest
                </Btn>
              </>
            ) : (
              <>
                <Btn className={styles.dropdownItem} onClick={handleLogin}>
                  Login with Google
                </Btn>
                <Btn
                  className={styles.dropdownItem}
                  onClick={() => {
                    setShowGuestInput((prev) => !prev);
                  }}>
                  Play as guest
                </Btn>
                {showGuestInput && (
                  <div className={styles.guestInputWrapper}>
                    <Input
                      className={styles.guestInput}
                      value={guestName}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        setGuestName(e.target.value);
                      }}
                      placeholder='Guest username'
                      maxLength={20}
                    />
                    <Btn
                      className={styles.dropdownItem}
                      onClick={void handleGuestLogin}
                      disabled={guestLoading}>
                      Confirm
                    </Btn>
                    {guestError && (
                      <span className={styles.guestError}>{guestError}</span>
                    )}
                  </div>
                )}
              </>
            )}
            <div className={styles.dropdownDivider} />
            <IconBtn
              type='default'
              className={styles.dropdownItem}
              onClick={toggleTheme}
              size='lg'
              icon={
                theme === "dark" ? (
                  <img src={lightModeIcon} />
                ) : (
                  <img src={darkModeIcon} />
                )
              }
            />
          </div>
        )}
      </div>
    </div>
  );
}
