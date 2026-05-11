/**
 * Manages the STOMP/WebSocket connection for an active game session.
 * Subscribes to all four game topics, dispatches the payloads into the
 * Redux game slice, and exposes helper functions for sending player actions.
 * The connection is established on mount and torn down on unmount.
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
  type RoundStartPayload,
  type RoundResultPayload,
  type GameOverPayload,
  type WsErrorPayload,
  type AnswerProgressPayload,
  type PresencePayload,
} from "../store/gameSlice";
import type { ShowcaseDto } from "../store/BrainFlexApi";

export function useGameWebSocket(roomCode: string | null) {
  const dispatch = useAppDispatch();
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!roomCode) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/ws`),
      reconnectDelay: 3000,
      onConnect: () => {
        // Lobby state: player joins/leaves, session status changes
        client.subscribe(`/topic/showcase/${roomCode}/lobby`, (msg) => {
          dispatch(setSession(JSON.parse(msg.body) as ShowcaseDto));
        });

        // Round started: server sends the question (without correct answer)
        client.subscribe(`/topic/showcase/${roomCode}/round`, (msg) => {
          dispatch(roundStarted(JSON.parse(msg.body) as RoundStartPayload));
        });

        // Round ended: server reveals correct answer and scores
        client.subscribe(`/topic/showcase/${roomCode}/roundResult`, (msg) => {
          dispatch(
            roundResultReceived(JSON.parse(msg.body) as RoundResultPayload),
          );
        });

        // Game finished: server sends final placements
        client.subscribe(`/topic/showcase/${roomCode}/gameOver`, (msg) => {
          dispatch(gameOver(JSON.parse(msg.body) as GameOverPayload));
        });

        // Live answer progress: server pushes the userIds of players who've answered
        // the current round so the player list can show ✓ next to them in real time.
        client.subscribe(`/topic/showcase/${roomCode}/answered`, (msg) => {
          dispatch(
            answerProgressReceived(JSON.parse(msg.body) as AnswerProgressPayload),
          );
        });

        // Global presence: per-user connect/disconnect transitions so the player
        // list can dim disconnected participants.
        client.subscribe(`/topic/presence`, (msg) => {
          dispatch(presenceUpdated(JSON.parse(msg.body) as PresencePayload));
        });

        // Per-user errors from failed @MessageMapping handlers (e.g. Start with
        // an empty deck). Spring routes /user/queue/errors to this principal only.
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

  // Sends a STOMP message if the connection is active
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
    /** Host: transitions session from LOBBY → IN_PROGRESS and fires first question. */
    sendStart: useCallback(() => {
      send(`/app/showcase/${roomCode}/start`);
    }, [roomCode, send]),
    /**
     * Player: submits an answer for the current round.
     * For MULTIPLE_CHOICE pass selectedOption (0-based index). For TEXT_INPUT pass textAnswer.
     * The unused field is filled with a sentinel so the server schema stays uniform.
     */
    sendAnswer: useCallback(
      (questionId: string, payload: { selectedOption?: number; textAnswer?: string }) => {
        send(`/app/showcase/${roomCode}/answer`, {
          questionId,
          selectedOption: payload.selectedOption ?? -1,
          textAnswer: payload.textAnswer ?? null,
        });
      },
      [roomCode, send],
    ),
    /** Host (TURN_BASED): advances to the next question after reviewing the result. */
    sendNextRound: useCallback(() => {
      send(`/app/showcase/${roomCode}/nextRound`);
    }, [roomCode, send]),
    /** Any player: leaves the session and broadcasts updated lobby state. */
    sendLeave: useCallback(() => {
      send(`/app/showcase/${roomCode}/leave`);
    }, [roomCode, send]),
    /** Host: removes a specific player from the showcase. */
    sendBoot: useCallback(
      (userId: string) => {
        send(`/app/showcase/${roomCode}/boot`, { userId });
      },
      [roomCode, send],
    ),
    /** Host: ends the showcase mid-game and triggers the gameOver broadcast. */
    sendEndShowcase: useCallback(() => {
      send(`/app/showcase/${roomCode}/end`);
    }, [roomCode, send]),
  };
}
