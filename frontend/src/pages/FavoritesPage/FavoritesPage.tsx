// /my-favorites — paginated grid of decks the caller has starred. Reuses the
// same card visual language as MyDecks; the heart on each tile doubles as the
// remove-from-favorites toggle thanks to the shared optimistic update wiring
// in apiEnhancements.ts.
import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { useListMyFavoritesQuery } from "@/store/BrainFlexApi";
import { FavoriteHeart } from "@/components/Common/FavoriteHeart/FavoriteHeart";
import { Pagination } from "@/components/Common/Pagination/Pagination";
import { resolveDeckCover } from "@/utils/deckImages";
import styles from "./FavoritesPage.module.css";

const PAGE_SIZE = 24;

const FavoritesPage = () => {
  // Gated by /_authenticated — caller is always a registered user here.
  const [page, setPage] = useState(0);

  const { data, isFetching } = useListMyFavoritesQuery(
    { page, size: PAGE_SIZE },
    { refetchOnMountOrArgChange: true },
  );

  const items = data?.items ?? [];
  const total = data?.totalElements ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

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

      {pageCount > 1 && (
        <div className={styles.pager}>
          <Pagination
            page={page}
            pageCount={pageCount}
            disabled={isFetching}
            ariaLabel='Favorites pagination'
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
};

export { FavoritesPage };
