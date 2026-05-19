// /my-decks/collections — paginated grid of the caller's curated deck
// collections. Visually mirrors the my-decks card surface; tapping a tile
// drills into the collection's ordered deck list at /collections/$id.
// "New collection" opens a modal-driven create flow that seeds the cache
// optimistically and navigates to the new collection's detail view as soon
// as the server confirms the id, the same shape as the deck-create flow.
import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { PlusIcon } from "@heroicons/react/24/outline";
import { Btn } from "@/components/Common/Buttons/Btn";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { useModal } from "@/context/useModal";
import {
  useListMyCollectionsQuery,
  useDeleteCollectionMutation,
} from "@/store/BrainFlexApi";
import { useConfirm } from "@/components/Common/ConfirmDialog/useConfirm";
import { resolveDeckCover } from "@/utils/deckImages";
import { CollectionCreateForm } from "./CollectionCreateForm";
import styles from "./CollectionsPage.module.css";

const PAGE_SIZE = 24;

const CollectionsPage = () => {
  const userState = useCurrentUser();
  const isRegistered = userState.state === "registered";
  const navigate = useNavigate();
  const { openModal, closeModal } = useModal();
  const confirm = useConfirm();

  const [page, setPage] = useState(0);
  const { data, isFetching } = useListMyCollectionsQuery(
    { page, size: PAGE_SIZE },
    { skip: !isRegistered, refetchOnMountOrArgChange: true },
  );

  const [deleteCollection] = useDeleteCollectionMutation();

  const items = data?.items ?? [];
  const total = data?.totalElements ?? 0;
  const hasMore = data?.hasMore ?? false;

  const handleCreate = () => {
    openModal({
      title: "New collection",
      content: (
        <CollectionCreateForm
          onCancel={closeModal}
          onCreated={(id) => {
            closeModal();
            void navigate({
              to: "/collections/$collectionId",
              params: { collectionId: id },
            });
          }}
        />
      ),
    });
  };

  const handleDelete = async (id: string, name: string) => {
    const ok = await confirm({
      title: "Delete collection",
      message: `Delete "${name}"? The decks inside stay, only the grouping is removed.`,
      confirmLabel: "Delete",
      variant: "danger",
    });
    if (!ok) return;
    await deleteCollection({ id }).unwrap();
  };

  if (!isRegistered) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>Collections</h1>
        <p className={styles.empty}>
          Sign in to organize your decks into collections.
        </p>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>Collections</h1>
        <span className={styles.subtitle}>
          {isFetching ? "Loading…" : `${String(total)} collections`}
        </span>
        <Btn onClick={handleCreate}>New collection</Btn>
      </header>

      {items.length === 0 && !isFetching ? (
        <button
          type='button'
          className={styles.emptyTile}
          onClick={handleCreate}
          aria-label='Create your first collection'>
          <span className={styles.emptyIcon} aria-hidden='true'>
            <PlusIcon />
          </span>
          <span className={styles.emptyTitle}>
            Create your first collection
          </span>
          <span className={styles.emptySubtitle}>
            Group decks into a course, a series, or just a folder.
          </span>
        </button>
      ) : (
        <div className={styles.grid}>
          {items.map((col) => (
            <div key={col.id} className={styles.cardWrap}>
              <Link
                to='/collections/$collectionId'
                params={{ collectionId: col.id ?? "" }}
                className={styles.card}>
                <img
                  src={resolveDeckCover(col.cover, col.id)}
                  alt=''
                  className={styles.cover}
                  loading='lazy'
                />
                <span className={styles.name}>{col.name}</span>
                <span className={styles.meta}>
                  {col.deckCount ?? 0} decks
                  {col.visibility && col.visibility !== "PRIVATE"
                    ? ` · ${col.visibility.toLowerCase()}`
                    : ""}
                </span>
                {col.description != null && col.description !== "" && (
                  <span className={styles.desc}>{col.description}</span>
                )}
              </Link>
              <div className={styles.cardActions}>
                <Btn
                  size='sm'
                  variant='error'
                  onClick={() => {
                    void handleDelete(
                      col.id ?? "",
                      col.name ?? "this collection",
                    );
                  }}>
                  Delete
                </Btn>
              </div>
            </div>
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

export { CollectionsPage };
