/**
 * Typed selector hook that returns the full game session state from the Redux store.
 * Components import this instead of calling useAppSelector directly so the
 * game slice shape is accessed from one place.
 */
import { useAppSelector } from "../store/hooks";

export function useGameSession() {
  return useAppSelector((state) => state.game);
}
