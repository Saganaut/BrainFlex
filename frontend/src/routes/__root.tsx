import { createRootRouteWithContext, Outlet } from "@tanstack/react-router";
import { TanStackRouterDevtools } from "@tanstack/react-router-devtools";
import { NavBar } from "../components/Nav/NavBar/NavBar";
import { LayoutProvider } from "../context/LayoutProvider";
import { ModalProvider } from "../context/ModalProvider";
import { ToastProvider } from "../context/ToastProvider";
import { NotFoundPage } from "../pages/ErrorPage/ErrorPage";
import { AuthPromptBridge } from "../components/Common/LoginModal/AuthPromptBridge";
import { ActiveThemeBridge } from "../components/Common/ActiveThemeBridge";
import { Layout } from "@/components/Layout/Layout";
import { MainHeader } from "@/components/Layout/MainHeader";
import type { CurrentUserState } from "@/hooks/useCurrentUser";

export interface RouterContext {
  auth: CurrentUserState;
}

export const Route = createRootRouteWithContext<RouterContext>()({
  component: () => (
    <LayoutProvider>
      <ToastProvider>
        <ModalProvider>
          <Layout>
            <AuthPromptBridge />
            <ActiveThemeBridge />
            <MainHeader children={<NavBar />} />

            <Outlet />
          </Layout>
          <TanStackRouterDevtools />
        </ModalProvider>
      </ToastProvider>
    </LayoutProvider>
  ),
  notFoundComponent: NotFoundPage,
});
