// The dedicated /games hub has been retired; MainPage at / is the entry point.
// Anything still linking to /games gets sent home.
import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/games/")({
  beforeLoad: () => {
    // eslint-disable-next-line @typescript-eslint/only-throw-error -- TanStack Router uses throw-redirect as its idiom
    throw redirect({ to: "/" });
  },
});
