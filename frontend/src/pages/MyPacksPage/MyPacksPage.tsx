// Lists user-owned content packs and all system packs, with create/edit/delete actions.
import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import {
  useListMyDecksQuery,
  useListDecksQuery,
  useDeleteDeckMutation,
} from "../../store/BrainFlexApi";
import type { DeckDto } from "../../store/BrainFlexApi";
import { Btn } from "@/components/Common/Buttons/Btn";
import { resolveDeckCover } from "../../utils/deckImages";
import styles from "./MyPacksPage.module.css";

const PackCard = ({
  pack,
  editable,
  onDelete,
}: {
  pack: DeckDto;
  editable: boolean;
  onDelete?: (id: string) => void;
}) => (
  <div className={styles.card}>
    <img
      src={resolveDeckCover(pack.coverImageUrl, pack.id)}
      alt=''
      className={styles.cardCover}
      loading='lazy'
    />
    <span className={styles.cardName}>{pack.name}</span>
    {pack.isSystem && <span className={styles.systemBadge}>System</span>}
    <span className={styles.cardMeta}>
      {pack.elementCount ?? 0} elements
      {pack.tags && pack.tags.length > 0 ? ` · ${pack.tags[0]}` : ""}
    </span>
    {pack.description && (
      <span className={styles.cardDesc}>{pack.description}</span>
    )}
    {editable && pack.id && (
      <div className={styles.cardActions}>
        <Link
          to='/my-packs/$packId/edit'
          params={{ packId: pack.id }}
          viewTransition>
          <Btn size='sm'>Edit</Btn>
        </Link>
        <Btn
          size='sm'
          variant='error'
          onClick={() => {
            if (pack.id) onDelete?.(pack.id);
          }}>
          Delete
        </Btn>
      </div>
    )}
  </div>
);

const MyPacksPage = () => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";

  const { data: systemPacks = [], isLoading: loadingSystem } =
    useListDecksQuery(undefined, { refetchOnMountOrArgChange: true });
  const {
    data: myPacks = [],
    isLoading: loadingMine,
    refetch,
  } = useListMyDecksQuery(undefined, {
    skip: !isRegistered,
    refetchOnMountOrArgChange: true,
  });

  const [deletePack] = useDeleteDeckMutation();

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this content pack and all its questions?")) return;
    await deletePack({ id }).unwrap();
    void refetch();
  };

  const userSystemPacks = systemPacks.filter((p) => p.isSystem);

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>My Content Packs</h1>
        {isRegistered && (
          <Link to='/my-packs/create' viewTransition>
            <Btn>New Pack</Btn>
          </Link>
        )}
      </div>

      {isRegistered && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Your Packs</h2>
          {loadingMine ? (
            <p className={styles.empty}>Loading…</p>
          ) : myPacks.length === 0 ? (
            <p className={styles.empty}>You haven't created any packs yet.</p>
          ) : (
            <div className={styles.grid}>
              {myPacks.map((pack) => (
                <PackCard
                  key={pack.id}
                  pack={pack}
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
        <h2 className={styles.sectionTitle}>System Packs</h2>
        {loadingSystem ? (
          <p className={styles.empty}>Loading…</p>
        ) : (
          <div className={styles.grid}>
            {userSystemPacks.map((pack) => (
              <PackCard key={pack.id} pack={pack} editable={false} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export { MyPacksPage };
