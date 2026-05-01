import { useGetCurrentUserQuery } from "../store/BrainFlexApi";
import type { RegisteredUser, GuestUser as Guest } from "../store/BrainFlexApi";

type AuthenticatedUser = { authenticated: true; user: RegisteredUser };
type GuestUser = { authenticated: false; user: Guest };

export type CurrentUser = AuthenticatedUser | GuestUser;

export function useCurrentUser() {
  const { data, isLoading, isError } = useGetCurrentUserQuery();

  const user = data;
  const authenticated = user?.isGuest === false;

  return {
    isLoading,
    isError,
    authenticated,
    user: user as CurrentUser["user"] | undefined,
  };
}
