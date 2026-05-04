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
export type UserDto = GuestUser | RegisteredUser;
export const {
  useUploadProfileImageMutation,
  useCloseAccountMutation,
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
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLoginQuery,
  useLazyLoginQuery,
} = injectedRtkApi;
