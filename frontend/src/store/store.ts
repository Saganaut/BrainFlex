import { configureStore } from "@reduxjs/toolkit";
import { emptySplitApi } from "./emptyApi";
import interactiveSessionReducer from "./interactiveSessionSlice";
// Side-effect import: layers cache-sync onQueryStarted handlers onto the
// auto-generated BrainFlex mutations so mutation responses update getDeck.
import "./apiEnhancements";

export const store = configureStore({
  reducer: {
    [emptySplitApi.reducerPath]: emptySplitApi.reducer,
    interactiveSession: interactiveSessionReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(emptySplitApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
