// Lists user-owned content decks and all system decks, with create/edit/delete actions.
import { Link, useNavigate } from "@tanstack/react-router";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import {
  BrainFlex,
  useListMyDecksQuery,
  useListDecksQuery,
  useDeleteDeckMutation,
  useCreateDeckMutation,
} from "../../store/BrainFlexApi";
import type { DeckDto } from "../../store/BrainFlexApi";
import { useAppDispatch } from "../../store/hooks";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { resolveDeckCover } from "../../utils/deckImages";
import styles from "./MyDecksPage.module.css";

const buildOptimisticDeck = (id: string, name: string): DeckDto => ({
  id,
  name,
  description: "",
  tags: [],
  isSystem: false,
  visibility: "PRIVATE",
  recommendedPreset: "GAME",
  elementCount: 0,
  elements: [],
});

const DeckCard = ({
  deck,
  editable,
  onDelete,
}: {
  deck: DeckDto;
  editable: boolean;
  onDelete?: (id: string) => void;
}) => (
  <div className={styles.card}>
    <img
      src={resolveDeckCover(deck.coverImageUrl, deck.id)}
      alt=''
      className={styles.cardCover}
      loading='lazy'
    />
    <span className={styles.cardName}>{deck.name}</span>
    {deck.isSystem && <span className={styles.systemBadge}>System</span>}
    <span className={styles.cardMeta}>
      {deck.elementCount ?? 0} elements
      {deck.tags && deck.tags.length > 0 ? ` · ${deck.tags[0]}` : ""}
    </span>
    {deck.description && (
      <span className={styles.cardDesc}>{deck.description}</span>
    )}
    {editable && deck.id && (
      <div className={styles.cardActions}>
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
      </div>
    )}
  </div>
);

const MyDecksPage = () => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const { data: systemDecks = [], isLoading: loadingSystem } =
    useListDecksQuery(undefined, { refetchOnMountOrArgChange: true });
  const {
    data: myDecks = [],
    isLoading: loadingMine,
    refetch,
  } = useListMyDecksQuery(undefined, {
    skip: !isRegistered,
    refetchOnMountOrArgChange: true,
  });

  const [deleteDeck] = useDeleteDeckMutation();
  const [createDeck] = useCreateDeckMutation();
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

    void dispatch(
      BrainFlex.util.upsertQueryData(
        "getDeck",
        { id },
        buildOptimisticDeck(id, name),
      ),
    );

    void navigate({
      to: "/decks/$deckId/edit",
      params: { deckId: id },
      search: { questionId: undefined },
    });

    void createDeck({ createDeckRequest: { id, name } })
      .unwrap()
      .catch((err: unknown) => {
        console.error("Failed to create deck", err);
      });
  };

  const userSystemDecks = systemDecks.filter((p) => p.isSystem);

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>My Decks</h1>
        {isRegistered && <Btn onClick={handleCreateDeck}>New Deck</Btn>}
      </div>

      {isRegistered && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Your Decks</h2>
          {loadingMine ? (
            <p className={styles.empty}>Loading…</p>
          ) : myDecks.length === 0 ? (
            <p className={styles.empty}>You haven't created any decks yet.</p>
          ) : (
            <div className={styles.grid}>
              {myDecks.map((deck) => (
                <DeckCard
                  key={deck.id}
                  deck={deck}
                  editable
                  onDelete={(id) => {
                    void handleDelete(id);
                  }}
                />
              ))}
            </div>
          )}
        </section>
      )}

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
