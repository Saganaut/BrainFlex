import { useGetCurrentUserQuery } from "../store/BrainFlexApi";
import type { RegisteredUser, GuestUser as Guest } from "../store/BrainFlexApi";

type AuthenticatedUser = { authenticated: true; user: RegisteredUser };
type GuestSession = { authenticated: false; user: Guest; isGuestSession: true };
type AnonymousUser = {
  authenticated: false;
  user: undefined;
  isGuestSession: false;
};

export type CurrentUser = AuthenticatedUser | GuestSession | AnonymousUser;

export function useCurrentUser() {
  const { data, isLoading, isError } = useGetCurrentUserQuery();

  const user = data;
  const authenticated = user?.isGuest === false;
  const isGuestSession = user?.isGuest === true && user?.id !== "0";

  return {
    isLoading,
    isError,
    authenticated,
    isGuestSession,
    user,
  };
}
