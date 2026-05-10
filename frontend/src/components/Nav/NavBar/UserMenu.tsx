import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import React from "react";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input";
import lightModeIcon from "@/assets/nav/LightModeIcon.svg";
import darkModeIcon from "@/assets/nav/DarkModeIcon.svg";
import { Link } from "@tanstack/react-router";

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
    dropdownOpen,
    showGuestInput,
    theme,
    toggleTheme,
    avatarContent,
    setDropdownOpen,
    setShowGuestInput,
    guestName,
  } = useUserMenu();

  const userState = useCurrentUser();
  const user =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user
      : undefined;
  return (
    <div>
      {" "}
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
              <Btn className={styles.dropdownItem} onClick={void handleLogout}>
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
              <Btn className={styles.dropdownItem} onClick={void handleLogout}>
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
  );
};

export { UserMenu };
