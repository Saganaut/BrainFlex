import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../../hooks/useCurrentUser";

import styles from "./NavBar.module.css";
import { UserMenu } from "./UserMenu";

export function NavBar() {
  const userState = useCurrentUser();
  const user =
    userState.state === "registered" || userState.state === "guest"
      ? userState.user
      : undefined;

  if (userState.state === "loading") {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }
  console.log("user picture", user?.pictureUrl);
  return (
    <div className={styles.navContainer}>
      <div className={styles.userMenuWrapper}>
        <Link to='/design-system' viewTransition>
          Design system
        </Link>
        <Link to='/games' viewTransition>
          Games
        </Link>
        <Link to='/games' viewTransition>
          Landing Page
        </Link>
        <UserMenu />
      </div>
    </div>
  );
}
