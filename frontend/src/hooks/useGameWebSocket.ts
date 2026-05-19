/**
 * Manages the STOMP/WebSocket connection for an active showcase.
 * Subscribes to every showcase topic, dispatches payloads into the Redux game
 * slice, and exposes helper functions for sending host/player actions.
 */
import { useEffect, useRef, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { apiBaseUrl } from "../store/emptyApi";
import { useAppDispatch } from "../store/hooks";
import {
  setSession,
  roundStarted,
  roundResultReceived,
  gameOver,
  wsErrorReceived,
  answerProgressReceived,
  presenceUpdated,
  votePhaseStarted,
  voteProgressReceived,
  wordCloudUpdated,
  type RoundStartPayload,
  type RoundResultPayload,
  type GameOverPayload,
  type WsErrorPayload,
  type AnswerProgressPayload,
  type PresencePayload,
  type WordCloudUpdatePayload,
} from "../store/gameSlice";
import type { ShowcaseDto } from "../store/BrainFlexApi";
import type { AnswerPayload } from "../types/elements";
import type {
  VotePhaseStartPayload,
  VoteProgressPayload,
} from "../types/bestAnswer";

export function useGameWebSocket(roomCode: string | null) {
  const dispatch = useAppDispatch();
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!roomCode) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws`),
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/showcase/${roomCode}/lobby`, (msg) => {
          dispatch(setSession(JSON.parse(msg.body) as ShowcaseDto));
        });
        client.subscribe(`/topic/showcase/${roomCode}/round`, (msg) => {
          dispatch(roundStarted(JSON.parse(msg.body) as RoundStartPayload));
        });
        client.subscribe(`/topic/showcase/${roomCode}/roundResult`, (msg) => {
          dispatch(
            roundResultReceived(JSON.parse(msg.body) as RoundResultPayload),
          );
        });
        client.subscribe(`/topic/showcase/${roomCode}/gameOver`, (msg) => {
          dispatch(gameOver(JSON.parse(msg.body) as GameOverPayload));
        });
        client.subscribe(`/topic/showcase/${roomCode}/answered`, (msg) => {
          dispatch(
            answerProgressReceived(
              JSON.parse(msg.body) as AnswerProgressPayload,
            ),
          );
        });
        client.subscribe(`/topic/showcase/${roomCode}/votePhase`, (msg) => {
          dispatch(
            votePhaseStarted(JSON.parse(msg.body) as VotePhaseStartPayload),
          );
        });
        client.subscribe(`/topic/showcase/${roomCode}/voted`, (msg) => {
          dispatch(
            voteProgressReceived(JSON.parse(msg.body) as VoteProgressPayload),
          );
        });
        client.subscribe(`/topic/showcase/${roomCode}/wordCloud`, (msg) => {
          dispatch(
            wordCloudUpdated(JSON.parse(msg.body) as WordCloudUpdatePayload),
          );
        });
        client.subscribe(`/topic/presence`, (msg) => {
          dispatch(presenceUpdated(JSON.parse(msg.body) as PresencePayload));
        });
        client.subscribe(`/user/queue/errors`, (msg) => {
          dispatch(wsErrorReceived(JSON.parse(msg.body) as WsErrorPayload));
        });
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      void client.deactivate();
      clientRef.current = null;
    };
  }, [roomCode, dispatch]);

  const send = useCallback((destination: string, body?: object) => {
    const client = clientRef.current;
    if (client?.connected) {
      client.publish({
        destination,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    }
  }, []);

  return {
    sendStart: useCallback(() => {
      send(`/app/showcase/${roomCode}/start`);
    }, [roomCode, send]),

    /**
     * Submit a polymorphic answer for the current element. The payload's `kind`
     * discriminator picks the server-side scoring branch.
     */
    sendAnswer: useCallback(
      (elementId: string, payload: AnswerPayload) => {
        send(`/app/showcase/${roomCode}/answer`, { elementId, payload });
      },
      [roomCode, send],
    ),

    /** Cast a vote during the VOTE phase of a Best Answer round. */
    sendVote: useCallback(
      (elementId: string, submissionId: string) => {
        send(`/app/showcase/${roomCode}/vote`, { elementId, submissionId });
      },
      [roomCode, send],
    ),

    sendNextRound: useCallback(() => {
      send(`/app/showcase/${roomCode}/nextRound`);
    }, [roomCode, send]),

    sendLeave: useCallback(() => {
      send(`/app/showcase/${roomCode}/leave`);
    }, [roomCode, send]),

    sendBoot: useCallback(
      (userId: string) => {
        send(`/app/showcase/${roomCode}/boot`, { userId });
      },
      [roomCode, send],
    ),

    sendEndShowcase: useCallback(() => {
      send(`/app/showcase/${roomCode}/end`);
    }, [roomCode, send]),
  };
}
