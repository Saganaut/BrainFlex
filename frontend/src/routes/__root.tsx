import { createRootRoute, Outlet } from "@tanstack/react-router";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import { AuthBar } from "../components/Common/AuthBar";

export const Route = createRootRoute({
  component: () => (
    <>
      <AuthBar />
      <Outlet />
      <TanStackRouterDevtools />
    </>
  ),
});
