import { configureStore } from "@reduxjs/toolkit";
import { emptySplitApi } from "./emptyApi";
import gameReducer from "./gameSlice";
// Side-effect import: layers cache-sync onQueryStarted handlers onto the
// auto-generated BrainFlex mutations so mutation responses update getDeck.
import "./apiEnhancements";

export const store = configureStore({
  reducer: {
    [emptySplitApi.reducerPath]: emptySplitApi.reducer,
    game: gameReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(emptySplitApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
