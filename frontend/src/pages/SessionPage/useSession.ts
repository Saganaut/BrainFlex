import type {
  DeckResponse,
  InteractiveSessionResponse,
} from "@/store/BrainFlexApi";
import { getRouteApi } from "@tanstack/react-router";
import {
  mockFellowshipSession,
  mockFellowshipTriviaDeck,
} from "@/utils/MockData";

const routeApi = getRouteApi("/sessions/$sessionId/");

interface useSessionResponse {
  sessionId: string;
  interactiveSession: InteractiveSessionResponse;
  currentDeck: DeckResponse;
}

const useSession = (): useSessionResponse => {
  const { sessionId } = routeApi.useParams();
  const interactiveSession = mockFellowshipSession;
  const currentDeck = mockFellowshipTriviaDeck;
  return {
    sessionId,
    interactiveSession,
    currentDeck,
  };
};

export { useSession };
