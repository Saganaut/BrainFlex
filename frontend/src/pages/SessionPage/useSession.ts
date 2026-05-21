import type { DeckDto, InteractiveSessionDto } from "@/store/BrainFlexApi";
import { getRouteApi } from "@tanstack/react-router";
import {
  mockFellowshipSession,
  mockFellowshipTriviaDeck,
} from "@/utils/MockData";

const routeApi = getRouteApi("/sessions/$sessionId/");

interface useSessionResponse {
  sessionId: string;
  interactiveSession: InteractiveSessionDto;
  currentDeck: DeckDto;
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
