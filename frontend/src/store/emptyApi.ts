// Or from '@reduxjs/toolkit/query' if not using the auto-generated hooks
import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from "@reduxjs/toolkit/query/react";
import { emitAuthRequired } from "./authPromptBus";

export const apiBaseUrl: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

const rawBaseQuery = fetchBaseQuery({
  baseUrl: apiBaseUrl,
  credentials: "include",
});

// `/api/auth/me` is the optional-auth probe — a 401 there just means "visitor",
// not "the user tried to do something they aren't allowed to do", so we don't
// surface the login modal for it.
const isAuthProbe = (args: string | FetchArgs): boolean => {
  const url = typeof args === "string" ? args : args.url;
  return url.endsWith("/api/auth/me");
};

const baseQueryWithAuthPrompt: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  const result = await rawBaseQuery(args, api, extraOptions);
  if (result.error?.status === 401 && !isAuthProbe(args)) {
    emitAuthRequired({ message: "Please sign in to continue." });
  }
  return result;
};

// initialize an empty api service that we'll inject endpoints into later as needed
export const emptySplitApi = createApi({
  baseQuery: baseQueryWithAuthPrompt,
  endpoints: () => ({}),
});
