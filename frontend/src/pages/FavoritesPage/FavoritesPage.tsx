// /my-favorites — paginated grid of decks the caller has starred. Reuses the
// same card visual language as MyDecks; the heart on each tile doubles as the
// remove-from-favorites toggle thanks to the shared optimistic update wiring
// in apiEnhancements.ts.
import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useListMyFavoritesQuery } from "@/store/BrainFlexApi";
import { Btn } from "@/components/Common/Buttons/Btn";
import { FavoriteHeart } from "@/components/Common/FavoriteHeart/FavoriteHeart";
import { resolveDeckCover } from "@/utils/deckImages";
import styles from "./FavoritesPage.module.css";

const PAGE_SIZE = 24;

const FavoritesPage = () => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";
  const [page, setPage] = useState(0);

  const { data, isFetching } = useListMyFavoritesQuery(
    { page, size: PAGE_SIZE },
    { skip: !isRegistered, refetchOnMountOrArgChange: true },
  );

  const items = data?.items ?? [];
  const total = data?.totalElements ?? 0;
  const hasMore = data?.hasMore ?? false;

  if (!isRegistered) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>Favorites</h1>
        <p className={styles.empty}>Sign in to see your favorited decks.</p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>Favorites</h1>
        <span className={styles.subtitle}>
          {isFetching ? "Loading…" : `${String(total)} decks`}
        </span>
      </header>

      {items.length === 0 && !isFetching ? (
        <p className={styles.empty}>
          You haven&apos;t favorited any decks yet. Tap the heart on any deck
          card to add it here.
        </p>
      ) : (
        <div className={styles.grid}>
          {items.map((deck) => (
            <Link
              key={deck.id}
              to='/decks/$deckId/edit'
              params={{ deckId: deck.id ?? "" }}
              search={{ questionId: undefined }}
              className={styles.card}>
              <div className={styles.coverWrap}>
                <img
                  src={resolveDeckCover(deck.cover, deck.id ?? "")}
                  alt=''
                  className={styles.cover}
                  loading='lazy'
                />
                {deck.id != null && deck.id !== "" && (
                  <span className={styles.heart}>
                    <FavoriteHeart
                      deckId={deck.id}
                      isFavorited={deck.isFavorited ?? true}
                      favoriteCount={deck.favoriteCount}
                      showCount
                      size='sm'
                    />
                  </span>
                )}
              </div>
              <span className={styles.name}>{deck.name}</span>
              <span className={styles.meta}>
                {deck.elementCount ?? 0} elements
                {deck.tags && deck.tags.length > 0 ? ` · ${deck.tags[0]}` : ""}
              </span>
            </Link>
          ))}
        </div>
      )}

      {(page > 0 || hasMore) && (
        <div className={styles.pager}>
          <Btn
            size='sm'
            shape='pill'
            disabled={page === 0 || isFetching}
            onClick={() => {
              setPage((p) => Math.max(0, p - 1));
            }}>
            Previous
          </Btn>
          <span className={styles.pageInfo}>Page {page + 1}</span>
          <Btn
            size='sm'
            shape='pill'
            disabled={!hasMore || isFetching}
            onClick={() => {
              setPage((p) => p + 1);
            }}>
            Next
          </Btn>
        </div>
      )}
    </div>
  );
};

export { FavoritesPage };
