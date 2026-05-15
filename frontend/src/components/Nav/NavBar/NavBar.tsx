import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import styles from "./NavBar.module.css";
import { UserMenu } from "./UserMenu";
import { CephadexLogo } from "@/components/Graphic/CephadexLogo";

export function NavBar() {
  const userState = useCurrentUser();

  if (userState.state === "loading") {
    return <div style={{ padding: "1rem" }}>Loading auth...</div>;
  }
  return (
    <div className={styles.navContainer}>
      <div className={styles.homeMenuWrapper}>
        <Link to='/' viewTransition>
          <CephadexLogo />
        </Link>
      </div>
      <div className={styles.userMenuWrapper}>
        <Link to='/design-system' viewTransition>
          Design system
        </Link>
        <Link to='/pricing' viewTransition>
          Pricing
        </Link>
        <Link to='/games/create' viewTransition>
          New Game
        </Link>
        {userState.state === "registered" && (
          <Link to='/decks' viewTransition>
            My Decks
          </Link>
        )}
        <UserMenu />
      </div>
    </div>
  );
}
