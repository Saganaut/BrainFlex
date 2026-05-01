import { emptySplitApi } from "./emptyApi";

const registrationApi = emptySplitApi.injectEndpoints({
  endpoints: (build) => ({
    checkUsername: build.query<{ available: boolean }, string>({
      query: (username) => ({
        url: `/api/users/check-username`,
        params: { username },
      }),
    }),
  }),
  overrideExisting: false,
});

export const { useLazyCheckUsernameQuery } = registrationApi;
