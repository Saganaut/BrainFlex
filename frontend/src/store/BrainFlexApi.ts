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
    updateTeam: build.mutation<UpdateTeamApiResponse, UpdateTeamApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/teams/${queryArg.teamId}`,
        method: "PUT",
        body: queryArg.teamCrudRequest,
      }),
    }),
    deleteTeam: build.mutation<DeleteTeamApiResponse, DeleteTeamApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/teams/${queryArg.teamId}`,
        method: "DELETE",
      }),
    }),
    movePlayerToTeam: build.mutation<
      MovePlayerToTeamApiResponse,
      MovePlayerToTeamApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/players/${queryArg.userId}/team`,
        method: "PUT",
        body: queryArg.teamMoveRequest,
      }),
    }),
    moderateChat: build.mutation<ModerateChatApiResponse, ModerateChatApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/chat/${queryArg.messageId}/moderate`,
        method: "PUT",
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
    rateDeck: build.mutation<RateDeckApiResponse, RateDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/rating`,
        method: "PUT",
        body: queryArg.rateDeckRequest,
      }),
    }),
    deleteMyRating: build.mutation<
      DeleteMyRatingApiResponse,
      DeleteMyRatingApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/rating`,
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
    updateCollaboratorRole: build.mutation<
      UpdateCollaboratorRoleApiResponse,
      UpdateCollaboratorRoleApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/collaborators/${queryArg.userId}`,
        method: "PUT",
        body: queryArg.updateCollaboratorRoleRequest,
      }),
    }),
    removeCollaborator: build.mutation<
      RemoveCollaboratorApiResponse,
      RemoveCollaboratorApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/collaborators/${queryArg.userId}`,
        method: "DELETE",
      }),
    }),
    editComment: build.mutation<EditCommentApiResponse, EditCommentApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.deckId}/comments/${queryArg.commentId}`,
        method: "PUT",
        body: queryArg.updateCommentRequest,
      }),
    }),
    deleteComment: build.mutation<
      DeleteCommentApiResponse,
      DeleteCommentApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.deckId}/comments/${queryArg.commentId}`,
        method: "DELETE",
      }),
    }),
    getCollection: build.query<GetCollectionApiResponse, GetCollectionApiArg>({
      query: (queryArg) => ({ url: `/api/collections/${queryArg.id}` }),
    }),
    updateCollection: build.mutation<
      UpdateCollectionApiResponse,
      UpdateCollectionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/${queryArg.id}`,
        method: "PUT",
        body: queryArg.updateDeckCollectionRequest,
      }),
    }),
    deleteCollection: build.mutation<
      DeleteCollectionApiResponse,
      DeleteCollectionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/${queryArg.id}`,
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
    createTeam: build.mutation<CreateTeamApiResponse, CreateTeamApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/teams`,
        method: "POST",
        body: queryArg.teamCrudRequest,
      }),
    }),
    sendReaction: build.mutation<SendReactionApiResponse, SendReactionApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/reactions`,
        method: "POST",
        body: queryArg.reactionSendRequest,
      }),
    }),
    joinByRoomCode: build.mutation<
      JoinByRoomCodeApiResponse,
      JoinByRoomCodeApiArg
    >({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/join`,
        method: "POST",
        body: queryArg.joinShowcaseRequest,
      }),
    }),
    listChat: build.query<ListChatApiResponse, ListChatApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/chat`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
    }),
    sendChat: build.mutation<SendChatApiResponse, SendChatApiArg>({
      query: (queryArg) => ({
        url: `/api/showcases/${queryArg.roomCode}/chat`,
        method: "POST",
        body: queryArg.chatSendRequest,
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
    favoriteDeck: build.mutation<FavoriteDeckApiResponse, FavoriteDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/favorite`,
        method: "POST",
      }),
    }),
    unfavoriteDeck: build.mutation<
      UnfavoriteDeckApiResponse,
      UnfavoriteDeckApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/favorite`,
        method: "DELETE",
      }),
    }),
    recountFavorites: build.mutation<
      RecountFavoritesApiResponse,
      RecountFavoritesApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/favorite/recount`,
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
    listComments: build.query<ListCommentsApiResponse, ListCommentsApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/comments`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
    }),
    postComment: build.mutation<PostCommentApiResponse, PostCommentApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/comments`,
        method: "POST",
        body: queryArg.createCommentRequest,
      }),
    }),
    listCollaborators: build.query<
      ListCollaboratorsApiResponse,
      ListCollaboratorsApiArg
    >({
      query: (queryArg) => ({ url: `/api/decks/${queryArg.id}/collaborators` }),
    }),
    inviteCollaborator: build.mutation<
      InviteCollaboratorApiResponse,
      InviteCollaboratorApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/collaborators`,
        method: "POST",
        body: queryArg.inviteCollaboratorRequest,
      }),
    }),
    transferOwnership: build.mutation<
      TransferOwnershipApiResponse,
      TransferOwnershipApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/collaborators/transfer`,
        method: "POST",
        body: queryArg.transferOwnershipRequest,
      }),
    }),
    archiveDeck: build.mutation<ArchiveDeckApiResponse, ArchiveDeckApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/archive`,
        method: "POST",
      }),
    }),
    toggleCommentUpvote: build.mutation<
      ToggleCommentUpvoteApiResponse,
      ToggleCommentUpvoteApiArg
    >({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.deckId}/comments/${queryArg.commentId}/upvote`,
        method: "POST",
      }),
    }),
    createCollection: build.mutation<
      CreateCollectionApiResponse,
      CreateCollectionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections`,
        method: "POST",
        body: queryArg.createDeckCollectionRequest,
      }),
    }),
    addDeckToCollection: build.mutation<
      AddDeckToCollectionApiResponse,
      AddDeckToCollectionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/${queryArg.id}/decks`,
        method: "POST",
        body: queryArg.addDeckToCollectionRequest,
      }),
    }),
    reorderCollectionDecks: build.mutation<
      ReorderCollectionDecksApiResponse,
      ReorderCollectionDecksApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/${queryArg.id}/decks`,
        method: "PATCH",
        body: queryArg.reorderCollectionDecksRequest,
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
    listMyFavorites: build.query<
      ListMyFavoritesApiResponse,
      ListMyFavoritesApiArg
    >({
      query: (queryArg) => ({
        url: `/api/users/me/favorites`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
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
    listRatings: build.query<ListRatingsApiResponse, ListRatingsApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.id}/ratings`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
    }),
    getMyRating: build.query<GetMyRatingApiResponse, GetMyRatingApiArg>({
      query: (queryArg) => ({ url: `/api/decks/${queryArg.id}/rating/mine` }),
    }),
    listReplies: build.query<ListRepliesApiResponse, ListRepliesApiArg>({
      query: (queryArg) => ({
        url: `/api/decks/${queryArg.deckId}/comments/${queryArg.commentId}/replies`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
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
    listMyCollections: build.query<
      ListMyCollectionsApiResponse,
      ListMyCollectionsApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/mine`,
        params: {
          page: queryArg.page,
          size: queryArg.size,
        },
      }),
    }),
    list: build.query<ListApiResponse, ListApiArg>({
      query: () => ({ url: `/api/avatars` }),
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
    removeDeckFromCollection: build.mutation<
      RemoveDeckFromCollectionApiResponse,
      RemoveDeckFromCollectionApiArg
    >({
      query: (queryArg) => ({
        url: `/api/collections/${queryArg.id}/decks/${queryArg.deckId}`,
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
export type UpdateTeamApiResponse = /** status 200 OK */ ShowcaseDto;
export type UpdateTeamApiArg = {
  roomCode: string;
  teamId: string;
  teamCrudRequest: TeamCrudRequest;
};
export type DeleteTeamApiResponse = /** status 200 OK */ ShowcaseDto;
export type DeleteTeamApiArg = {
  roomCode: string;
  teamId: string;
};
export type MovePlayerToTeamApiResponse = /** status 200 OK */ ShowcaseDto;
export type MovePlayerToTeamApiArg = {
  roomCode: string;
  userId: string;
  teamMoveRequest: TeamMoveRequest;
};
export type ModerateChatApiResponse =
  /** status 200 OK */ ShowcaseChatMessageDto;
export type ModerateChatApiArg = {
  roomCode: string;
  messageId: string;
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
export type RateDeckApiResponse = /** status 200 OK */ DeckRatingDto;
export type RateDeckApiArg = {
  id: string;
  rateDeckRequest: RateDeckRequest;
};
export type DeleteMyRatingApiResponse = unknown;
export type DeleteMyRatingApiArg = {
  id: string;
};
export type UpdateElementApiResponse = /** status 200 OK */ DeckDto;
export type UpdateElementApiArg = {
  id: string;
  elementId: string;
  body:
    | AllocationQuestion
    | DrawingQuestion
    | GridQuestion
    | MatchingQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
    | WordCloudQuestion;
};
export type DeleteElementApiResponse = /** status 200 OK */ DeckDto;
export type DeleteElementApiArg = {
  id: string;
  elementId: string;
};
export type UpdateCollaboratorRoleApiResponse =
  /** status 200 OK */ DeckCollaboratorDto;
export type UpdateCollaboratorRoleApiArg = {
  id: string;
  userId: string;
  updateCollaboratorRoleRequest: UpdateCollaboratorRoleRequest;
};
export type RemoveCollaboratorApiResponse = unknown;
export type RemoveCollaboratorApiArg = {
  id: string;
  userId: string;
};
export type EditCommentApiResponse = /** status 200 OK */ DeckCommentDto;
export type EditCommentApiArg = {
  deckId: string;
  commentId: string;
  updateCommentRequest: UpdateCommentRequest;
};
export type DeleteCommentApiResponse = /** status 200 OK */ DeckCommentDto;
export type DeleteCommentApiArg = {
  deckId: string;
  commentId: string;
};
export type GetCollectionApiResponse = /** status 200 OK */ DeckCollectionDto;
export type GetCollectionApiArg = {
  id: string;
};
export type UpdateCollectionApiResponse =
  /** status 200 OK */ DeckCollectionDto;
export type UpdateCollectionApiArg = {
  id: string;
  updateDeckCollectionRequest: UpdateDeckCollectionRequest;
};
export type DeleteCollectionApiResponse = unknown;
export type DeleteCollectionApiArg = {
  id: string;
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
export type CreateTeamApiResponse = /** status 200 OK */ ShowcaseDto;
export type CreateTeamApiArg = {
  roomCode: string;
  teamCrudRequest: TeamCrudRequest;
};
export type SendReactionApiResponse = /** status 200 OK */ Reaction;
export type SendReactionApiArg = {
  roomCode: string;
  reactionSendRequest: ReactionSendRequest;
};
export type JoinByRoomCodeApiResponse = /** status 200 OK */ ShowcaseDto;
export type JoinByRoomCodeApiArg = {
  roomCode: string;
  joinShowcaseRequest: JoinShowcaseRequest;
};
export type ListChatApiResponse = /** status 200 OK */ ShowcaseChatMessageDto[];
export type ListChatApiArg = {
  roomCode: string;
  page?: number;
  size?: number;
};
export type SendChatApiResponse = /** status 200 OK */ ShowcaseChatMessageDto;
export type SendChatApiArg = {
  roomCode: string;
  chatSendRequest: ChatSendRequest;
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
export type FavoriteDeckApiResponse = /** status 200 OK */ DeckFavoriteResponse;
export type FavoriteDeckApiArg = {
  id: string;
};
export type UnfavoriteDeckApiResponse =
  /** status 200 OK */ DeckFavoriteResponse;
export type UnfavoriteDeckApiArg = {
  id: string;
};
export type RecountFavoritesApiResponse =
  /** status 200 OK */ DeckFavoriteResponse;
export type RecountFavoritesApiArg = {
  id: string;
};
export type AddElementApiResponse = /** status 200 OK */ DeckDto;
export type AddElementApiArg = {
  id: string;
  body:
    | AllocationQuestion
    | DrawingQuestion
    | GridQuestion
    | MatchingQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
    | WordCloudQuestion;
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
export type ListCommentsApiResponse = /** status 200 OK */ DeckCommentsPage;
export type ListCommentsApiArg = {
  id: string;
  page?: number;
  size?: number;
};
export type PostCommentApiResponse = /** status 200 OK */ DeckCommentDto;
export type PostCommentApiArg = {
  id: string;
  createCommentRequest: CreateCommentRequest;
};
export type ListCollaboratorsApiResponse =
  /** status 200 OK */ DeckCollaboratorDto[];
export type ListCollaboratorsApiArg = {
  id: string;
};
export type InviteCollaboratorApiResponse =
  /** status 200 OK */ DeckCollaboratorDto;
export type InviteCollaboratorApiArg = {
  id: string;
  inviteCollaboratorRequest: InviteCollaboratorRequest;
};
export type TransferOwnershipApiResponse =
  /** status 200 OK */ DeckCollaboratorDto[];
export type TransferOwnershipApiArg = {
  id: string;
  transferOwnershipRequest: TransferOwnershipRequest;
};
export type ArchiveDeckApiResponse = /** status 200 OK */ DeckDto;
export type ArchiveDeckApiArg = {
  id: string;
};
export type ToggleCommentUpvoteApiResponse =
  /** status 200 OK */ DeckCommentDto;
export type ToggleCommentUpvoteApiArg = {
  deckId: string;
  commentId: string;
};
export type CreateCollectionApiResponse =
  /** status 200 OK */ DeckCollectionDto;
export type CreateCollectionApiArg = {
  createDeckCollectionRequest: CreateDeckCollectionRequest;
};
export type AddDeckToCollectionApiResponse =
  /** status 200 OK */ DeckCollectionDto;
export type AddDeckToCollectionApiArg = {
  id: string;
  addDeckToCollectionRequest: AddDeckToCollectionRequest;
};
export type ReorderCollectionDecksApiResponse =
  /** status 200 OK */ DeckCollectionDto;
export type ReorderCollectionDecksApiArg = {
  id: string;
  reorderCollectionDecksRequest: ReorderCollectionDecksRequest;
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
export type ListMyFavoritesApiResponse = /** status 200 OK */ DeckFavoritesPage;
export type ListMyFavoritesApiArg = {
  page?: number;
  size?: number;
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
export type ListRatingsApiResponse = /** status 200 OK */ DeckRatingsPage;
export type ListRatingsApiArg = {
  id: string;
  page?: number;
  size?: number;
};
export type GetMyRatingApiResponse = /** status 200 OK */ DeckRatingDto;
export type GetMyRatingApiArg = {
  id: string;
};
export type ListRepliesApiResponse = /** status 200 OK */ DeckCommentsPage;
export type ListRepliesApiArg = {
  deckId: string;
  commentId: string;
  page?: number;
  size?: number;
};
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
export type ListMyCollectionsApiResponse =
  /** status 200 OK */ DeckCollectionsPage;
export type ListMyCollectionsApiArg = {
  page?: number;
  size?: number;
};
export type ListApiResponse = /** status 200 OK */ AvatarPreset[];
export type ListApiArg = void;
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
export type RemoveDeckFromCollectionApiResponse =
  /** status 200 OK */ DeckCollectionDto;
export type RemoveDeckFromCollectionApiArg = {
  id: string;
  deckId: string;
};
export type ImageVariant = {
  size?: "XS" | "SM" | "MD" | "LG" | "XL";
  url?: string;
  width?: number;
  height?: number;
};
export type Image = {
  useExternalImg?: boolean;
  internalImgId?: string;
  variants?: ImageVariant[];
  blank?: boolean;
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
  background?: Image;
  logo?: Image;
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
export type DeckElementBase = {
  kind: string;
};
export type McqOption = {
  id?: string;
  text?: string;
  image?: Image;
  color?: string;
};
export type AllocationQuestion = {
  kind: "AllocationQuestion";
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
    totalPointsToDistribute?: number;
    allowZeroOnItem?: boolean;
    enforceExactTotal?: boolean;
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
  };
export type DrawingQuestion = {
  kind: "DrawingQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    backingImage?: Image;
    canvasWidth?: number;
    canvasHeight?: number;
    maxStrokesPerPlayer?: number;
    maxPointsPerStroke?: number;
    palette?: string[];
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
  };
export type MatchingPair = {
  id?: string;
  leftLabel?: string;
  rightLabel?: string;
  leftImage?: Image;
  rightImage?: Image;
};
export type MatchingQuestion = {
  kind: "MatchingQuestion";
} & DeckElementBase & {
    id?: string;
    publicKey?: string;
    privateKey?: string;
    title?: string;
    styledTitle?: {
      [key: string]: object;
    };
    prompt?: string;
    pairs?: MatchingPair[];
    scoring?: "ALL_OR_NOTHING" | "PARTIAL";
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    shuffleOptions?: boolean;
    allowMultipleSelect?: boolean;
    maxSelections?: number;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    minValue?: number;
    maxValue?: number;
    allowNegative?: boolean;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    anonymousSubmissions?: boolean;
    minVotesToShow?: number;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    shuffleItemsForPresentation?: boolean;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    autoAdvanceSeconds?: number;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
    maxLength?: number;
    trimWhitespace?: boolean;
    fuzzyMatch?: boolean;
    fuzzyDistance?: number;
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
  };
export type WordCloudQuestion = {
  kind: "WordCloudQuestion";
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
    maxWordLength?: number;
    caseSensitive?: boolean;
    profanityFilter?: boolean;
    bannedWords?: string[];
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
    createdByUserId?: string;
    lastEditedByUserId?: string;
    createdAt?: string;
    updatedAt?: string;
    tagIds?: string[];
    mediaCaption?: string;
    altText?: string;
    reactionsEnabled?: boolean;
    version?: number;
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
  reactionsEnabled?: boolean;
  chatEnabled?: boolean;
  teamMode?: boolean;
  teamCount?: number;
  autoBalanceTeams?: boolean;
  shuffleQuestions?: boolean;
  shuffleAnswers?: boolean;
  autoAdvance?: boolean;
  podiumDuration?: number;
  lobbyCountdownSeconds?: number;
  lobbyMusicAssetId?: string;
  requireFullName?: boolean;
  spectatorsAllowed?: boolean;
};
export type ShowcasePlayerDto = {
  userId?: string;
  userName?: string;
  pictureUrl?: string;
  isGuest?: boolean;
  score?: number;
  teamId?: string;
  avatarKey?: string;
  colorTag?: string;
  currentStreak?: number;
  longestStreak?: number;
  accuracy?: number;
  reactionsSent?: number;
  speedBonusTotal?: number;
  lateJoin?: boolean;
  disconnected?: boolean;
  lastSeenAt?: string;
};
export type Team = {
  id?: string;
  name?: string;
  color?: string;
  captainUserId?: string;
  score?: number;
  memberCount?: number;
};
export type ShowcaseDto = {
  id?: string;
  roomCode?: string;
  inviteToken?: string;
  status?: "LOBBY" | "IN_PROGRESS" | "RESULTS" | "FINISHED" | "CANCELLED";
  phase?: "SUBMIT" | "VOTE" | "REVEAL";
  hostUserId?: string;
  hostName?: string;
  hostAvatarUrl?: string;
  deckId?: string;
  deckCoverImageUrl?: string;
  deckBackgroundImageUrl?: string;
  themeId?: string;
  deckSnapshot?: (
    | AllocationQuestion
    | DrawingQuestion
    | GridQuestion
    | MatchingQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
    | WordCloudQuestion
  )[];
  settings?: ShowcaseSettings;
  players?: ShowcasePlayerDto[];
  teamMode?: boolean;
  autoBalanceTeams?: boolean;
  teams?: Team[];
  anonymousMode?: boolean;
  customRoomCode?: string;
  allowReJoin?: boolean;
  spectatorCount?: number;
  lobbyOpenedAt?: string;
  currentRound?: number;
  createdAt?: string;
  startedAt?: string;
};
export type TeamCrudRequest = {
  name?: string;
  color?: string;
};
export type TeamMoveRequest = {
  teamId: string;
};
export type ShowcaseChatMessageDto = {
  id?: string;
  authorUserId?: string;
  authorName?: string;
  authorPictureUrl?: string;
  fromHost?: boolean;
  guest?: boolean;
  body?: string;
  sentAt?: string;
  moderated?: boolean;
  moderatedByUserId?: string;
  moderatedAt?: string;
};
export type GalleryImageResponse = {
  id?: string;
  name?: string;
  ownerId?: string;
  organizationId?: string;
  tags?: string[];
  variants?: ImageVariant[];
  createdAt?: string;
};
export type UpdateGalleryImageRequest = {
  name?: string;
  tags?: string[];
  organizationId?: string;
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
    | AllocationQuestion
    | DrawingQuestion
    | GridQuestion
    | MatchingQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
    | WordCloudQuestion
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
  isFavorited?: boolean;
  averageRating?: number;
  ratingCount?: number;
  myRating?: number;
  isRatedByMe?: boolean;
  myRole?: "VIEWER" | "EDITOR" | "OWNER";
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
export type DeckRatingDto = {
  id?: string;
  deckId?: string;
  userId?: string;
  userName?: string;
  userPictureUrl?: string;
  stars?: number;
  review?: string;
  createdAt?: string;
  updatedAt?: string;
};
export type RateDeckRequest = {
  stars?: number;
  review?: string;
};
export type DeckCollaboratorDto = {
  id?: string;
  deckId?: string;
  userId?: string;
  email?: string;
  userName?: string;
  name?: string;
  pictureUrl?: string;
  role?: "VIEWER" | "EDITOR" | "OWNER";
  invitedByUserId?: string;
  invitedAt?: string;
  acceptedAt?: string;
};
export type UpdateCollaboratorRoleRequest = {
  role: "VIEWER" | "EDITOR" | "OWNER";
};
export type DeckCommentDto = {
  id?: string;
  deckId?: string;
  authorUserId?: string;
  authorName?: string;
  authorPictureUrl?: string;
  parentCommentId?: string;
  body?: string;
  upvotes?: number;
  upvotedByMe?: boolean;
  edited?: boolean;
  deleted?: boolean;
  replyCount?: number;
  createdAt?: string;
  updatedAt?: string;
};
export type UpdateCommentRequest = {
  body: string;
};
export type DeckCollectionDto = {
  id?: string;
  ownerUserId?: string;
  organizationId?: string;
  name?: string;
  description?: string;
  cover?: Image;
  deckIds?: string[];
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  viewCount?: number;
  deckCount?: number;
  decks?: DeckDto[];
  createdAt?: string;
  updatedAt?: string;
};
export type UpdateDeckCollectionRequest = {
  name?: string;
  description?: string;
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  cover?: Image;
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
  picture?: Image;
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
  reactionsEnabled?: boolean;
  chatEnabled?: boolean;
  teamMode?: boolean;
  teamCount?: number;
  autoBalanceTeams?: boolean;
  customRoomCode?: string;
  anonymousMode?: boolean;
  shuffleQuestions?: boolean;
  shuffleAnswers?: boolean;
  autoAdvance?: boolean;
  podiumDuration?: number;
  lobbyCountdownSeconds?: number;
  requireFullName?: boolean;
  spectatorsAllowed?: boolean;
};
export type Reaction = {
  id?: string;
  showcaseId?: string;
  elementId?: string;
  userId?: string;
  userName?: string;
  guest?: boolean;
  emoji?: string;
  offsetMs?: number;
  sentAt?: string;
};
export type ReactionSendRequest = {
  emoji: string;
};
export type JoinShowcaseRequest = {
  teamId?: string;
  avatarKey?: string;
  colorTag?: string;
};
export type ChatSendRequest = {
  body: string;
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
export type DeckFavoriteResponse = {
  deckId?: string;
  isFavorited?: boolean;
  favoriteCount?: number;
};
export type DeckCommentsPage = {
  items?: DeckCommentDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
};
export type CreateCommentRequest = {
  body: string;
  parentCommentId?: string;
};
export type InviteCollaboratorRequest = {
  userIdOrEmail: string;
  role: "VIEWER" | "EDITOR" | "OWNER";
};
export type TransferOwnershipRequest = {
  userId: string;
};
export type CreateDeckCollectionRequest = {
  id?: string;
  name: string;
  description?: string;
  organizationId?: string;
  visibility?: "PRIVATE" | "UNLISTED" | "ORG" | "PUBLIC";
  cover?: Image;
};
export type AddDeckToCollectionRequest = {
  deckId: string;
  position?: number;
};
export type ReorderCollectionDecksRequest = {
  deckIds: string[];
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
  picture?: Image;
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
export type DeckFavoritesPage = {
  items?: DeckDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
};
export type PlayerPlacement = {
  userId?: string;
  userName?: string;
  finalScore?: number;
  placement?: number;
  correctAnswers?: number;
  totalQuestions?: number;
  teamId?: string;
  longestStreak?: number;
  accuracy?: number;
  reactionsSent?: number;
  guest?: boolean;
};
export type AnswerPayloadBase = {
  kind: string;
};
export type AllocationAnswer = {
  kind: "AllocationAnswer";
} & AnswerPayloadBase & {
    optionIdToPoints?: {
      [key: string]: number;
    };
  };
export type Stroke = {
  color?: string;
  thickness?: number;
  points?: number[];
};
export type DrawingAnswer = {
  kind: "DrawingAnswer";
} & AnswerPayloadBase & {
    strokes?: Stroke[];
  };
export type GridAnswer = {
  kind: "GridAnswer";
} & AnswerPayloadBase & {
    selectedCellIndexes?: number[];
  };
export type MatchingAnswer = {
  kind: "MatchingAnswer";
} & AnswerPayloadBase & {
    leftIdToRightId?: {
      [key: string]: string;
    };
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
export type WordCloudAnswer = {
  kind: "WordCloudAnswer";
} & AnswerPayloadBase & {
    words?: string[];
  };
export type PlayerRoundDetail = {
  userId?: string;
  userName?: string;
  payload?:
    | AllocationAnswer
    | DrawingAnswer
    | GridAnswer
    | MatchingAnswer
    | McqAnswer
    | NumberAnswer
    | PlaceOnImageAnswer
    | RankingAnswer
    | ScalesAnswer
    | TextAnswer
    | TimeoutAnswer
    | WordCloudAnswer;
  wasCorrect?: boolean;
  pointsAwarded?: number;
};
export type RoundReview = {
  round?: number;
  element?:
    | AllocationQuestion
    | DrawingQuestion
    | GridQuestion
    | MatchingQuestion
    | McqQuestion
    | NumberQuestion
    | PlaceOnImageQuestion
    | QAndAQuestion
    | RankingQuestion
    | ScalesQuestion
    | Slide
    | TextQuestion
    | WordCloudQuestion;
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
export type DeckRatingsPage = {
  items?: DeckRatingDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
  averageRating?: number;
  ratingCount?: number;
  starDistribution?: number[];
};
export type DeckExploreResponse = {
  items?: DeckDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
};
export type DeckCollectionsPage = {
  items?: DeckCollectionDto[];
  page?: number;
  size?: number;
  totalElements?: number;
  hasMore?: boolean;
};
export type AvatarPreset = {
  key?: string;
  displayName?: string;
  imageUrl?: string;
  colorTag?: string;
};
export type UserDto = GuestUser | RegisteredUser;
export const {
  useUpdateThemeMutation,
  useDeleteThemeMutation,
  useGetTagQuery,
  useLazyGetTagQuery,
  useUpdateTagMutation,
  useDeleteTagMutation,
  useUpdateTeamMutation,
  useDeleteTeamMutation,
  useMovePlayerToTeamMutation,
  useModerateChatMutation,
  useUpdateImageMutation,
  useDeleteImageMutation,
  useGetDeckQuery,
  useLazyGetDeckQuery,
  useUpdateDeckMutation,
  useDeleteDeckMutation,
  useRateDeckMutation,
  useDeleteMyRatingMutation,
  useUpdateElementMutation,
  useDeleteElementMutation,
  useUpdateCollaboratorRoleMutation,
  useRemoveCollaboratorMutation,
  useEditCommentMutation,
  useDeleteCommentMutation,
  useGetCollectionQuery,
  useLazyGetCollectionQuery,
  useUpdateCollectionMutation,
  useDeleteCollectionMutation,
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
  useCreateTeamMutation,
  useSendReactionMutation,
  useJoinByRoomCodeMutation,
  useListChatQuery,
  useLazyListChatQuery,
  useSendChatMutation,
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
  useFavoriteDeckMutation,
  useUnfavoriteDeckMutation,
  useRecountFavoritesMutation,
  useAddElementMutation,
  useMoveMcqOptionMutation,
  useMoveElementMutation,
  useListCommentsQuery,
  useLazyListCommentsQuery,
  usePostCommentMutation,
  useListCollaboratorsQuery,
  useLazyListCollaboratorsQuery,
  useInviteCollaboratorMutation,
  useTransferOwnershipMutation,
  useArchiveDeckMutation,
  useToggleCommentUpvoteMutation,
  useCreateCollectionMutation,
  useAddDeckToCollectionMutation,
  useReorderCollectionDecksMutation,
  useRegisterMutation,
  useGuestLoginMutation,
  useUpdateProfileMutation,
  useGetUserProfileQuery,
  useLazyGetUserProfileQuery,
  useListMyFavoritesQuery,
  useLazyListMyFavoritesQuery,
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
  useListRatingsQuery,
  useLazyListRatingsQuery,
  useGetMyRatingQuery,
  useLazyGetMyRatingQuery,
  useListRepliesQuery,
  useLazyListRepliesQuery,
  useListMyDecksQuery,
  useLazyListMyDecksQuery,
  useExploreDecksQuery,
  useLazyExploreDecksQuery,
  useListMyCollectionsQuery,
  useLazyListMyCollectionsQuery,
  useListQuery,
  useLazyListQuery,
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLoginQuery,
  useLazyLoginQuery,
  useLeaveOrgMutation,
  useRemoveDeckFromCollectionMutation,
} = injectedRtkApi;
