import { useRef, type JSX } from "react";

import { UserCircleIcon } from "@heroicons/react/24/solid";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { useTheme, type ThemeMode } from "../../../hooks/useTheme";
import { useGuestLoginMutation } from "../../../store/BrainFlexApi";
import { apiBaseUrl } from "../../../store/emptyApi";
import { useEffect, useState } from "react";
import styles from "./NavBar.module.css";
interface useUserMenuResponse {
  handleLogin: () => void;
  handleLogout: () => Promise<void>;
  handleGuestLogin: () => Promise<void>;
  avatarContent: () => JSX.Element;
  setGuestName: React.Dispatch<React.SetStateAction<string>>;
  guestError: string | null;
  guestLoading: boolean;
  showGuestInput: boolean;
  theme: ThemeMode;
  toggleTheme: () => void;
  dropdownOpen: boolean;
  setDropdownOpen: React.Dispatch<React.SetStateAction<boolean>>;
  setShowGuestInput: React.Dispatch<React.SetStateAction<boolean>>;
  guestName: string;
}

const useUserMenu = (): useUserMenuResponse => {
  const dropdownRef = useRef<HTMLDivElement>(null);

  const [guestName, setGuestName] = useState("");
  const [guestError, setGuestError] = useState<string | null>(null);
  const [guestLogin, { isLoading: guestLoading }] = useGuestLoginMutation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [showGuestInput, setShowGuestInput] = useState(false);
  const currentUrl = window.location.href;

  const { theme, toggleTheme } = useTheme();
  const userState = useCurrentUser();
  const user =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user
      : undefined;

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
  return {
    handleGuestLogin,
    handleLogin,
    handleLogout,
    setGuestName,
    guestError,
    guestLoading,
    dropdownOpen,
    showGuestInput,
    theme,
    toggleTheme,
    avatarContent,
    setDropdownOpen,
    setShowGuestInput,
    guestName,
  };
};

export { useUserMenu };
