/**
 * Rich text input — TipTap-backed editable surface that looks like a text
 * input by default. Clicking inside opens a floating toolbar (bold / strike /
 * underline / link / color / font-size). The toolbar closes on outside
 * pointerdown or Escape — same pattern the rest of the app's popovers use
 * (see McqOptionEditable).
 *
 * Positioning uses CSS Anchor Positioning (`anchor-name` on the surface,
 * `position-anchor` + `anchor()` on the toolbar) so the toolbar sits above
 * the input regardless of where the input lives in the page flow. No portal,
 * no floating-ui — the toolbar is a DOM sibling of the editor inside the
 * wrapper.
 *
 * Toolbar buttons use `preventFocusSteal` so clicking them doesn't blur the
 * ProseMirror editor; the link <input> is a real focusable element, so the
 * blur handler in useRichTextEditor ignores focus moves that land inside the
 * wrapper.
 *
 * The `value` / `onChange` API mirrors the rest of the form primitives: HTML
 * string in, HTML string out on every edit. Consumers can persist that string
 * directly to the backend (e.g. Slide.body).
 */
import { EditorContent, type Editor } from "@tiptap/react";
import { useEffect, useImperativeHandle, useRef, useState } from "react";
import styles from "./RichTextInput.module.css";
import {
  Popover,
  PopoverRow,
  PopoverButton,
  PopoverDivider,
} from "../Popover/Popover";
import { ColorOptionBtn } from "../../Buttons/ColorOptionBtn";
import { useRichTextEditor, useLinkEditor } from "./useRichTextInput";

interface RichTextInputHandle {
  /** Blur the underlying editor and close the toolbar. Used by parents that
   *  need to dismiss the floating toolbar without waiting for the user to
   *  click outside (e.g. a collapsing drawer). */
  blur: () => void;
}

interface RichTextInputProps {
  value: string;
  onChange: (html: string) => void;
  /** Fired when focus leaves the editor (and the toolbar). Useful for
   *  flushing a debounced commit. */
  onBlur?: () => void;
  placeholder?: string;
  label?: string;
  id?: string;
  ref?: React.Ref<RichTextInputHandle>;
  isBordered?: boolean;
}

// A handful of presets — "a few choices" per the spec.
const COLOR_CHOICES: { label: string; value: string }[] = [
  { label: "Default", value: "" },
  { label: "Red", value: "#e53e3e" },
  { label: "Orange", value: "#dd6b20" },
  { label: "Green", value: "#38a169" },
  { label: "Blue", value: "#3182ce" },
  { label: "Purple", value: "#805ad5" },
];

const SIZE_CHOICES: { label: string; value: string }[] = [
  { label: "S", value: "0.875rem" },
  { label: "M", value: "1rem" },
  { label: "L", value: "1.25rem" },
  { label: "XL", value: "1.75rem" },
];

interface ToolbarProps {
  editor: Editor;
  linkOpen: boolean;
  setLinkOpen: (v: boolean) => void;
}

const Toolbar = ({ editor, linkOpen, setLinkOpen }: ToolbarProps) => {
  const {
    linkUrl,
    setLinkUrl,
    linkInputRef,
    applyLink,
    removeLink,
    toggleLinkEditor,
  } = useLinkEditor({ editor, linkOpen, setLinkOpen });

  return (
    <>
      <Popover role='toolbar' ariaLabel='Text formatting'>
        <PopoverRow>
          <PopoverButton
            ariaLabel='Bold'
            isActive={editor.isActive("bold")}
            preventFocusSteal
            onClick={() => editor.chain().focus().toggleBold().run()}>
            <strong>B</strong>
          </PopoverButton>
          <PopoverButton
            ariaLabel='Underline'
            isActive={editor.isActive("underline")}
            preventFocusSteal
            onClick={() => editor.chain().focus().toggleUnderline().run()}>
            <u>U</u>
          </PopoverButton>
          <PopoverButton
            ariaLabel='Strikethrough'
            isActive={editor.isActive("strike")}
            preventFocusSteal
            onClick={() => editor.chain().focus().toggleStrike().run()}>
            <s>S</s>
          </PopoverButton>
          <PopoverButton
            ariaLabel={linkOpen ? "Close link editor" : "Insert link"}
            isActive={editor.isActive("link") || linkOpen}
            preventFocusSteal
            onClick={toggleLinkEditor}>
            <span aria-hidden='true'>🔗</span>
          </PopoverButton>

          <PopoverDivider />

          {COLOR_CHOICES.map((color) => (
            <ColorOptionBtn
              key={color.label}
              label={color.label}
              color={color.value}
              preventFocusSteal
              onClick={() => {
                if (color.value === "") {
                  editor.chain().focus().unsetColor().run();
                } else {
                  editor.chain().focus().setColor(color.value).run();
                }
              }}
            />
          ))}

          <PopoverDivider />

          {SIZE_CHOICES.map((size) => (
            <PopoverButton
              key={size.label}
              ariaLabel={`Set font size ${size.label}`}
              isActive={editor.isActive("textStyle", { fontSize: size.value })}
              preventFocusSteal
              onClick={() =>
                editor.chain().focus().setFontSize(size.value).run()
              }>
              {size.label}
            </PopoverButton>
          ))}
        </PopoverRow>
      </Popover>

      {linkOpen && (
        <Popover
          className={styles.linkPopover}
          role='dialog'
          ariaLabel='Edit link'>
          <PopoverRow>
            <input
              ref={linkInputRef}
              type='url'
              className={styles.linkInput}
              placeholder='https://example.com'
              value={linkUrl}
              onChange={(e) => {
                setLinkUrl(e.target.value);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  applyLink();
                } else if (e.key === "Escape") {
                  e.preventDefault();
                  setLinkOpen(false);
                }
              }}
            />
            <button
              type='button'
              className={styles.linkApply}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={applyLink}>
              Apply
            </button>
            {editor.isActive("link") && (
              <button
                type='button'
                className={styles.linkRemove}
                onMouseDown={(e) => {
                  e.preventDefault();
                }}
                onClick={removeLink}>
                Remove
              </button>
            )}
          </PopoverRow>
        </Popover>
      )}
    </>
  );
};

const RichTextInput = ({
  value,
  onChange,
  onBlur,
  placeholder,
  label,
  id,
  ref,
  isBordered = true,
}: RichTextInputProps) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [toolbarOpen, setToolbarOpen] = useState(false);
  const [linkOpen, setLinkOpen] = useState(false);

  const editor = useRichTextEditor({
    value,
    onChange,
    onBlur,
    placeholder,
    id,
    editorClassName: styles.editor,
    wrapperRef,
  });

  // Open the toolbar whenever the editor takes focus. Subsequent focus events
  // are a no-op against the already-true state.
  useEffect(() => {
    const handleFocus = () => {
      setToolbarOpen(true);
    };
    editor.on("focus", handleFocus);
    return () => {
      editor.off("focus", handleFocus);
    };
  }, [editor]);

  // Dismiss the toolbar on outside pointerdown / Escape — same shape as
  // McqOptionEditable. Listeners only attach while open.
  useEffect(() => {
    if (!toolbarOpen) return;
    const handlePointerDown = (e: PointerEvent) => {
      if (!wrapperRef.current?.contains(e.target as Node)) {
        setToolbarOpen(false);
        setLinkOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setToolbarOpen(false);
        setLinkOpen(false);
      }
    };
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [toolbarOpen]);

  useImperativeHandle(ref, () => ({
    blur: () => {
      editor.commands.blur();
      setLinkOpen(false);
      setToolbarOpen(false);
    },
  }));

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      {label && (
        <label className={styles.label} htmlFor={id}>
          {label}
        </label>
      )}
      <div
        className={`${styles.surface} ${isBordered ? " " : styles.noBorders}`}>
        {toolbarOpen && (
          <div className={styles.floatingToolbar}>
            <Toolbar
              editor={editor}
              linkOpen={linkOpen}
              setLinkOpen={setLinkOpen}
            />
          </div>
        )}
        <EditorContent editor={editor} />
      </div>
    </div>
  );
};

export { RichTextInput };
export type { RichTextInputHandle };
