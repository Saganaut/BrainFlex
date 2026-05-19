// Multi-select tag chooser backed by the /api/tags endpoint. Renders selected
// tags as removable pills and a typeahead input that shows matching tags as a
// dropdown menu below. Selecting a row in the menu adds it; clicking the ✕ on
// a pill removes it. Suppressed when the user hasn't authenticated (the tags
// API requires ROLE_USER); the caller should hide the surface in that case.
import { useMemo, useRef, useState } from "react";
import {
  useListTagsQuery,
  type TagResponse,
} from "@/store/BrainFlexApi";
import { Tag } from "../Tag/Tag";
import styles from "./TagPicker.module.css";

interface TagPickerProps {
  value: string[];
  onChange: (next: string[]) => void;
  placeholder?: string;
  // When provided, restricts the dropdown to descendants of this root tag.
  parentTagId?: string;
  // When set, only curated tags appear. Used by the Explore filter row.
  curatedOnly?: boolean;
  label?: string;
}

const TagPicker = ({
  value,
  onChange,
  placeholder = "Add a tag…",
  parentTagId,
  curatedOnly,
  label,
}: TagPickerProps) => {
  const [query, setQuery] = useState("");
  const [isFocused, setIsFocused] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const { data: tags = [], isLoading } = useListTagsQuery({
    curated: curatedOnly,
    parentTagId,
  });

  const byId = useMemo(() => {
    const map = new Map<string, TagResponse>();
    for (const tag of tags) {
      if (tag.id) map.set(tag.id, tag);
    }
    return map;
  }, [tags]);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return tags
      .filter((tag) => !!tag.id && !value.includes(tag.id))
      .filter((tag) => {
        if (needle === "") return true;
        return (
          (tag.id ?? "").toLowerCase().includes(needle) ||
          (tag.displayName ?? "").toLowerCase().includes(needle)
        );
      });
  }, [tags, value, query]);

  const add = (tagId: string) => {
    if (value.includes(tagId)) return;
    onChange([...value, tagId]);
    setQuery("");
    inputRef.current?.focus();
  };

  const remove = (tagId: string) => {
    onChange(value.filter((id) => id !== tagId));
  };

  return (
    <div className={styles.tagPicker}>
      {label != null && <label className={styles.label}>{label}</label>}
      <div className={styles.field}>
        <div className={styles.chips}>
          {value.map((id) => {
            const displayName = byId.get(id)?.displayName;
            return (
              <Tag
                key={id}
                size='sm'
                onRemove={() => {
                  remove(id);
                }}>
                {displayName ?? id}
              </Tag>
            );
          })}
          <input
            ref={inputRef}
            type='text'
            className={styles.input}
            value={query}
            placeholder={value.length === 0 ? placeholder : ""}
            onChange={(e) => {
              setQuery(e.target.value);
            }}
            onFocus={() => {
              setIsFocused(true);
            }}
            onBlur={() => {
              // Defer so a click on a menu item registers before the menu hides.
              window.setTimeout(() => {
                setIsFocused(false);
              }, 150);
            }}
            onKeyDown={(e) => {
              if (
                e.key === "Backspace" &&
                query === "" &&
                value.length > 0
              ) {
                remove(value[value.length - 1]);
              }
              if (e.key === "Enter" && filtered[0]?.id != null) {
                e.preventDefault();
                add(filtered[0].id);
              }
            }}
          />
        </div>
        {isFocused && (
          <div className={styles.menu} role='listbox'>
            {isLoading && <div className={styles.menuEmpty}>Loading…</div>}
            {!isLoading && filtered.length === 0 && (
              <div className={styles.menuEmpty}>No matching tags</div>
            )}
            {!isLoading &&
              filtered.map((tag) => (
                <button
                  key={tag.id}
                  type='button'
                  role='option'
                  aria-selected={false}
                  className={styles.menuItem}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    if (tag.id != null) add(tag.id);
                  }}>
                  <span className={styles.itemLabel}>{tag.displayName}</span>
                  {tag.curated && (
                    <span className={styles.curatedBadge}>curated</span>
                  )}
                </button>
              ))}
          </div>
        )}
      </div>
    </div>
  );
};

export { TagPicker };
