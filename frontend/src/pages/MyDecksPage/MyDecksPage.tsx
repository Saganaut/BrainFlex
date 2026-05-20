// Lists user-owned content decks and all system decks, with create/edit/delete actions.
import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { StarIcon, PlayIcon } from "@heroicons/react/24/outline";
import {
  BrainFlex,
  useListMyDecksQuery,
  useListDecksQuery,
  useDeleteDeckMutation,
  useCreateDeckMutation,
  useAddElementMutation,
} from "../../store/BrainFlexApi";
import type { DeckDto, Slide } from "../../store/BrainFlexApi";
import { useAppDispatch } from "../../store/hooks";
import { Btn } from "@/components/Common/Buttons/Btn";
import { DeckActionButton } from "@/components/Common/Buttons/DeckActionButton/DeckActionButton";
import { Badge } from "@/components/Common/Badge";
import { FavoriteHeart } from "@/components/Common/FavoriteHeart/FavoriteHeart";
import {
  DropdownMenu,
  DropdownMenuItem,
} from "@/components/Menus/DropdownMenu";
import { AddToCollectionModal } from "@/components/Collections/AddToCollectionModal";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { useModal } from "@/context/useModal";
import { resolveDeckCover } from "../../utils/deckImages";
import styles from "./MyDecksPage.module.css";

const PUBLISH_BADGE_VARIANT = {
  DRAFT: "info",
  PUBLISHED: "success",
  ARCHIVED: "warning",
} as const;

const ROLE_BADGE_VARIANT = {
  OWNER: "brand",
  EDITOR: "success",
  VIEWER: "info",
} as const;

const ROLE_BADGE_LABEL = {
  OWNER: "Owner",
  EDITOR: "Editor",
  VIEWER: "Viewer",
} as const;

const DIFFICULTY_LABEL = {
  EASY: "Easy",
  MEDIUM: "Medium",
  HARD: "Hard",
} as const;

const formatRating = (rating: number, count: number): string => {
  if (count <= 0) return "—";
  return rating.toFixed(1);
};

const DeckStatusRow = ({ deck }: { deck: DeckDto }) => {
  if (deck.isSystem) {
    return <Badge size='sm' variant='brand' label='System' />;
  }
  const status = deck.publishStatus ?? "DRAFT";
  const role = deck.myRole;
  return (
    <span className={styles.statusRow}>
      <Badge
        size='sm'
        variant={PUBLISH_BADGE_VARIANT[status]}
        label={status.charAt(0) + status.slice(1).toLowerCase()}
      />
      {role && role !== "OWNER" && (
        <Badge
          size='sm'
          variant={ROLE_BADGE_VARIANT[role]}
          label={ROLE_BADGE_LABEL[role]}
        />
      )}
    </span>
  );
};

const DeckDiscoveryRow = ({ deck }: { deck: DeckDto }) => {
  const plays = deck.playCount ?? 0;
  const rating = deck.averageRating ?? 0;
  const ratingCount = deck.ratingCount ?? 0;
  const language = deck.language ?? "en";
  const difficulty = deck.difficulty ?? "MEDIUM";

  return (
    <span className={styles.cardDiscovery}>
      <span className={styles.cardDiscoveryItem} aria-label='Plays'>
        <PlayIcon className={styles.cardIcon} />
        {plays}
      </span>
      <span className={styles.cardDiscoveryItem} aria-label='Average rating'>
        <StarIcon className={styles.cardIcon} />
        {formatRating(rating, ratingCount)}
      </span>
      <span className={styles.cardDiscoveryItem} aria-label='Language'>
        {language.toUpperCase()}
      </span>
      <span className={styles.cardDiscoveryItem} aria-label='Difficulty'>
        {DIFFICULTY_LABEL[difficulty]}
      </span>
    </span>
  );
};

/**
 * Starter slide stamped into every new deck so the editor never opens onto an
 * empty rail. Same id is used for the optimistic cache seed AND the persisted
 * `addElement` call after the deck is created — that way the slide stays
 * selected and visible without a flicker between optimistic and confirmed state.
 */
// Primitive defaults must mirror useDeckEditor.buildNewElement — Jackson
// cannot deserialize null into the backend's primitive boolean/int fields.
const buildFirstSlide = (id: string): Slide => ({
  kind: "Slide",
  id,
  slideKind: "TITLE",
  title: "Untitled slide",
  body: "",
  scored: false,
  survey: false,
  displaySeconds: 0,
  mediaPosition: "NONE",
  resultsDisplayType: "DEFAULT",
  multipleSelectionsEnabled: false,
  selectionsPerParticipant: 1,
  showResultsAsPercentage: false,
  joinType: "INSTRUCTIONS_BAR",
  showJoinInformation: true,
  showQrCode: false,
  showResponses: "INSTANT",
  tagIds: [],
  reactionsEnabled: true,
  version: 1,
});

const buildOptimisticDeck = (
  id: string,
  name: string,
  firstSlide: Slide,
): DeckDto => ({
  id,
  name,
  description: "",
  tags: [],
  isSystem: false,
  visibility: "PRIVATE",
  defaultSessionFormat: "GAME",
  elementCount: 1,
  elements: [firstSlide],
});
//TODO Extract this out into its own component, it should go with the other Card components - potentially be a variant
//TODO the cards should have a small menu button on the top right that toggles a dropdown which then exposes options
const DeckCard = ({
  deck,
  editable,
  onDelete,
}: {
  deck: DeckDto;
  editable: boolean;
  onDelete?: (id: string) => void;
}) => {
  const navigate = useNavigate();
  const { openModal, closeModal } = useModal();

  const handleAddToCollection = () => {
    if (deck.id == null) return;
    const deckId = deck.id;
    openModal({
      title: "Add to collection",
      content: <AddToCollectionModal deckId={deckId} onClose={closeModal} />,
    });
  };

  return (
    <DropdownMenu
      position='top-left'
      anchorToCursor
      trigger={(toggle) => (
        <div
          className={styles.card}
          onClick={() => {
            void navigate({ to: `/decks/${deck.id}/edit` });
          }}
          onContextMenu={(e) => {
            e.preventDefault();
            toggle(e);
          }}>
          <div className={styles.cardCoverWrap}>
            <img
              src={resolveDeckCover(deck.cover, deck.id)}
              alt=''
              className={styles.cardCover}
              loading='lazy'
            />
            {deck.id && (
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
          <span className={styles.cardName}>{deck.name}</span>
          <DeckStatusRow deck={deck} />
          <span className={styles.cardMeta}>
            {deck.elementCount ?? 0} elements
            {deck.tags && deck.tags.length > 0 ? ` · ${deck.tags[0]}` : ""}
          </span>
          <DeckDiscoveryRow deck={deck} />
          {deck.description && (
            <span className={styles.cardDesc}>{deck.description}</span>
          )}
          {deck.id && (
            <div className={styles.cardActions}>
              <DeckActionButton deckId={deck.id} size='sm' />
              {editable && (
                <>
                  <Link
                    to='/decks/$deckId/edit'
                    params={{ deckId: deck.id }}
                    search={{ questionId: undefined }}
                    viewTransition>
                    <Btn size='sm'>Edit</Btn>
                  </Link>
                  <Btn
                    size='sm'
                    variant='error'
                    onClick={() => {
                      if (deck.id) onDelete?.(deck.id);
                    }}>
                    Delete
                  </Btn>
                </>
              )}
            </div>
          )}
        </div>
      )}>
      <DropdownMenuItem onClick={handleAddToCollection}>
        Add to collection…
      </DropdownMenuItem>
    </DropdownMenu>
  );
};

const MyDecksPage = () => {
  // Gated by /_authenticated — caller is always a registered user here.
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const { data: systemDecks = [], isLoading: loadingSystem } =
    useListDecksQuery(undefined, { refetchOnMountOrArgChange: true });
  const {
    data: myDecks = [],
    isLoading: loadingMine,
    refetch,
  } = useListMyDecksQuery(undefined, {
    refetchOnMountOrArgChange: true,
  });

  const [deleteDeck] = useDeleteDeckMutation();
  const [createDeck] = useCreateDeckMutation();
  const [addElement] = useAddElementMutation();
  const confirm = useConfirm();

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: "Delete deck",
      message: "Delete this deck and all its questions?",
      confirmLabel: "Delete",
      variant: "danger",
    });
    if (!ok) return;
    await deleteDeck({ id }).unwrap();
    void refetch();
  };

  const handleCreateDeck = () => {
    const id = crypto.randomUUID();
    const name = "Untitled Deck";
    const firstSlide = buildFirstSlide(crypto.randomUUID());

    void dispatch(
      BrainFlex.util.upsertQueryData(
        "getDeck",
        { id },
        buildOptimisticDeck(id, name, firstSlide),
      ),
    );

    void navigate({
      to: "/decks/$deckId/edit",
      params: { deckId: id },
      search: { questionId: firstSlide.id },
    });

    void createDeck({ createDeckRequest: { id, name } })
      .unwrap()
      .then(() => addElement({ id, body: firstSlide }).unwrap())
      .catch((err: unknown) => {
        console.error("Failed to create deck", err);
      });
  };

  const userSystemDecks = systemDecks.filter((p) => p.isSystem);

  type Tab = "live" | "drafts" | "shared";
  const [tab, setTab] = useState<Tab>("live");
  // Owned = decks the caller is the OWNER of (or pre-backfill, has no role
  // but still appears in /api/decks/mine via the legacy creatorUserId path —
  // treat a missing role on a non-shared deck as owner).
  const ownedDecks = myDecks.filter(
    (d) => d.myRole == null || d.myRole === "OWNER",
  );
  const sharedDecks = myDecks.filter(
    (d) => d.myRole === "EDITOR" || d.myRole === "VIEWER",
  );
  // Draft = explicit DRAFT or legacy decks without a publishStatus field set.
  const draftDecks = ownedDecks.filter(
    (d) => (d.publishStatus ?? "DRAFT") === "DRAFT",
  );
  const liveDecks = ownedDecks.filter(
    (d) => (d.publishStatus ?? "DRAFT") !== "DRAFT",
  );
  const visibleDecks =
    tab === "drafts"
      ? draftDecks
      : tab === "shared"
        ? sharedDecks
        : liveDecks;

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>My Decks</h1>
        <Btn onClick={handleCreateDeck}>New Deck</Btn>
      </div>

      <section className={styles.section}>
          <div className={styles.tabs} role='tablist' aria-label='Your decks'>
            <button
              type='button'
              role='tab'
              aria-selected={tab === "live"}
              className={[styles.tab, tab === "live" ? styles.tabActive : ""]
                .filter(Boolean)
                .join(" ")}
              onClick={() => {
                setTab("live");
              }}>
              Live
              <span className={styles.tabCount}>{liveDecks.length}</span>
            </button>
            <button
              type='button'
              role='tab'
              aria-selected={tab === "drafts"}
              className={[styles.tab, tab === "drafts" ? styles.tabActive : ""]
                .filter(Boolean)
                .join(" ")}
              onClick={() => {
                setTab("drafts");
              }}>
              Drafts
              <span className={styles.tabCount}>{draftDecks.length}</span>
            </button>
            <button
              type='button'
              role='tab'
              aria-selected={tab === "shared"}
              className={[styles.tab, tab === "shared" ? styles.tabActive : ""]
                .filter(Boolean)
                .join(" ")}
              onClick={() => {
                setTab("shared");
              }}>
              Shared with me
              <span className={styles.tabCount}>{sharedDecks.length}</span>
            </button>
          </div>
          {loadingMine ? (
            <p className={styles.empty}>Loading…</p>
          ) : visibleDecks.length === 0 ? (
            <p className={styles.empty}>
              {tab === "drafts"
                ? "No drafts. Decks default to draft until you publish them."
                : tab === "shared"
                  ? "Nothing shared with you yet. Owners can invite you from the Share button in their deck editor."
                  : "No published decks yet. Hit Publish in the editor when you're ready."}
            </p>
          ) : (
            <div className={styles.grid}>
              {visibleDecks.map((deck) => (
                <DeckCard
                  key={deck.id}
                  deck={deck}
                  editable={
                    tab !== "shared" || deck.myRole === "EDITOR"
                  }
                  onDelete={(id) => {
                    void handleDelete(id);
                  }}
                />
              ))}
            </div>
          )}
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>System Decks</h2>
        {loadingSystem ? (
          <p className={styles.empty}>Loading…</p>
        ) : (
          <div className={styles.grid}>
            {userSystemDecks.map((deck) => (
              <DeckCard key={deck.id} deck={deck} editable={false} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export { MyDecksPage };
