/**
 * Manages the STOMP/WebSocket connection for an active interactiveSession.
 * Subscribes to every interactiveSession topic, dispatches payloads into the Redux game
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
import type { InteractiveSessionDto } from "../store/BrainFlexApi";
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
        client.subscribe(`/topic/interactive-session/${roomCode}/lobby`, (msg) => {
          dispatch(setSession(JSON.parse(msg.body) as InteractiveSessionDto));
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/round`, (msg) => {
          dispatch(roundStarted(JSON.parse(msg.body) as RoundStartPayload));
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/roundResult`, (msg) => {
          dispatch(
            roundResultReceived(JSON.parse(msg.body) as RoundResultPayload),
          );
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/gameOver`, (msg) => {
          dispatch(gameOver(JSON.parse(msg.body) as GameOverPayload));
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/answered`, (msg) => {
          dispatch(
            answerProgressReceived(
              JSON.parse(msg.body) as AnswerProgressPayload,
            ),
          );
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/votePhase`, (msg) => {
          dispatch(
            votePhaseStarted(JSON.parse(msg.body) as VotePhaseStartPayload),
          );
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/voted`, (msg) => {
          dispatch(
            voteProgressReceived(JSON.parse(msg.body) as VoteProgressPayload),
          );
        });
        client.subscribe(`/topic/interactive-session/${roomCode}/wordCloud`, (msg) => {
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
      send(`/app/interactive-session/${roomCode}/start`);
    }, [roomCode, send]),

    /**
     * Submit a polymorphic answer for the current element. The payload's `kind`
     * discriminator picks the server-side scoring branch.
     */
    sendAnswer: useCallback(
      (elementId: string, payload: AnswerPayload) => {
        send(`/app/interactive-session/${roomCode}/answer`, { elementId, payload });
      },
      [roomCode, send],
    ),

    /** Cast a vote during the VOTE phase of a Best Answer round. */
    sendVote: useCallback(
      (elementId: string, submissionId: string) => {
        send(`/app/interactive-session/${roomCode}/vote`, { elementId, submissionId });
      },
      [roomCode, send],
    ),

    sendNextRound: useCallback(() => {
      send(`/app/interactive-session/${roomCode}/nextRound`);
    }, [roomCode, send]),

    sendLeave: useCallback(() => {
      send(`/app/interactive-session/${roomCode}/leave`);
    }, [roomCode, send]),

    sendBoot: useCallback(
      (userId: string) => {
        send(`/app/interactive-session/${roomCode}/boot`, { userId });
      },
      [roomCode, send],
    ),

    sendEndInteractiveSession: useCallback(() => {
      send(`/app/interactive-session/${roomCode}/end`);
    }, [roomCode, send]),
  };
}
