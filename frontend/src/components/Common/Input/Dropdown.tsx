// Dropdown with optional multi-select and searchable filtering of options
import React, { useEffect, useId, useRef, useState } from "react";
import styles from "./Input.module.css";
import { Btn } from "../Buttons/Btn";

interface DropdownOption {
  value: string;
  label: string;
}

interface DropdownProps {
  options: DropdownOption[];
  value?: string[];
  onChange?: (values: string[]) => void;
  multiple?: boolean;
  searchable?: boolean;
  label?: string;
  labelPosition?: "labelAbove" | "labelInFront";
  placeholder?: string;
  errorMessage?: string;
  infoMessage?: string;
  id?: string;
}

const Dropdown: React.FC<DropdownProps> = ({
  options,
  value = [],
  onChange,
  multiple = false,
  searchable = false,
  label,
  labelPosition = "labelAbove",
  placeholder = "Select...",
  errorMessage,
  infoMessage,
  id,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();

  const filtered = searchable
    ? options.filter((o) => o.label.toLowerCase().includes(query.toLowerCase()))
    : options;

  useEffect(() => {
    if (!isOpen) return;
    const handleClick = (e: MouseEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) {
        setIsOpen(false);
        setQuery("");
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => {
      document.removeEventListener("mousedown", handleClick);
    };
  }, [isOpen]);

  const toggle = (optValue: string) => {
    let next: string[];
    if (multiple) {
      next = value.includes(optValue)
        ? value.filter((v) => v !== optValue)
        : [...value, optValue];
    } else {
      next = [optValue];
      setIsOpen(false);
      setQuery("");
    }
    onChange?.(next);
  };

  const handleTriggerClick = () => {
    if (isOpen) setQuery("");
    setIsOpen((prev) => !prev);
  };

  const removeChip = (e: React.MouseEvent, v: string) => {
    e.stopPropagation();
    onChange?.(value.filter((x) => x !== v));
  };

  const removeChipOnKey = (e: React.KeyboardEvent, v: string) => {
    if (e.key === "Enter") {
      e.stopPropagation();
      onChange?.(value.filter((x) => x !== v));
    }
  };

  const triggerContent =
    value.length === 0 ? (
      <span className={styles.dropdownPlaceholder}>{placeholder}</span>
    ) : multiple ? (
      <span className={styles.chipList}>
        {value.map((v) => {
          const opt = options.find((o) => o.value === v);
          return (
            <span key={v} className={styles.chip}>
              {opt?.label}
              <span
                role='button'
                tabIndex={0}
                aria-label={`Remove ${opt?.label ?? v}`}
                className={styles.chipRemove}
                onClick={(e) => {
                  removeChip(e, v);
                }}
                onKeyDown={(e) => {
                  removeChipOnKey(e, v);
                }}>
                &times;
              </span>
            </span>
          );
        })}
      </span>
    ) : (
      <span>
        {options.find((o) => o.value === value[0])?.label ?? placeholder}
      </span>
    );

  return (
    <div className={[styles.inputContainer, styles[labelPosition]].join(" ")}>
      {label && <label htmlFor={id}>{label}</label>}
      <div className={styles.dropdown} ref={containerRef}>
        <Btn
          type='button'
          id={id}
          className={styles.dropdownTrigger}
          aria-haspopup='listbox'
          aria-expanded={isOpen}
          aria-owns={listboxId}
          onClick={handleTriggerClick}>
          {triggerContent}
          <svg
            className={[styles.chevron, isOpen ? styles.chevronOpen : ""]
              .filter(Boolean)
              .join(" ")}
            xmlns='http://www.w3.org/2000/svg'
            fill='none'
            viewBox='0 0 24 24'
            strokeWidth={2}>
            <path
              strokeLinecap='round'
              strokeLinejoin='round'
              d='m19.5 8.25-7.5 7.5-7.5-7.5'
            />
          </svg>
        </Btn>

        {isOpen && (
          <div className={styles.dropdownPanel}>
            {searchable && (
              <div className={styles.dropdownSearch}>
                <input
                  type='text'
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                  }}
                  placeholder='Search...'
                  aria-label='Search options'
                  autoFocus
                />
              </div>
            )}
            <ul
              id={listboxId}
              role='listbox'
              aria-multiselectable={multiple}
              className={styles.dropdownList}>
              {filtered.length === 0 ? (
                <li className={styles.dropdownEmpty}>No options</li>
              ) : (
                filtered.map((opt) => (
                  <li
                    key={opt.value}
                    role='option'
                    aria-selected={value.includes(opt.value)}
                    className={[
                      styles.dropdownOption,
                      value.includes(opt.value) ? styles.selected : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                    onClick={() => {
                      toggle(opt.value);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        toggle(opt.value);
                      }
                    }}
                    tabIndex={0}>
                    {multiple && (
                      <input
                        type='checkbox'
                        readOnly
                        checked={value.includes(opt.value)}
                        tabIndex={-1}
                        aria-hidden='true'
                      />
                    )}
                    {opt.label}
                  </li>
                ))
              )}
            </ul>
          </div>
        )}

        {(errorMessage != null || infoMessage != null) && (
          <span
            className={[
              styles.inputInfoMessage,
              errorMessage ? styles.errorMessage : "",
            ]
              .filter(Boolean)
              .join(" ")}>
            {errorMessage ?? infoMessage}
          </span>
        )}
      </div>
    </div>
  );
};

export { Dropdown };
export type { DropdownOption };
