import { emptySplitApi as api } from "./emptyApi";
const injectedRtkApi = api.injectEndpoints({
  endpoints: (build) => ({
    uploadProfileImage: build.mutation<
      UploadProfileImageApiResponse,
      UploadProfileImageApiArg
    >({
      query: (queryArg) => ({
        url: `/api/users/me/profile-image`,
        method: "POST",
        body: queryArg.body,
      }),
    }),
    closeAccount: build.mutation<CloseAccountApiResponse, CloseAccountApiArg>({
      query: () => ({ url: `/api/users/me/close`, method: "POST" }),
    }),
    createGame: build.mutation<CreateGameApiResponse, CreateGameApiArg>({
      query: (queryArg) => ({
        url: `/api/games`,
        method: "POST",
        body: queryArg.createGameRequest,
      }),
    }),
    joinByRoomCode: build.mutation<
      JoinByRoomCodeApiResponse,
      JoinByRoomCodeApiArg
    >({
      query: (queryArg) => ({
        url: `/api/games/${queryArg.roomCode}/join`,
        method: "POST",
      }),
    }),
    register: build.mutation<RegisterApiResponse, RegisterApiArg>({
      query: (queryArg) => ({
        url: `/api/auth/register`,
        method: "POST",
        body: queryArg.registerRequest,
      }),
    }),
    guestLogin: build.mutation<GuestLoginApiResponse, GuestLoginApiArg>({
      query: (queryArg) => ({
        url: `/api/auth/guest`,
        method: "POST",
        body: queryArg.guestLoginRequest,
      }),
    }),
    updateProfile: build.mutation<
      UpdateProfileApiResponse,
      UpdateProfileApiArg
    >({
      query: (queryArg) => ({
        url: `/api/users/me`,
        method: "PATCH",
        body: queryArg.updateProfileRequest,
      }),
    }),
    getUserProfile: build.query<
      GetUserProfileApiResponse,
      GetUserProfileApiArg
    >({
      query: (queryArg) => ({ url: `/api/users/${queryArg.id}` }),
    }),
    getLeaderboard: build.query<
      GetLeaderboardApiResponse,
      GetLeaderboardApiArg
    >({
      query: (queryArg) => ({
        url: `/api/users/leaderboard`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
    }),
    checkUsername: build.query<CheckUsernameApiResponse, CheckUsernameApiArg>({
      query: (queryArg) => ({
        url: `/api/users/check-username`,
        params: {
          username: queryArg.username,
        },
      }),
    }),
    getHealth: build.query<GetHealthApiResponse, GetHealthApiArg>({
      query: () => ({ url: `/api/health` }),
    }),
    getSession: build.query<GetSessionApiResponse, GetSessionApiArg>({
      query: (queryArg) => ({ url: `/api/games/${queryArg.roomCode}` }),
    }),
    cancelGame: build.mutation<CancelGameApiResponse, CancelGameApiArg>({
      query: (queryArg) => ({
        url: `/api/games/${queryArg.roomCode}`,
        method: "DELETE",
      }),
    }),
    getResults: build.query<GetResultsApiResponse, GetResultsApiArg>({
      query: (queryArg) => ({ url: `/api/games/${queryArg.roomCode}/results` }),
    }),
    getByInviteToken: build.query<
      GetByInviteTokenApiResponse,
      GetByInviteTokenApiArg
    >({
      query: (queryArg) => ({ url: `/api/games/join/${queryArg.inviteToken}` }),
    }),
    listPacks: build.query<ListPacksApiResponse, ListPacksApiArg>({
      query: () => ({ url: `/api/content-packs` }),
    }),
    getPack: build.query<GetPackApiResponse, GetPackApiArg>({
      query: (queryArg) => ({ url: `/api/content-packs/${queryArg.id}` }),
    }),
    getCurrentUser: build.query<
      GetCurrentUserApiResponse,
      GetCurrentUserApiArg
    >({
      query: () => ({ url: `/api/auth/me` }),
    }),
    login: build.query<LoginApiResponse, LoginApiArg>({
      query: (queryArg) => ({
        url: `/api/auth/login`,
        params: {
          returnUrl: queryArg.returnUrl,
          guestId: queryArg.guestId,
        },
      }),
    }),
  }),
  overrideExisting: false,
});
export { injectedRtkApi as BrainFlex };
export type UploadProfileImageApiResponse = /** status 200 OK */ RegisteredUser;
export type UploadProfileImageApiArg = {
  body: {
    image: Blob;
  };
};
export type CloseAccountApiResponse = unknown;
export type CloseAccountApiArg = void;
export type CreateGameApiResponse = /** status 200 OK */ GameSessionDto;
export type CreateGameApiArg = {
  createGameRequest: CreateGameRequest;
};
export type JoinByRoomCodeApiResponse = /** status 200 OK */ GameSessionDto;
export type JoinByRoomCodeApiArg = {
  roomCode: string;
};
export type RegisterApiResponse = /** status 200 OK */ RegisteredUser;
export type RegisterApiArg = {
  registerRequest: RegisterRequest;
};
export type GuestLoginApiResponse = /** status 200 OK */ GuestUser;
export type GuestLoginApiArg = {
  guestLoginRequest: GuestLoginRequest;
};
export type UpdateProfileApiResponse = /** status 200 OK */ RegisteredUser;
export type UpdateProfileApiArg = {
  updateProfileRequest: UpdateProfileRequest;
};
export type GetUserProfileApiResponse = /** status 200 OK */ RegisteredUser;
export type GetUserProfileApiArg = {
  id: string;
};
export type GetLeaderboardApiResponse = /** status 200 OK */ GuestUser[];
export type GetLeaderboardApiArg = {
  page?: number;
  size?: number;
};
export type CheckUsernameApiResponse = /** status 200 OK */ {
  [key: string]: boolean;
};
export type CheckUsernameApiArg = {
  username: string;
};
export type GetHealthApiResponse = /** status 200 OK */ HealthCheckResponse;
export type GetHealthApiArg = void;
export type GetSessionApiResponse = /** status 200 OK */ GameSessionDto;
export type GetSessionApiArg = {
  roomCode: string;
};
export type CancelGameApiResponse = unknown;
export type CancelGameApiArg = {
  roomCode: string;
};
export type GetResultsApiResponse = /** status 200 OK */ GameResult;
export type GetResultsApiArg = {
  roomCode: string;
};
export type GetByInviteTokenApiResponse = /** status 200 OK */ GameSessionDto;
export type GetByInviteTokenApiArg = {
  inviteToken: string;
};
export type ListPacksApiResponse = /** status 200 OK */ ContentPackDto[];
export type ListPacksApiArg = void;
export type GetPackApiResponse = /** status 200 OK */ ContentPackDto;
export type GetPackApiArg = {
  id: string;
};
export type GetCurrentUserApiResponse = /** status 200 OK */ UserDto;
export type GetCurrentUserApiArg = void;
export type LoginApiResponse = unknown;
export type LoginApiArg = {
  returnUrl?: string;
  guestId?: string;
};
export type PlayerStats = {
  gamesPlayed?: number;
  highScore?: number;
  totalPoints?: number;
  currentStreak?: number;
};
export type RegisteredUser = {
  id?: string;
  email?: string;
  name?: string;
  userName?: string;
  isGuest?: boolean;
  googleId?: string;
  pictureUrl?: string;
  stats?: PlayerStats;
  newsletter?: boolean;
  lastLogin?: string;
  createdAt?: string;
};
export type GameSettings = {
  maxPlayers?: number;
  totalRounds?: number;
  timePerQuestion?: number;
  speedBonus?: boolean;
  allowGuests?: boolean;
  gameMode?: "SIMULTANEOUS" | "TURN_BASED";
};
export type SessionPlayerDto = {
  userId?: string;
  userName?: string;
  pictureUrl?: string;
  isGuest?: boolean;
  score?: number;
};
export type GameSessionDto = {
  id?: string;
  roomCode?: string;
  inviteToken?: string;
  type?: "TRIVIA" | "IMAGE" | "WORD";
  status?: "LOBBY" | "IN_PROGRESS" | "RESULTS" | "FINISHED" | "CANCELLED";
  hostUserId?: string;
  contentPackId?: string;
  settings?: GameSettings;
  players?: SessionPlayerDto[];
  currentRound?: number;
  createdAt?: string;
  startedAt?: string;
};
export type CreateGameRequest = {
  contentPackId: string;
  gameMode?: "SIMULTANEOUS" | "TURN_BASED";
  totalRounds?: number;
  timePerQuestion?: number;
  speedBonus?: boolean;
  allowGuests?: boolean;
  maxPlayers?: number;
};
export type RegisterRequest = {
  username: string;
  newsletter?: boolean;
};
export type GuestUser = {
  id?: string;
  userName?: string;
  isGuest?: boolean;
  pictureUrl?: string;
  stats?: PlayerStats;
};
export type GuestLoginRequest = {
  username?: string;
};
export type UpdateProfileRequest = {
  pictureUrl?: string;
  newsletter?: boolean;
};
export type HealthCheckResponse = {
  status?: string;
  message?: string;
  timestamp?: string;
  database?: string;
  redis?: string;
};
export type PlayerPlacement = {
  userId?: string;
  userName?: string;
  finalScore?: number;
  placement?: number;
  correctAnswers?: number;
  totalQuestions?: number;
  guest?: boolean;
};
export type GameResult = {
  id?: string;
  gameSessionId?: string;
  placements?: PlayerPlacement[];
  endedAt?: string;
};
export type ContentPackDto = {
  id?: string;
  name?: string;
  description?: string;
  category?: string;
  questionCount?: number;
  isSystem?: boolean;
  createdAt?: string;
};
export type UserDto = GuestUser | RegisteredUser;
export const {
  useUploadProfileImageMutation,
  useCloseAccountMutation,
  useCreateGameMutation,
  useJoinByRoomCodeMutation,
  useRegisterMutation,
  useGuestLoginMutation,
  useUpdateProfileMutation,
  useGetUserProfileQuery,
  useLazyGetUserProfileQuery,
  useGetLeaderboardQuery,
  useLazyGetLeaderboardQuery,
  useCheckUsernameQuery,
  useLazyCheckUsernameQuery,
  useGetHealthQuery,
  useLazyGetHealthQuery,
  useGetSessionQuery,
  useLazyGetSessionQuery,
  useCancelGameMutation,
  useGetResultsQuery,
  useLazyGetResultsQuery,
  useGetByInviteTokenQuery,
  useLazyGetByInviteTokenQuery,
  useListPacksQuery,
  useLazyListPacksQuery,
  useGetPackQuery,
  useLazyGetPackQuery,
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLoginQuery,
  useLazyLoginQuery,
} = injectedRtkApi;
