import { createRootRoute, Outlet } from "@tanstack/react-router";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import { NavBar } from "../components/Nav/NavBar/NavBar";
import { LayoutProvider } from "../context/LayoutProvider";
import { ModalProvider } from "../context/ModalProvider";
import { ToastProvider } from "../context/ToastProvider";
import { NotFoundPage } from "../pages/ErrorPage/ErrorPage";
import { AuthPromptBridge } from "../components/Common/LoginModal/AuthPromptBridge";

export const Route = createRootRoute({
  component: () => (
    <LayoutProvider>
      <ToastProvider>
        <ModalProvider>
          <AuthPromptBridge />
          <NavBar />
          <Outlet />
          <TanStackRouterDevtools />
        </ModalProvider>
      </ToastProvider>
    </LayoutProvider>
  ),
  notFoundComponent: NotFoundPage,
});
