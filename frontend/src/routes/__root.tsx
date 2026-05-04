import { createRootRoute, Outlet } from "@tanstack/react-router";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import { NavBar } from "../components/Nav/NavBar";
import { ModalProvider } from "../context/ModalProvider";
import { ToastProvider } from "../context/ToastProvider";
import { NotFoundPage } from "../pages/ErrorPage";

export const Route = createRootRoute({
  component: () => (
    <ToastProvider>
      <ModalProvider>
        <NavBar />
        <Outlet />
        <TanStackRouterDevtools />
      </ModalProvider>
    </ToastProvider>
  ),
  notFoundComponent: NotFoundPage,
});
