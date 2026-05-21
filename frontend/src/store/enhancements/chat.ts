/**
 * Audience-chat cache patches: optimistic append for the sender, moderation
 * flip for the host. STOMP /chat broadcasts reconcile both for every other
 * viewer; these handlers keep the mutating client's own panel smooth even
 * if its socket is briefly disconnected.
 *
 * - `optimisticSendChat` appends a synthetic message into every cached page
 *   of `listChat` for the room so the panel renders instantly; on fulfill
 *   the STOMP /chat broadcast (handled in the slice) carries the canonical
 *   row, and the optimistic id is replaced when the real id arrives. On
 *   reject we drop the synthetic message so a failed send doesn't strand
 *   a phantom row.
 *
 * - `optimisticModerateChat` patches the moderated flag on a chat row across
 *   every cached `listChat` page for the room when the host hides a
 *   message. The server also rebroadcasts on STOMP /chat, so this is
 *   mostly belt-and-suspenders for the caller-side cache (the moderating
 *   host's own panel).
 *
 * Imported for its side effect via the `../apiEnhancements` barrel.
 */
import {
  BrainFlex,
  type InteractiveSessionChatMessageDto,
  type ListChatApiArg,
} from "../BrainFlexApi";
import type { WithApiQueries } from "./types";

interface ChatSendApi {
  dispatch: (action: unknown) => unknown;
  getState: () => WithApiQueries;
  queryFulfilled: Promise<{ data: InteractiveSessionChatMessageDto }>;
}

const optimisticSendChat = async (
  arg: { roomCode: string; chatSendRequest: { body: string } },
  api: ChatSendApi,
) => {
  const tempId = `tmp-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  const optimistic: InteractiveSessionChatMessageDto = {
    id: tempId,
    body: arg.chatSendRequest.body,
    sentAt: new Date().toISOString(),
    moderated: false,
  };
  const patches: { undo: () => void }[] = [];
  const queries = api.getState().api?.queries ?? {};
  for (const entry of Object.values(queries)) {
    if (entry?.endpointName !== "listChat") continue;
    const queryArg = (entry.originalArgs ?? {}) as ListChatApiArg;
    if (queryArg.roomCode !== arg.roomCode) continue;
    patches.push(
      api.dispatch(
        BrainFlex.util.updateQueryData("listChat", queryArg, (draft) => {
          draft.push(optimistic);
        }),
      ) as { undo: () => void },
    );
  }
  try {
    const { data } = await api.queryFulfilled;
    for (const entry of Object.values(queries)) {
      if (entry?.endpointName !== "listChat") continue;
      const queryArg = (entry.originalArgs ?? {}) as ListChatApiArg;
      if (queryArg.roomCode !== arg.roomCode) continue;
      api.dispatch(
        BrainFlex.util.updateQueryData("listChat", queryArg, (draft) => {
          const idx = draft.findIndex((m) => m.id === tempId);
          if (idx >= 0) draft[idx] = data;
          else draft.push(data);
        }),
      );
    }
  } catch {
    for (const p of patches) p.undo();
  }
};

const optimisticModerateChat = async (
  arg: { roomCode: string; messageId: string },
  api: ChatSendApi,
) => {
  const patches: { undo: () => void }[] = [];
  const queries = api.getState().api?.queries ?? {};
  for (const entry of Object.values(queries)) {
    if (entry?.endpointName !== "listChat") continue;
    const queryArg = (entry.originalArgs ?? {}) as ListChatApiArg;
    if (queryArg.roomCode !== arg.roomCode) continue;
    patches.push(
      api.dispatch(
        BrainFlex.util.updateQueryData("listChat", queryArg, (draft) => {
          const row = draft.find((m) => m.id === arg.messageId);
          if (row) row.moderated = true;
        }),
      ) as { undo: () => void },
    );
  }
  try {
    const { data } = await api.queryFulfilled;
    for (const entry of Object.values(queries)) {
      if (entry?.endpointName !== "listChat") continue;
      const queryArg = (entry.originalArgs ?? {}) as ListChatApiArg;
      if (queryArg.roomCode !== arg.roomCode) continue;
      api.dispatch(
        BrainFlex.util.updateQueryData("listChat", queryArg, (draft) => {
          const idx = draft.findIndex((m) => m.id === arg.messageId);
          if (idx >= 0) draft[idx] = data;
        }),
      );
    }
  } catch {
    for (const p of patches) p.undo();
  }
};

BrainFlex.enhanceEndpoints({
  endpoints: {
    sendChat: {
      onQueryStarted: (arg, api) => optimisticSendChat(arg, api),
    },
    moderateChat: {
      onQueryStarted: (arg, api) => optimisticModerateChat(arg, api),
    },
  },
});
