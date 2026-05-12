/**
 * Mounted once near the root of the app. Listens to the auth-prompt event bus
 * fed by the RTK Query baseQuery wrapper (`store/emptyApi.ts`) and opens the
 * shared LoginModal when a request comes back 401. Throttled so a burst of
 * 401s (e.g. several queries firing on the same page) only opens one modal.
 */
import { useEffect, useRef } from "react";
import { useRequireLogin } from "@/hooks/useRequireLogin";
import { subscribeAuthRequired } from "@/store/authPromptBus";

const AuthPromptBridge = () => {
  const { openLoginModal, isAuthenticated } = useRequireLogin();
  const lastOpenedAt = useRef(0);

  useEffect(() => {
    return subscribeAuthRequired(({ message }) => {
      if (isAuthenticated) return;
      const now = Date.now();
      if (now - lastOpenedAt.current < 500) return;
      lastOpenedAt.current = now;
      openLoginModal({ message });
    });
  }, [openLoginModal, isAuthenticated]);

  return null;
};

export { AuthPromptBridge };
