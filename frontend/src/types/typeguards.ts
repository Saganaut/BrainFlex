import type { GuestUser, RegisteredUser } from "../store/BrainFlexApi";

export function isRegisteredUser(
  user: RegisteredUser | GuestUser,
): user is RegisteredUser {
  return (
    typeof user === "object" &&
    user !== null &&
    "name" in user &&
    typeof (user as any).name === "string"
  );
}
