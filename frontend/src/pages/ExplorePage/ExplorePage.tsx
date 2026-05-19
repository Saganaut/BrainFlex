// Curated discovery surface for PUBLIC + PUBLISHED decks. Now backed by the
// dedicated /api/decks/explore endpoint so filtering + sorting happen on the
// server (and the response carries denormalized rating / playCount fields
// the deck card surfaces).
//
// Layout: filter sidebar on the left (subject chips, language, difficulty)
// + sort dropdown + paginated deck grid. "Load more" pulls the next page;
// changing any filter resets to page 0.
import { Link } from "@tanstack/react-router";
import { useState } from "react";
import { PlayIcon, StarIcon } from "@heroicons/react/24/outline";
import {
  useExploreDecksQuery,
  useListTagsQuery,
  type DeckDto,
  type ExploreDecksApiArg,
} from "@/store/BrainFlexApi";
import { Tag } from "@/components/Common/Tag/Tag";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { FavoriteHeart } from "@/components/Common/FavoriteHeart/FavoriteHeart";
import { resolveDeckCover } from "@/utils/deckImages";
import styles from "./ExplorePage.module.css";

type SortKey = "trending" | "new" | "top-rated" | "most-played";
type DifficultyKey = NonNullable<DeckDto["difficulty"]>;
type LanguageKey = "en" | "es" | "fr" | "de";

const PAGE_SIZE = 12;

const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: "trending", label: "Trending" },
  { value: "new", label: "New" },
  { value: "top-rated", label: "Top rated" },
  { value: "most-played", label: "Most played" },
];

const LANGUAGE_OPTIONS: { value: LanguageKey | "all"; label: string }[] = [
  { value: "all", label: "Any language" },
  { value: "en", label: "English" },
  { value: "es", label: "Spanish" },
  { value: "fr", label: "French" },
  { value: "de", label: "German" },
];

const DIFFICULTY_OPTIONS: { value: DifficultyKey | "all"; label: string }[] = [
  { value: "all", label: "Any difficulty" },
  { value: "EASY", label: "Easy" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HARD", label: "Hard" },
];

const formatRating = (rating: number, count: number): string => {
  if (count <= 0) return "—";
  return rating.toFixed(1);
};

const ExploreCard = ({ deck }: { deck: DeckDto }) => (
  <li className={styles.card}>
    <Link
      to='/decks/$deckId/edit'
      params={{ deckId: deck.id ?? "" }}
      search={{ questionId: undefined }}
      className={styles.cardLink}>
      <div className={styles.cardCoverWrap}>
        <img
          src={resolveDeckCover(deck.cover, deck.id ?? "")}
          alt=''
          loading='lazy'
          className={styles.cardCover}
        />
        {deck.id != null && deck.id !== "" && (
          <span className={styles.cardHeart}>
            <FavoriteHeart
              deckId={deck.id}
              isFavorited={deck.isFavorited ?? false}
              favoriteCount={deck.favoriteCount}
              showCount
              size='sm'
            />
          </span>
        )}
      </div>
      <div className={styles.cardBody}>
        <span className={styles.cardName}>{deck.name}</span>
        {deck.description != null && deck.description !== "" && (
          <span className={styles.cardDesc}>{deck.description}</span>
        )}
        <div className={styles.cardMeta}>
          <span className={styles.cardMetaItem} ariaLabel='Plays'>
            <PlayIcon className={styles.cardIcon} />
            {deck.playCount ?? 0}
          </span>
          <span className={styles.cardMetaItem} ariaLabel='Rating'>
            <StarIcon className={styles.cardIcon} />
            {formatRating(deck.averageRating ?? 0, deck.ratingCount ?? 0)}
          </span>
          <span className={styles.cardMetaItem}>
            {(deck.language ?? "en").toUpperCase()}
          </span>
          <span className={styles.cardMetaItem}>
            {deck.difficulty ?? "MEDIUM"}
          </span>
        </div>
        {deck.tags != null && deck.tags.length > 0 && (
          <div className={styles.cardTags}>
            {deck.tags.slice(0, 3).map((t) => (
              <Tag key={t} size='sm'>
                {t}
              </Tag>
            ))}
          </div>
        )}
      </div>
    </Link>
  </li>
);

const ExplorePage = () => {
  const [activeTagId, setActiveTagId] = useState<string | null>(null);
  const [language, setLanguage] = useState<LanguageKey | "all">("all");
  const [difficulty, setDifficulty] = useState<DifficultyKey | "all">("all");
  const [sort, setSort] = useState<SortKey>("trending");
  const [page, setPage] = useState(0);

  // Every filter setter rewinds to page 0 — there's no notion of "load more"
  // here, just Previous / Next paging, so we don't need a client accumulator.
  const changeFilter = (apply: () => void) => {
    apply();
    setPage(0);
  };

  const { data: curatedTags = [] } = useListTagsQuery({ curated: true });

  const exploreArgs: ExploreDecksApiArg = {
    tagId: activeTagId ?? undefined,
    language: language === "all" ? undefined : language,
    difficulty: difficulty === "all" ? undefined : difficulty,
    sort,
    page,
    size: PAGE_SIZE,
  };
  const { data: response, isFetching } = useExploreDecksQuery(exploreArgs);

  const items = response?.items ?? [];
  const hasMore = response?.hasMore ?? false;
  const total = response?.totalElements ?? 0;

  return (
    <div className={styles.explore}>
      <header className={styles.header}>
        <h1 className={styles.title}>Explore</h1>
        <p className={styles.subtitle}>
          Browse published decks by subject. Filter on the left, sort on the
          right.
        </p>
      </header>

      <div className={styles.layout}>
        <aside className={styles.sidebar} ariaLabel='Deck filters'>
          <section className={styles.filterSection}>
            <h2 className={styles.filterHeading}>Subject</h2>
            <div
              className={styles.subjectChips}
              role='radiogroup'
              ariaLabel='Subject'>
              <button
                type='button'
                role='radio'
                aria-checked={activeTagId === null}
                className={[
                  styles.tagChip,
                  activeTagId === null ? styles.tagChipActive : "",
                ]
                  .filter(Boolean)
                  .join(" ")}
                onClick={() => {
                  changeFilter(() => {
                    setActiveTagId(null);
                  });
                }}>
                All
              </button>
              {curatedTags.map((tag) => (
                <button
                  key={tag.id}
                  type='button'
                  role='radio'
                  aria-checked={activeTagId === tag.id}
                  className={[
                    styles.tagChip,
                    activeTagId === tag.id ? styles.tagChipActive : "",
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  onClick={() => {
                    changeFilter(() => {
                      setActiveTagId(tag.id ?? null);
                    });
                  }}>
                  <span>{tag.displayName}</span>
                  <span className={styles.tagCount}>{tag.deckCount ?? 0}</span>
                </button>
              ))}
            </div>
          </section>

          <section className={styles.filterSection}>
            <Dropdown
              label='Language'
              options={LANGUAGE_OPTIONS}
              value={[language]}
              onChange={(values) => {
                changeFilter(() => {
                  setLanguage(values[0] as LanguageKey | "all");
                });
              }}
            />
          </section>

          <section className={styles.filterSection}>
            <Dropdown
              label='Difficulty'
              options={DIFFICULTY_OPTIONS}
              value={[difficulty]}
              onChange={(values) => {
                changeFilter(() => {
                  setDifficulty(values[0] as DifficultyKey | "all");
                });
              }}
            />
          </section>
        </aside>

        <section className={styles.main}>
          <div className={styles.toolbar}>
            <span className={styles.resultsCount}>
              {isFetching ? "Loading…" : `${String(total)} decks`}
            </span>
            <Dropdown
              label='Sort by'
              labelPosition='labelInFront'
              options={SORT_OPTIONS}
              value={[sort]}
              onChange={(values) => {
                changeFilter(() => {
                  setSort(values[0] as SortKey);
                });
              }}
            />
          </div>

          {items.length === 0 && !isFetching ? (
            <p className={styles.empty}>No decks match these filters yet.</p>
          ) : (
            <ul className={styles.grid}>
              {items.map((deck) => (
                <ExploreCard key={deck.id} deck={deck} />
              ))}
            </ul>
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
              <span className={styles.pagerInfo}>Page {page + 1}</span>
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
        </section>
      </div>
    </div>
  );
};

export { ExplorePage };
