/**
 * Content pack selection grid for the Create Game flow.
 * Shows system packs and the user's own packs (when logged in).
 * A "Create new pack" link lets registered users build a custom pack and return here.
 */
import { Link } from "@tanstack/react-router";
import { useCurrentUser } from "../../../hooks/useCurrentUser";
import { useListDecksQuery, useListMyDecksQuery } from "../../../store/BrainFlexApi";
import type { DeckDto } from "../../../store/BrainFlexApi";
import styles from "./ContentPackPicker.module.css";

interface ContentPackPickerProps {
  selectedPackId: string | null;
  onSelect: (packId: string) => void;
}

const PackButton = ({
  pack,
  selected,
  onSelect,
}: {
  pack: DeckDto;
  selected: boolean;
  onSelect: (id: string) => void;
}) => (
  <button
    type='button'
    className={`${styles.pack} ${selected ? styles.selected : ""}`}
    onClick={() => { if (pack.id) onSelect(pack.id); }}>
    <span className={styles.packName}>{pack.name}</span>
    <span className={styles.packMeta}>
      {pack.elementCount ?? 0} elements
      {pack.tags && pack.tags.length > 0 ? ` · ${pack.tags[0]}` : ""}
    </span>
    {pack.description && (
      <span className={styles.packDesc}>{pack.description}</span>
    )}
  </button>
);

const ContentPackPicker = ({ selectedPackId, onSelect }: ContentPackPickerProps) => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";

  const { data: publicPacks = [], isLoading: loadingPublic, isError } = useListDecksQuery();
  const { data: myPacks = [], isLoading: loadingMine } = useListMyDecksQuery(
    undefined,
    { skip: !isRegistered },
  );

  const isLoading = loadingPublic || (isRegistered && loadingMine);
  const systemPacks = publicPacks.filter((p) => p.isSystem);

  if (isLoading) return <p className={styles.message}>Loading packs…</p>;
  if (isError) return <p className={styles.message}>Failed to load content packs.</p>;

  return (
    <div>
      {isRegistered && myPacks.length > 0 && (
        <div className={styles.section}>
          <span className={styles.sectionLabel}>Your Packs</span>
          <div className={styles.grid}>
            {myPacks.map((pack) => (
              <PackButton
                key={pack.id}
                pack={pack}
                selected={selectedPackId === pack.id}
                onSelect={onSelect}
              />
            ))}
          </div>
        </div>
      )}

      <div className={styles.section}>
        <span className={styles.sectionLabel}>System Packs</span>
        {systemPacks.length > 0 ? (
          <div className={styles.grid}>
            {systemPacks.map((pack) => (
              <PackButton
                key={pack.id}
                pack={pack}
                selected={selectedPackId === pack.id}
                onSelect={onSelect}
              />
            ))}
          </div>
        ) : (
          <p className={styles.message}>No content packs available.</p>
        )}
      </div>

      {isRegistered && (
        <Link
          to='/my-packs/create'
          search={{ returnTo: "/games/create" }}
          className={styles.createLink}
          viewTransition>
          + Create your own pack
        </Link>
      )}
    </div>
  );
};

export { ContentPackPicker };
