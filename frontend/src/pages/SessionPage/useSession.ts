import { getRouteApi } from "@tanstack/react-router";

const routeApi = getRouteApi("/sessions/$sessionId/");

interface useSessionResponse {
  sessionId: string;
}

const useSession = (): useSessionResponse => {
  const { sessionId } = routeApi.useParams();

  return {
    sessionId,
  };
};

export { useSession };
