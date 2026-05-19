// Avatar button + contextual auth/account menu in the NavBar
import React from "react";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Input } from "@/components/Common/Input/Input/Input";
import SunIcon from "@/assets/icons/theme/sun.svg?react";
import MoonIcon from "@/assets/icons/theme/moon.svg?react";
import { Link } from "@tanstack/react-router";
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLink,
  DropdownMenuLabel,
  DropdownMenuDivider,
} from "@/components/Menus/DropdownMenu";

import styles from "./NavBar.module.css";
import { useUserMenu } from "./useUserMenu";
import { useCurrentUser } from "@/hooks/useCurrentUser";

const UserMenu = () => {
  const {
    handleGuestLogin,
    handleLogin,
    handleLogout,
    setGuestName,
    guestError,
    guestLoading,
    showGuestInput,
    theme,
    toggleTheme,
    avatarContent,
    setShowGuestInput,
    guestName,
  } = useUserMenu();

  const userState = useCurrentUser();
  const user =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user
      : undefined;

  return (
    <DropdownMenu
      trigger={(toggle) => (
        <IconBtn
          className={styles.avatarBtn}
          shape='avatar'
          variant='primary'
          size='md'
          aria-label='User menu'
          icon={avatarContent()}
          onClick={toggle}
        />
      )}
      position='top-right'>
      {userState.state === "registered" ? (
        <>
          <DropdownMenuLabel>{user?.userName}</DropdownMenuLabel>
          <DropdownMenuLink>
            <Link to='/account'>Account</Link>
          </DropdownMenuLink>
          <DropdownMenuItem onClick={() => void handleLogout()}>
            Logout
          </DropdownMenuItem>
        </>
      ) : userState.state === "guest" ? (
        <>
          <DropdownMenuLabel>Guest: {user?.userName}</DropdownMenuLabel>
          <DropdownMenuItem onClick={handleLogin}>
            Sign in with Google
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => void handleLogout()}>
            Logout guest
          </DropdownMenuItem>
        </>
      ) : (
        <>
          <DropdownMenuItem onClick={handleLogin}>
            Login with Google
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() => {
              setShowGuestInput((prev) => !prev);
            }}>
            Play as guest
          </DropdownMenuItem>
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
              <DropdownMenuItem
                onClick={() => {
                  void handleGuestLogin();
                }}
                disabled={guestLoading}>
                Confirm
              </DropdownMenuItem>
              {guestError && (
                <span className={styles.guestError}>{guestError}</span>
              )}
            </div>
          )}
        </>
      )}
      <DropdownMenuDivider />
      <DropdownMenuItem onClick={toggleTheme} centered={true}>
        <span className={styles.themeToggle} aria-hidden='true'>
          <SunIcon
            className={
              theme === "dark" ? styles.themeIconActive : styles.themeIconHidden
            }
          />
          <MoonIcon
            className={
              theme === "light"
                ? styles.themeIconActive
                : styles.themeIconHidden
            }
          />
        </span>
      </DropdownMenuItem>
    </DropdownMenu>
  );
};

export { UserMenu };
