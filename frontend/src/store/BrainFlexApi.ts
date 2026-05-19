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
    getTag: build.query<GetTagApiResponse, GetTagApiArg>({
      query: (queryArg) => ({ url: `/api/tags/${queryArg.id}` }),
    }),
    updateTag: build.mutation<UpdateTagApiResponse, UpdateTagApiArg>({
      query: (queryArg) => ({
        url: `/api/tags/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateTagRequest,
      }),
    }),
    deleteTag: build.mutation<DeleteTagApiResponse, DeleteTagApiArg>({
      query: (queryArg) => ({
        url: `/api/tags/${queryArg.id}`,
        method: "DELETE",
      }),
    }),
    updateImage: build.mutation<UpdateImageApiResponse, UpdateImageApiArg>({
      query: (queryArg) => ({
        url: `/api/gallery/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateGalleryImageRequest,
      }),
    }),
    deleteImage: build.mutation<DeleteImageApiResponse, DeleteImageApiArg>({
      query: (queryArg) => ({
        url: `/api/gallery/${queryArg.id}`,
        method: "DELETE",
      }),
    }),
    getDeck: build.query<GetDeckApiResponse, GetDeckApiArg>({
      query: (queryArg) => ({ url: `/api/decks/${queryArg.id}` }),
    }),
    updateDeck: build.mutation<UpdateDeckApiResponse, UpdateDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateDeckRequest,
      }),
    }),
    deleteDeck: build.mutation<DeleteDeckApiResponse, DeleteDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}`,
        method: "DELETE",
      }),
    }),
    updateElement: build.mutation<
      UpdateElementApiResponse,
      UpdateElementApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/elements/${queryArg.elementId}`,
        method: "PUT",
        body: queryArg.body,
      }),
    }),
    deleteElement: build.mutation<
      DeleteElementApiResponse,
      DeleteElementApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/elements/${queryArg.elementId}`,
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
    listTags: build.query<ListTagsApiResponse, ListTagsApiArg>({
      query: (queryArg) => ({
        url: `/api/tags`,
        params: {
          curated: queryArg.curated,
          parentTagId: queryArg.parentTagId,
          search: queryArg.search,
        },
      }),
    }),
    createTag: build.mutation<CreateTagApiResponse, CreateTagApiArg>({
      query: (queryArg) => ({
        url: `/api/tags`,
        method: "POST",
        body: queryArg.createTagRequest,
      }),
    }),
    createShowcase: build.mutation<
      CreateShowcaseApiResponse,
      CreateShowcaseApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases`,
        method: "POST",
        body: queryArg.createShowcaseRequest,
      }),
    }),
    joinByRoomCode: build.mutation<
      JoinByRoomCodeApiResponse,
      JoinByRoomCodeApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/join`,
        method: "POST",
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
    listImages: build.query<ListImagesApiResponse, ListImagesApiArg>({
      query: () => ({ url: `/api/gallery` }),
    }),
    uploadImage: build.mutation<UploadImageApiResponse, UploadImageApiArg>({
      query: (queryArg) => ({
        url: `/api/gallery`,
        method: "POST",
        body: queryArg.body,
        params: {
          name: queryArg.name,
          tags: queryArg.tags,
          organizationId: queryArg.organizationId,
        },
      }),
    }),
    listDecks: build.query<ListDecksApiResponse, ListDecksApiArg>({
      query: () => ({ url: `/api/decks` }),
    }),
    createDeck: build.mutation<CreateDeckApiResponse, CreateDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks`,
        method: "POST",
        body: queryArg.createDeckRequest,
      }),
    }),
    unpublishDeck: build.mutation<
      UnpublishDeckApiResponse,
      UnpublishDeckApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/unpublish`,
        method: "POST",
      }),
    }),
    publishDeck: build.mutation<PublishDeckApiResponse, PublishDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/publish`,
        method: "POST",
      }),
    }),
    addElement: build.mutation<AddElementApiResponse, AddElementApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/elements`,
        method: "POST",
        body: queryArg.body,
      }),
    }),
    moveMcqOption: build.mutation<
      MoveMcqOptionApiResponse,
      MoveMcqOptionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/elements/${queryArg.elementId}/options/${queryArg.optionId}/move`,
        method: "POST",
        params: {
          to: queryArg.to,
        },
      }),
    }),
    moveElement: build.mutation<MoveElementApiResponse, MoveElementApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/elements/${queryArg.elementId}/move`,
        method: "POST",
        params: {
          to: queryArg.to,
        },
      }),
    }),
    archiveDeck: build.mutation<ArchiveDeckApiResponse, ArchiveDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/archive`,
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
    getShowcase: build.query<GetShowcaseApiResponse, GetShowcaseApiArg>({
      query: (queryArg) => ({ url: `/api/showcases/${queryArg.roomCode}` }),
    }),
    cancelShowcase: build.mutation<
      CancelShowcaseApiResponse,
      CancelShowcaseApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}`,
        method: "DELETE",
      }),
    }),
    getReview: build.query<GetReviewApiResponse, GetReviewApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/review`,
      }),
    }),
    getResults: build.query<GetResultsApiResponse, GetResultsApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/results`,
      }),
    }),
    getByInviteToken: build.query<
      GetByInviteTokenApiResponse,
      GetByInviteTokenApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases/join/${queryArg.inviteToken}`,
      }),
    }),
    listMyOrgs: build.query<ListMyOrgsApiResponse, ListMyOrgsApiArg>({
      query: () => ({ url: `/api/organizations/mine` }),
    }),
    getHealth: build.query<GetHealthApiResponse, GetHealthApiArg>({
      query: () => ({ url: `/api/health` }),
    }),
    listMyDecks: build.query<ListMyDecksApiResponse, ListMyDecksApiArg>({
      query: () => ({ url: `/api/decks/mine` }),
    }),
    exploreDecks: build.query<ExploreDecksApiResponse, ExploreDecksApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/explore`,
        params: {
          tagId: queryArg.tagId,
          language: queryArg.language,
          difficulty: queryArg.difficulty,
          sort: queryArg.sort,
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
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
      query: (queryArg) => ({
        url: `/api/organizations/${queryArg.id}/leave`,
        method: "DELETE",
      }),
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
export type GetTagApiResponse = /** status 200 OK */ TagResponse;
export type GetTagApiArg = {
  id: string;
};
export type UpdateTagApiResponse = /** status 200 OK */ TagResponse;
export type UpdateTagApiArg = {
  id: string;
  updateTagRequest: UpdateTagRequest;
};
export type DeleteTagApiResponse = unknown;
export type DeleteTagApiArg = {
  id: string;
};
export type UpdateImageApiResponse = /** status 200 OK */ GalleryImageResponse;
export type UpdateImageApiArg = {
  id: string;
  updateGalleryImageRequest: UpdateGalleryImageRequest;
};
export type DeleteImageApiResponse = unknown;
export type DeleteImageApiArg = {
  id: string;
};
export type GetDeckApiResponse = /** status 200 OK */ DeckDto;
export type GetDeckApiArg = {
  id: string;
};
export type UpdateDeckApiResponse = /** status 200 OK */ DeckDto;
export type UpdateDeckApiArg = {
  id: string;
  updateDeckRequest: UpdateDeckRequest;
};
export type DeleteDeckApiResponse = unknown;
export type DeleteDeckApiArg = {
  id: string;
};
export type UpdateElementApiResponse = /** status 200 OK */ DeckDto;
export type UpdateElementApiArg = {
  id: string;
  elementId: string;
  body:
    | GridQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion;
};
export type DeleteElementApiResponse = /** status 200 OK */ DeckDto;
export type DeleteElementApiArg = {
  id: string;
  elementId: string;
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
export type ListTagsApiResponse = /** status 200 OK */ TagResponse[];
export type ListTagsApiArg = {
  curated?: boolean;
  parentTagId?: string;
  search?: string;
};
export type CreateTagApiResponse = /** status 200 OK */ TagResponse;
export type CreateTagApiArg = {
  createTagRequest: CreateTagRequest;
};
export type CreateShowcaseApiResponse = /** status 200 OK */ ShowcaseDto;
export type CreateShowcaseApiArg = {
  createShowcaseRequest: CreateShowcaseRequest;
};
export type JoinByRoomCodeApiResponse = /** status 200 OK */ ShowcaseDto;
export type JoinByRoomCodeApiArg = {
  roomCode: string;
};
export type CreateOrgApiResponse = /** status 200 OK */ OrganizationResponse;
export type CreateOrgApiArg = {
  createOrganizationRequest: CreateOrganizationRequest;
};
export type JoinOrgApiResponse = /** status 200 OK */ OrganizationResponse;
export type JoinOrgApiArg = {
  joinOrganizationRequest: JoinOrganizationRequest;
};
export type ListImagesApiResponse = /** status 200 OK */ GalleryImageResponse[];
export type ListImagesApiArg = void;
export type UploadImageApiResponse = /** status 200 OK */ GalleryImageResponse;
export type UploadImageApiArg = {
  name?: string;
  tags?: string;
  organizationId?: string;
  body: {
    image: Blob;
  };
};
export type ListDecksApiResponse = /** status 200 OK */ DeckDto[];
export type ListDecksApiArg = void;
export type CreateDeckApiResponse = /** status 200 OK */ DeckDto;
export type CreateDeckApiArg = {
  createDeckRequest: CreateDeckRequest;
};
export type UnpublishDeckApiResponse = /** status 200 OK */ DeckDto;
export type UnpublishDeckApiArg = {
  id: string;
};
export type PublishDeckApiResponse = /** status 200 OK */ DeckDto;
export type PublishDeckApiArg = {
  id: string;
};
export type AddElementApiResponse = /** status 200 OK */ DeckDto;
export type AddElementApiArg = {
  id: string;
  body:
    | GridQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion;
};
export type MoveMcqOptionApiResponse = /** status 200 OK */ DeckDto;
export type MoveMcqOptionApiArg = {
  id: string;
  elementId: string;
  optionId: string;
  to: number;
};
export type MoveElementApiResponse = /** status 200 OK */ DeckDto;
export type MoveElementApiArg = {
  id: string;
  elementId: string;
  to: number;
};
export type ArchiveDeckApiResponse = /** status 200 OK */ DeckDto;
export type ArchiveDeckApiArg = {
  id: string;
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
export type GetShowcaseApiResponse = /** status 200 OK */ ShowcaseDto;
export type GetShowcaseApiArg = {
  roomCode: string;
};
export type CancelShowcaseApiResponse = unknown;
export type CancelShowcaseApiArg = {
  roomCode: string;
};
export type GetReviewApiResponse = /** status 200 OK */ ShowcaseReviewDto;
export type GetReviewApiArg = {
  roomCode: string;
};
export type GetResultsApiResponse = /** status 200 OK */ ShowcaseResult;
export type GetResultsApiArg = {
  roomCode: string;
};
export type GetByInviteTokenApiResponse = /** status 200 OK */ ShowcaseDto;
export type GetByInviteTokenApiArg = {
  inviteToken: string;
};
export type ListMyOrgsApiResponse = /** status 200 OK */ OrganizationResponse[];
export type ListMyOrgsApiArg = void;
export type GetHealthApiResponse = /** status 200 OK */ HealthCheckResponse;
export type GetHealthApiArg = void;
export type ListMyDecksApiResponse = /** status 200 OK */ DeckDto[];
export type ListMyDecksApiArg = void;
export type ExploreDecksApiResponse = /** status 200 OK */ DeckExploreResponse;
export type ExploreDecksApiArg = {
  tagId?: string;
  language?: string;
  difficulty?: "EASY" | "MEDIUM" | "HARD";
  sort?: string;
  page?: number;
  size?: number;
};
export type GetCurrentUserApiResponse = /** status 200 OK */ UserDto;
export type GetCurrentUserApiArg = void;
export type LoginApiResponse = unknown;
export type LoginApiArg = {
  returnUrl?: string;
  guestId?: string;
};
export type LeaveOrgApiResponse = unknown;
export type LeaveOrgApiArg = {
  id: string;
};
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
export type TagResponse = {
  id?: string;
  displayName?: string;
  parentTagId?: string;
  description?: string;
  iconUrl?: string;
  deckCount?: number;
  curated?: boolean;
  children?: TagResponse[];
  createdAt?: string;
  updatedAt?: string;
};
export type UpdateTagRequest = {
  displayName?: string;
  parentTagId?: string;
  description?: string;
  iconUrl?: string;
  curated?: boolean;
};
export type GalleryImageResponse = {
  id?: string;
  name?: string;
  ownerId?: string;
  organizationId?: string;
  tags?: string[];
  imageUrl?: string;
  createdAt?: string;
};
export type UpdateGalleryImageRequest = {
  name?: string;
  tags?: string[];
  organizationId?: string;
};
export type Image = {
  useExternalImg?: boolean;
  internalImgId?: string;
  imgUrl?: string;
  blank?: boolean;
};
export type ShowcaseSettings = {
  maxPlayers?: number;
  totalRounds?: number;
  timePerQuestion?: number;
  speedBonus?: boolean;
  allowGuests?: boolean;
  gameMode?: "SIMULTANEOUS" | "TURN_BASED";
  allowLateJoin?: boolean;
  showScoresImmediately?: boolean;
  scoringEnabled?: boolean;
};
export type DeckElementBase = {
  kind: string;
};
export type GridCellsConfig = {
  labels?: string[];
  backingImage?: Image;
};
export type GridQuestion = {
  kind: "GridQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    rows?: number;
    cols?: number;
    cells?: GridCellsConfig;
    correctCellIndexes?: number[];
    multipleCorrect?: boolean;
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type McqOption = {
  id?: string;
  text?: string;
  image?: Image;
  color?: string;
};
export type McqQuestion = {
  kind: "McqQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    options?: McqOption[];
    correctOptionIds?: string[];
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type NumberQuestion = {
  kind: "NumberQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    correctValue?: number;
    tolerance?: number;
    unitLabel?: string;
    decimalPlaces?: number;
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type PlaceOnImageQuestion = {
  kind: "PlaceOnImageQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    targetImage?: Image;
    correctX?: number;
    correctY?: number;
    tolerance?: number;
    scoring?: "BINARY" | "LINEAR";
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type QAndAQuestion = {
  kind: "QAndAQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    maxSubmissionsPerPlayer?: number;
    allowVoting?: boolean;
    autoApprove?: boolean;
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type RankingItem = {
  id?: string;
  label?: string;
  image?: Image;
};
export type RankingQuestion = {
  kind: "RankingQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    items?: RankingItem[];
    correctOrder?: string[];
    scoring?: "EXACT" | "PARTIAL";
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type ScaleStatement = {
  id?: string;
  text?: string;
};
export type ScalesQuestion = {
  kind: "ScalesQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    statements?: ScaleStatement[];
    scaleMin?: number;
    scaleMax?: number;
    minLabel?: string;
    maxLabel?: string;
    correctRatings?: number[];
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type Slide = {
  kind: "Slide";
} & DeckElementBase & {
    id?: string;
    slideKind?: "TITLE" | "SECTION" | "CALLOUT" | "CONTENT" | "END";
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    body?: string;
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
    resultsDisplayType?: "DEFAULT" | "HISTOGRAM" | "PIE_CHART";
    multipleSelectionsEnabled?: boolean;
    selectionsPerParticipant?: number;
    showResultsAsPercentage?: boolean;
    joinType?: "INSTRUCTIONS_BAR" | "QR_CODE";
    showJoinInformation?: boolean;
    showResponses?: "INSTANT" | "ON_CLICK" | "PRIVATE";
    heading?: string;
    participantInformation?: {
      [key: string]: object;
    };
  };
export type TextQuestion = {
  kind: "TextQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    correctAnswer?: string;
    acceptedVariants?: string[];
    caseSensitive?: boolean;
    pointValue?: number;
    difficulty?: "EASY" | "MEDIUM" | "HARD";
    scored?: boolean;
    survey?: boolean;
    multipleSelections?: number;
    responseMode?: "ACCEPTING_RESPONSES" | "NOT_ACCEPTING_RESPONSES";
    bestAnswerMode?: boolean;
    bestAnswerTitle?: string;
    bestAnswerBonus?: number;
    explanation?: string;
    displaySeconds?: number;
    speakerNotes?: string;
    background?: Image;
    image?: Image;
    videoUrl?: string;
    audioUrl?: string;
    mediaPosition?: "TOP" | "BOTTOM" | "BACKGROUND" | "NONE";
  };
export type DeckDto = {
  id?: string;
  name?: string;
  description?: string;
  creatorUserId?: string;
  organizationId?: string;
  tags?: string[];
  tagIds?: string[];
  subjectTagId?: string;
  isSystem?: boolean;
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  recommendedPreset?: "GAME" | "PULSE" | "PRESENTATION";
  cover?: Image;
  background?: Image;
  themeId?: string;
  defaultSettings?: ShowcaseSettings;
  estimatedDurationMinutes?: number;
  elementCount?: number;
  elements?: (
    | GridQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
  )[];
  parentDeckId?: string;
  originalAuthorUserId?: string;
  publishStatus?: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  publishedAt?: string;
  language?: string;
  difficulty?: "EASY" | "MEDIUM" | "HARD";
  ageRange?: string;
  license?: "ALL_RIGHTS_RESERVED" | "CC_BY" | "CC_BY_SA" | "CC_BY_NC" | "CC0";
  playCount?: number;
  viewCount?: number;
  favoriteCount?: number;
  averageRating?: number;
  ratingCount?: number;
  lastPlayedAt?: string;
  createdAt?: string;
  updatedAt?: string;
};
export type UpdateDeckRequest = {
  name?: string;
  description?: string;
  tags?: string[];
  tagIds?: string[];
  subjectTagId?: string;
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  recommendedPreset?: "GAME" | "PULSE" | "PRESENTATION";
  cover?: Image;
  background?: Image;
  themeId?: string;
  estimatedDurationMinutes?: number;
  language?: string;
  difficulty?: "EASY" | "MEDIUM" | "HARD";
  ageRange?: string;
  license?: "ALL_RIGHTS_RESERVED" | "CC_BY" | "CC_BY_SA" | "CC_BY_NC" | "CC0";
};
export type PlayerStats = {
  gamesPlayed?: number;
  highScore?: number;
  totalPoints?: number;
  currentStreak?: number;
};
export type Membership = {
  tier?: "FREE" | "INDIVIDUAL" | "ORG_SEAT" | "ORG_TEAM" | "ORG_BUSINESS";
  status?: "ACTIVE" | "TRIALING" | "PAST_DUE" | "CANCELED" | "EXPIRED" | "NONE";
  startedAt?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd?: boolean;
  sourceOrganizationId?: string;
  stripeCustomerId?: string;
  stripeSubscriptionId?: string;
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
  membership?: Membership;
  newsletter?: boolean;
  organizationIds?: string[];
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
export type CreateTagRequest = {
  id?: string;
  displayName: string;
  parentTagId?: string;
  description?: string;
  iconUrl?: string;
  curated?: boolean;
};
export type ShowcasePlayerDto = {
  userId?: string;
  userName?: string;
  pictureUrl?: string;
  isGuest?: boolean;
  score?: number;
};
export type ShowcaseDto = {
  id?: string;
  roomCode?: string;
  inviteToken?: string;
  status?: "LOBBY" | "IN_PROGRESS" | "RESULTS" | "FINISHED" | "CANCELLED";
  phase?: "SUBMIT" | "VOTE" | "REVEAL";
  hostUserId?: string;
  deckId?: string;
  deckCoverImageUrl?: string;
  deckBackgroundImageUrl?: string;
  themeId?: string;
  deckSnapshot?: (
    | GridQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
  )[];
  settings?: ShowcaseSettings;
  players?: ShowcasePlayerDto[];
  currentRound?: number;
  createdAt?: string;
  startedAt?: string;
};
export type CreateShowcaseRequest = {
  deckId: string;
  gameMode?: "SIMULTANEOUS" | "TURN_BASED";
  totalRounds?: number;
  timePerQuestion?: number;
  speedBonus?: boolean;
  allowGuests?: boolean;
  maxPlayers?: number;
  allowLateJoin?: boolean;
  showScoresImmediately?: boolean;
  scoringEnabled?: boolean;
};
export type OrganizationPlan = {
  tier?: "FREE" | "INDIVIDUAL" | "ORG_SEAT" | "ORG_TEAM" | "ORG_BUSINESS";
  status?: "ACTIVE" | "TRIALING" | "PAST_DUE" | "CANCELED" | "EXPIRED" | "NONE";
  seatLimit?: number;
  startedAt?: string;
  currentPeriodEnd?: string;
  cancelAtPeriodEnd?: boolean;
  stripeCustomerId?: string;
  stripeSubscriptionId?: string;
};
export type OrganizationResponse = {
  id?: string;
  name?: string;
  ownerId?: string;
  plan?: OrganizationPlan;
  createdAt?: string;
};
export type CreateOrganizationRequest = {
  name?: string;
};
export type JoinOrganizationRequest = {
  organizationId?: string;
};
export type CreateDeckRequest = {
  id?: string;
  name: string;
  description?: string;
  tags?: string[];
  tagIds?: string[];
  subjectTagId?: string;
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  recommendedPreset?: "GAME" | "PULSE" | "PRESENTATION";
  cover?: Image;
  background?: Image;
  themeId?: string;
  estimatedDurationMinutes?: number;
  language?: string;
  difficulty?: "EASY" | "MEDIUM" | "HARD";
  ageRange?: string;
  license?: "ALL_RIGHTS_RESERVED" | "CC_BY" | "CC_BY_SA" | "CC_BY_NC" | "CC0";
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
  activeThemeId?: string;
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
export type AnswerPayloadBase = {
  kind: string;
};
export type GridAnswer = {
  kind: "GridAnswer";
} & AnswerPayloadBase & {
    selectedCellIndexes?: number[];
  };
export type McqAnswer = {
  kind: "McqAnswer";
} & AnswerPayloadBase & {
    optionIds?: string[];
  };
export type NumberAnswer = {
  kind: "NumberAnswer";
} & AnswerPayloadBase & {
    value?: number;
  };
export type PlaceOnImageAnswer = {
  kind: "PlaceOnImageAnswer";
} & AnswerPayloadBase & {
    x?: number;
    y?: number;
  };
export type RankingAnswer = {
  kind: "RankingAnswer";
} & AnswerPayloadBase & {
    orderedItemIds?: string[];
  };
export type ScalesAnswer = {
  kind: "ScalesAnswer";
} & AnswerPayloadBase & {
    ratings?: {
      [key: string]: number;
    };
  };
export type TextAnswer = {
  kind: "TextAnswer";
} & AnswerPayloadBase & {
    text?: string;
  };
export type TimeoutAnswer = {
  kind: "TimeoutAnswer";
} & AnswerPayloadBase;
export type PlayerRoundDetail = {
  userId?: string;
  userName?: string;
  payload?:
    | GridAnswer
    | McqAnswer
    | NumberAnswer
    | PlaceOnImageAnswer
    | RankingAnswer
    | ScalesAnswer
    | TextAnswer
    | TimeoutAnswer;
  wasCorrect?: boolean;
  pointsAwarded?: number;
};
export type RoundReview = {
  round?: number;
  element?:
    | GridQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion;
  timedOutCount?: number;
  playerAnswers?: PlayerRoundDetail[];
};
export type ShowcaseReviewDto = {
  showcaseId?: string;
  roomCode?: string;
  endedAt?: string;
  scoringEnabled?: boolean;
  placements?: PlayerPlacement[];
  rounds?: RoundReview[];
};
export type ShowcaseResult = {
  id?: string;
  showcaseId?: string;
  placements?: PlayerPlacement[];
  endedAt?: string;
};
export type HealthCheckResponse = {
  status?: string;
  message?: string;
  timestamp?: string;
  database?: string;
  redis?: string;
};
export type DeckExploreResponse = {
  items?: DeckDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
};
export type UserDto = GuestUser | RegisteredUser;
export const {
  useUpdateThemeMutation,
  useDeleteThemeMutation,
  useGetTagQuery,
  useLazyGetTagQuery,
  useUpdateTagMutation,
  useDeleteTagMutation,
  useUpdateImageMutation,
  useDeleteImageMutation,
  useGetDeckQuery,
  useLazyGetDeckQuery,
  useUpdateDeckMutation,
  useDeleteDeckMutation,
  useUpdateElementMutation,
  useDeleteElementMutation,
  useUploadProfileImageMutation,
  useCloseAccountMutation,
  useListThemesQuery,
  useLazyListThemesQuery,
  useCreateThemeMutation,
  useUploadLogoMutation,
  useUploadBackgroundMutation,
  useListTagsQuery,
  useLazyListTagsQuery,
  useCreateTagMutation,
  useCreateShowcaseMutation,
  useJoinByRoomCodeMutation,
  useCreateOrgMutation,
  useJoinOrgMutation,
  useListImagesQuery,
  useLazyListImagesQuery,
  useUploadImageMutation,
  useListDecksQuery,
  useLazyListDecksQuery,
  useCreateDeckMutation,
  useUnpublishDeckMutation,
  usePublishDeckMutation,
  useAddElementMutation,
  useMoveMcqOptionMutation,
  useMoveElementMutation,
  useArchiveDeckMutation,
  useRegisterMutation,
  useGuestLoginMutation,
  useUpdateProfileMutation,
  useGetUserProfileQuery,
  useLazyGetUserProfileQuery,
  useGetLeaderboardQuery,
  useLazyGetLeaderboardQuery,
  useCheckUsernameQuery,
  useLazyCheckUsernameQuery,
  useGetShowcaseQuery,
  useLazyGetShowcaseQuery,
  useCancelShowcaseMutation,
  useGetReviewQuery,
  useLazyGetReviewQuery,
  useGetResultsQuery,
  useLazyGetResultsQuery,
  useGetByInviteTokenQuery,
  useLazyGetByInviteTokenQuery,
  useListMyOrgsQuery,
  useLazyListMyOrgsQuery,
  useGetHealthQuery,
  useLazyGetHealthQuery,
  useListMyDecksQuery,
  useLazyListMyDecksQuery,
  useExploreDecksQuery,
  useLazyExploreDecksQuery,
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLoginQuery,
  useLazyLoginQuery,
  useLeaveOrgMutation,
} = injectedRtkApi;
