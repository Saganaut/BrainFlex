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

interface useInteractiveSessionResponse {
  sessionId: string;
  interactiveSession: InteractiveSessionResponse;
  currentDeck: DeckResponse;
}

const useInteractiveSession = (): useInteractiveSessionResponse => {
  const { sessionId } = routeApi.useParams();
  const interactiveSession = mockFellowshipSession;
  const currentDeck = mockFellowshipTriviaDeck;
  return {
    sessionId,
    interactiveSession,
    currentDeck,
  };
};

export { useInteractiveSession };
