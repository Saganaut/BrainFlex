/**
 * Tiny pub/sub bridge between the RTK Query base-query layer (plain JS, no
 * React context) and the React tree. When a mutation/query returns 401, the
 * baseQuery wrapper in `emptyApi.ts` calls `emitAuthRequired`; a single
 * subscriber mounted near the root (`AuthPromptBridge`) listens and opens the
 * shared LoginModal via the ModalProvider.
 *
 * Kept deliberately minimal — one event, in-memory listeners only.
 */

interface AuthRequiredPayload {
  message?: string;
}

type Listener = (payload: AuthRequiredPayload) => void;

const listeners = new Set<Listener>();

export function emitAuthRequired(payload: AuthRequiredPayload = {}): void {
  listeners.forEach((fn) => {
    fn(payload);
  });
}

export function subscribeAuthRequired(fn: Listener): () => void {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}
