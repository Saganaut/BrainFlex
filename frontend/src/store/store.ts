import { configureStore } from "@reduxjs/toolkit";
import { emptySplitApi } from "./emptyApi"; // Adjust this path to your emptyApi file

export const store = configureStore({
  reducer: {
    [emptySplitApi.reducerPath]: emptySplitApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(emptySplitApi.middleware),
});
