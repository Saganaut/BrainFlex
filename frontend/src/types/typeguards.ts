import type { GuestUser, RegisteredUser } from "../store/BrainFlexApi";

/** Right now just checking for name but consider using more detailed typeguards as necessary **/
export function isRegisteredUser(
  user: RegisteredUser | GuestUser,
): user is RegisteredUser {
  return typeof user === "object" && "name" in user;
}
