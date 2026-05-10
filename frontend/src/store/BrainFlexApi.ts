import { emptySplitApi as api } from "./emptyApi";
const injectedRtkApi = api.injectEndpoints({
  endpoints: (build) => ({
    updateTheme: build.mutation<UpdateThemeApiResponse, UpdateThemeApiArg>({
      query: (queryArg) => ({
        url: `/api/themes/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateThemeRequest,
      }),
    }),
    deleteTheme: build.mutation<DeleteThemeApiResponse, DeleteThemeApiArg>({
      query: (queryArg) => ({
        url: `/api/themes/${queryArg.id}`,
        method: "DELETE",
      }),
    }),
    getPack: build.query<GetPackApiResponse, GetPackApiArg>({
      query: (queryArg) => ({ url: `/api/content-packs/${queryArg.id}` }),
    }),
    updatePack: build.mutation<UpdatePackApiResponse, UpdatePackApiArg>({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateContentPackRequest,
      }),
    }),
    deletePack: build.mutation<DeletePackApiResponse, DeletePackApiArg>({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}`,
        method: "DELETE",
      }),
    }),
    updateQuestion: build.mutation<
      UpdateQuestionApiResponse,
      UpdateQuestionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}/questions/${queryArg.questionId}`,
        method: "PUT",
        body: queryArg.upsertQuestionRequest,
      }),
    }),
    deleteQuestion: build.mutation<
      DeleteQuestionApiResponse,
      DeleteQuestionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}/questions/${queryArg.questionId}`,
        method: "DELETE",
      }),
    }),
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
    listThemes: build.query<ListThemesApiResponse, ListThemesApiArg>({
      query: () => ({ url: `/api/themes` }),
    }),
    createTheme: build.mutation<CreateThemeApiResponse, CreateThemeApiArg>({
      query: (queryArg) => ({
        url: `/api/themes`,
        method: "POST",
        body: queryArg.createThemeRequest,
      }),
    }),
    uploadLogo: build.mutation<UploadLogoApiResponse, UploadLogoApiArg>({
      query: (queryArg) => ({
        url: `/api/themes/${queryArg.id}/logo`,
        method: "POST",
        body: queryArg.body,
      }),
    }),
    uploadBackground: build.mutation<
      UploadBackgroundApiResponse,
      UploadBackgroundApiArg
    >({
      query: (queryArg) => ({
        url: `/api/themes/${queryArg.id}/background`,
        method: "POST",
        body: queryArg.body,
      }),
    }),
    createOrg: build.mutation<CreateOrgApiResponse, CreateOrgApiArg>({
      query: (queryArg) => ({
        url: `/api/organizations`,
        method: "POST",
        body: queryArg.createOrganizationRequest,
      }),
    }),
    joinOrg: build.mutation<JoinOrgApiResponse, JoinOrgApiArg>({
      query: (queryArg) => ({
        url: `/api/organizations/join`,
        method: "POST",
        body: queryArg.joinOrganizationRequest,
      }),
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
    listPacks: build.query<ListPacksApiResponse, ListPacksApiArg>({
      query: () => ({ url: `/api/content-packs` }),
    }),
    createPack: build.mutation<CreatePackApiResponse, CreatePackApiArg>({
      query: (queryArg) => ({
        url: `/api/content-packs`,
        method: "POST",
        body: queryArg.createContentPackRequest,
      }),
    }),
    listQuestions: build.query<ListQuestionsApiResponse, ListQuestionsApiArg>({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}/questions`,
      }),
    }),
    addQuestion: build.mutation<AddQuestionApiResponse, AddQuestionApiArg>({
      query: (queryArg) => ({
        url: `/api/content-packs/${queryArg.id}/questions`,
        method: "POST",
        body: queryArg.upsertQuestionRequest,
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
    getMyOrg: build.query<GetMyOrgApiResponse, GetMyOrgApiArg>({
      query: () => ({ url: `/api/organizations/me` }),
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
    listMyPacks: build.query<ListMyPacksApiResponse, ListMyPacksApiArg>({
      query: () => ({ url: `/api/content-packs/mine` }),
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
    leaveOrg: build.mutation<LeaveOrgApiResponse, LeaveOrgApiArg>({
      query: () => ({ url: `/api/organizations/me/leave`, method: "DELETE" }),
    }),
  }),
  overrideExisting: false,
});
export { injectedRtkApi as BrainFlex };
export type UpdateThemeApiResponse = /** status 200 OK */ ThemeResponse;
export type UpdateThemeApiArg = {
  id: string;
  updateThemeRequest: UpdateThemeRequest;
};
export type DeleteThemeApiResponse = unknown;
export type DeleteThemeApiArg = {
  id: string;
};
export type GetPackApiResponse = /** status 200 OK */ ContentPackDto;
export type GetPackApiArg = {
  id: string;
};
export type UpdatePackApiResponse = /** status 200 OK */ ContentPackDto;
export type UpdatePackApiArg = {
  id: string;
  updateContentPackRequest: UpdateContentPackRequest;
};
export type DeletePackApiResponse = unknown;
export type DeletePackApiArg = {
  id: string;
};
export type UpdateQuestionApiResponse = /** status 200 OK */ QuestionEditorDto;
export type UpdateQuestionApiArg = {
  id: string;
  questionId: string;
  upsertQuestionRequest: UpsertQuestionRequest;
};
export type DeleteQuestionApiResponse = unknown;
export type DeleteQuestionApiArg = {
  id: string;
  questionId: string;
};
export type UploadProfileImageApiResponse = /** status 200 OK */ RegisteredUser;
export type UploadProfileImageApiArg = {
  body: {
    image: Blob;
  };
};
export type CloseAccountApiResponse = unknown;
export type CloseAccountApiArg = void;
export type ListThemesApiResponse = /** status 200 OK */ ThemeResponse[];
export type ListThemesApiArg = void;
export type CreateThemeApiResponse = /** status 200 OK */ ThemeResponse;
export type CreateThemeApiArg = {
  createThemeRequest: CreateThemeRequest;
};
export type UploadLogoApiResponse = /** status 200 OK */ ThemeResponse;
export type UploadLogoApiArg = {
  id: string;
  body: {
    image: Blob;
  };
};
export type UploadBackgroundApiResponse = /** status 200 OK */ ThemeResponse;
export type UploadBackgroundApiArg = {
  id: string;
  body: {
    image: Blob;
  };
};
export type CreateOrgApiResponse = /** status 200 OK */ OrganizationResponse;
export type CreateOrgApiArg = {
  createOrganizationRequest: CreateOrganizationRequest;
};
export type JoinOrgApiResponse = /** status 200 OK */ OrganizationResponse;
export type JoinOrgApiArg = {
  joinOrganizationRequest: JoinOrganizationRequest;
};
export type CreateGameApiResponse = /** status 200 OK */ GameSessionDto;
export type CreateGameApiArg = {
  createGameRequest: CreateGameRequest;
};
export type JoinByRoomCodeApiResponse = /** status 200 OK */ GameSessionDto;
export type JoinByRoomCodeApiArg = {
  roomCode: string;
};
export type ListPacksApiResponse = /** status 200 OK */ ContentPackDto[];
export type ListPacksApiArg = void;
export type CreatePackApiResponse = /** status 200 OK */ ContentPackDto;
export type CreatePackApiArg = {
  createContentPackRequest: CreateContentPackRequest;
};
export type ListQuestionsApiResponse = /** status 200 OK */ QuestionEditorDto[];
export type ListQuestionsApiArg = {
  id: string;
};
export type AddQuestionApiResponse = /** status 200 OK */ QuestionEditorDto;
export type AddQuestionApiArg = {
  id: string;
  upsertQuestionRequest: UpsertQuestionRequest;
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
export type GetMyOrgApiResponse = /** status 200 OK */ OrganizationResponse;
export type GetMyOrgApiArg = void;
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
export type ListMyPacksApiResponse = /** status 200 OK */ ContentPackDto[];
export type ListMyPacksApiArg = void;
export type GetCurrentUserApiResponse = /** status 200 OK */ UserDto;
export type GetCurrentUserApiArg = void;
export type LoginApiResponse = unknown;
export type LoginApiArg = {
  returnUrl?: string;
  guestId?: string;
};
export type LeaveOrgApiResponse = unknown;
export type LeaveOrgApiArg = void;
export type ThemeResponse = {
  id?: string;
  name?: string;
  ownerId?: string;
  organizationId?: string;
  huePrimary?: number;
  hueAccent?: number;
  mode?: string;
  backgroundImageUrl?: string;
  logoImageUrl?: string;
  createdAt?: string;
};
export type UpdateThemeRequest = {
  name?: string;
  huePrimary?: number;
  hueAccent?: number;
  mode?: string;
  organizationId?: string;
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
export type UpdateContentPackRequest = {
  name?: string;
  description?: string;
  category?: string;
};
export type QuestionEditorDto = {
  id?: string;
  questionText?: string;
  options?: string[];
  correctAnswer?: number;
  pointValue?: number;
  timeLimit?: number;
  type?: "MULTIPLE_CHOICE" | "IMAGE_CHOICE" | "TEXT_INPUT";
  difficulty?: "EASY" | "MEDIUM" | "HARD";
  imageUrl?: string;
};
export type UpsertQuestionRequest = {
  questionText: string;
  options: string[];
  correctAnswer?: number;
  pointValue?: number;
  timeLimit?: number;
  difficulty?: "EASY" | "MEDIUM" | "HARD";
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
  organizationId?: string;
  activeThemeId?: string;
  lastLogin?: string;
  createdAt?: string;
};
export type CreateThemeRequest = {
  name?: string;
  huePrimary?: number;
  hueAccent?: number;
  mode?: string;
  organizationId?: string;
};
export type OrganizationResponse = {
  id?: string;
  name?: string;
  ownerId?: string;
  createdAt?: string;
};
export type CreateOrganizationRequest = {
  name?: string;
};
export type JoinOrganizationRequest = {
  organizationId?: string;
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
export type CreateContentPackRequest = {
  name: string;
  description?: string;
  category?: string;
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
  organizationId?: string;
  activeThemeId?: string;
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
export type UserDto = GuestUser | RegisteredUser;
export const {
  useUpdateThemeMutation,
  useDeleteThemeMutation,
  useGetPackQuery,
  useLazyGetPackQuery,
  useUpdatePackMutation,
  useDeletePackMutation,
  useUpdateQuestionMutation,
  useDeleteQuestionMutation,
  useUploadProfileImageMutation,
  useCloseAccountMutation,
  useListThemesQuery,
  useLazyListThemesQuery,
  useCreateThemeMutation,
  useUploadLogoMutation,
  useUploadBackgroundMutation,
  useCreateOrgMutation,
  useJoinOrgMutation,
  useCreateGameMutation,
  useJoinByRoomCodeMutation,
  useListPacksQuery,
  useLazyListPacksQuery,
  useCreatePackMutation,
  useListQuestionsQuery,
  useLazyListQuestionsQuery,
  useAddQuestionMutation,
  useRegisterMutation,
  useGuestLoginMutation,
  useUpdateProfileMutation,
  useGetUserProfileQuery,
  useLazyGetUserProfileQuery,
  useGetLeaderboardQuery,
  useLazyGetLeaderboardQuery,
  useCheckUsernameQuery,
  useLazyCheckUsernameQuery,
  useGetMyOrgQuery,
  useLazyGetMyOrgQuery,
  useGetHealthQuery,
  useLazyGetHealthQuery,
  useGetSessionQuery,
  useLazyGetSessionQuery,
  useCancelGameMutation,
  useGetResultsQuery,
  useLazyGetResultsQuery,
  useGetByInviteTokenQuery,
  useLazyGetByInviteTokenQuery,
  useListMyPacksQuery,
  useLazyListMyPacksQuery,
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLoginQuery,
  useLazyLoginQuery,
  useLeaveOrgMutation,
} = injectedRtkApi;
