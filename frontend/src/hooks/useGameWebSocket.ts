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
  type RoundStartPayload,
  type RoundResultPayload,
  type GameOverPayload,
} from "../store/gameSlice";
import type { GameSessionDto } from "../store/BrainFlexApi";

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
        client.subscribe(`/topic/game/${roomCode}/lobby`, (msg) => {
          dispatch(setSession(JSON.parse(msg.body) as GameSessionDto));
        });

        // Round started: server sends the question (without correct answer)
        client.subscribe(`/topic/game/${roomCode}/round`, (msg) => {
          dispatch(roundStarted(JSON.parse(msg.body) as RoundStartPayload));
        });

        // Round ended: server reveals correct answer and scores
        client.subscribe(`/topic/game/${roomCode}/roundResult`, (msg) => {
          dispatch(
            roundResultReceived(JSON.parse(msg.body) as RoundResultPayload),
          );
        });

        // Game finished: server sends final placements
        client.subscribe(`/topic/game/${roomCode}/gameOver`, (msg) => {
          dispatch(gameOver(JSON.parse(msg.body) as GameOverPayload));
        });
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
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
    sendStart: useCallback(
      () => send(`/app/game/${roomCode}/start`),
      [roomCode, send],
    ),
    /** Player: submits the selected answer option for the current round. */
    sendAnswer: useCallback(
      (questionId: string, selectedOption: number) =>
        send(`/app/game/${roomCode}/answer`, { questionId, selectedOption }),
      [roomCode, send],
    ),
    /** Host (TURN_BASED): advances to the next question after reviewing the result. */
    sendNextRound: useCallback(
      () => send(`/app/game/${roomCode}/nextRound`),
      [roomCode, send],
    ),
    /** Any player: leaves the session and broadcasts updated lobby state. */
    sendLeave: useCallback(
      () => send(`/app/game/${roomCode}/leave`),
      [roomCode, send],
    ),
  };
}
