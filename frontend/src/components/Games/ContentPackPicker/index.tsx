/**
 * Content pack selection grid for the Create Game flow.
 * Fetches available packs from GET /api/content-packs and renders
 * selectable cards so the host can pick which pack to play.
 */
import { useListPacksQuery } from "../../../store/BrainFlexApi";
import styles from "./ContentPackPicker.module.css";

type Props = {
  selectedPackId: string | null;
  onSelect: (packId: string) => void;
};

export function ContentPackPicker({ selectedPackId, onSelect }: Props) {
  const { data: packs, isLoading, isError } = useListPacksQuery();

  if (isLoading) return <p className={styles.message}>Loading packs…</p>;
  if (isError) return <p className={styles.message}>Failed to load content packs.</p>;
  if (!packs?.length) return <p className={styles.message}>No content packs available.</p>;

  return (
    <div className={styles.grid}>
      {packs.map((pack) => (
        <button
          key={pack.id}
          type="button"
          className={`${styles.pack} ${selectedPackId === pack.id ? styles.selected : ""}`}
          onClick={() => pack.id && onSelect(pack.id)}
        >
          <span className={styles.packName}>{pack.name}</span>
          <span className={styles.packMeta}>
            {pack.questionCount ?? 0} questions
            {pack.category ? ` · ${pack.category}` : ""}
          </span>
          {pack.description && (
            <span className={styles.packDesc}>{pack.description}</span>
          )}
        </button>
      ))}
    </div>
  );
}
