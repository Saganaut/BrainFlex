import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../../hooks/useCurrentUser";

import styles from "./NavBar.module.css";
import { UserMenu } from "./UserMenu";

export function NavBar() {
  const userState = useCurrentUser();

  if (userState.state === "loading") {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }
  return (
    <div className={styles.navContainer}>
      <div className={styles.userMenuWrapper}>
        <Link to='/design-system' viewTransition>
          Design system
        </Link>
        <Link to='/games/create' viewTransition>
          New Game
        </Link>
        {userState.state === "registered" && (
          <Link to='/my-packs' viewTransition>
            My Packs
          </Link>
        )}
        <UserMenu />
      </div>
    </div>
  );
}
