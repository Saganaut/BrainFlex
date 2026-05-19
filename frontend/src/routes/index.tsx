// Home route. Accepts optional `authPrompt` + `returnUrl` search params that
// the /_authenticated layout sets when redirecting an unauthenticated user;
// MainPage reads them and auto-opens the LoginModal so the user lands at "/"
// already being asked to sign in, rather than bouncing silently.
import { createFileRoute } from "@tanstack/react-router";

import { MainPage } from "../pages/MainPage/MainPage";

interface IndexSearch {
  authPrompt?: boolean;
  returnUrl?: string;
}

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>): IndexSearch => ({
    authPrompt: search.authPrompt === true || search.authPrompt === "true",
    returnUrl:
      typeof search.returnUrl === "string" ? search.returnUrl : undefined,
  }),
  component: MainPage,
});
