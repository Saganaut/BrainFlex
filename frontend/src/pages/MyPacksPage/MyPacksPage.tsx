// Lists user-owned content packs and all system packs, with create/edit/delete actions.
import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import {
  useListMyPacksQuery,
  useListPacksQuery,
  useDeletePackMutation,
} from "../../store/BrainFlexApi";
import type { ContentPackDto } from "../../store/BrainFlexApi";
import { Btn } from "@/components/Common/Buttons/Btn";
import styles from "./MyPacksPage.module.css";

const PackCard = ({
  pack,
  editable,
  onDelete,
}: {
  pack: ContentPackDto;
  editable: boolean;
  onDelete?: (id: string) => void;
}) => (
  <div className={styles.card}>
    <span className={styles.cardName}>{pack.name}</span>
    {pack.isSystem && <span className={styles.systemBadge}>System</span>}
    <span className={styles.cardMeta}>
      {pack.questionCount ?? 0} questions
      {pack.category ? ` · ${pack.category}` : ""}
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
    useListPacksQuery(undefined, { refetchOnMountOrArgChange: true });
  const {
    data: myPacks = [],
    isLoading: loadingMine,
    refetch,
  } = useListMyPacksQuery(undefined, {
    skip: !isRegistered,
    refetchOnMountOrArgChange: true,
  });

  const [deletePack] = useDeletePackMutation();

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
