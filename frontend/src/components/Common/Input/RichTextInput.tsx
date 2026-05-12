/**
 * Rich text input — TipTap-backed editable surface that looks like a text
 * input by default. When the user focuses inside, a floating toolbar appears
 * with bold / strike / underline / link / color / font-size controls; it goes
 * away when focus leaves both the editor and the toolbar.
 *
 * Toolbar buttons use `onMouseDown` + `preventDefault` so clicking them does
 * not steal focus from the ProseMirror editor — that's the standard TipTap
 * pattern for floating toolbars.
 *
 * The `value` / `onChange` API mirrors the rest of the form primitives: HTML
 * string in, HTML string out on every edit. Consumers can persist that string
 * directly to the backend (e.g. Slide.body).
 */
import { useEditor, EditorContent, type Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { TextStyle, Color, FontSize } from "@tiptap/extension-text-style";
import { Placeholder } from "@tiptap/extensions";
import { useEffect, useRef, useState } from "react";
import styles from "./RichTextInput.module.css";

interface RichTextInputProps {
  value: string;
  onChange: (html: string) => void;
  /** Fired when focus leaves the editor (and the toolbar). Useful for
   *  flushing a debounced commit. */
  onBlur?: () => void;
  placeholder?: string;
  label?: string;
  id?: string;
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
}

/** Button that runs a chain command without stealing focus from the editor. */
interface ToolbarBtnProps {
  onClick: () => void;
  isActive?: boolean;
  ariaLabel: string;
  children: React.ReactNode;
}
const ToolbarBtn = ({
  onClick,
  isActive,
  ariaLabel,
  children,
}: ToolbarBtnProps) => (
  <button
    type='button'
    aria-label={ariaLabel}
    aria-pressed={isActive}
    className={`${styles.toolbarBtn} ${isActive ? styles.toolbarBtnActive : ""}`}
    // preventDefault on mousedown keeps focus inside the ProseMirror editor —
    // without this, clicking the toolbar collapses the selection.
    onMouseDown={(e) => {
      e.preventDefault();
    }}
    onClick={onClick}>
    {children}
  </button>
);

const Toolbar = ({ editor }: ToolbarProps) => {
  // Inline link editor — opening it stashes the current selection (TipTap
  // remembers it as long as the editor isn't destroyed), pre-fills with the
  // existing href if any, and lets the user apply or remove the link without
  // a modal/prompt.
  const [linkOpen, setLinkOpen] = useState(false);
  const [linkUrl, setLinkUrl] = useState("");
  const linkInputRef = useRef<HTMLInputElement>(null);

  const openLinkEditor = () => {
    const existing = editor.getAttributes("link").href as string | undefined;
    setLinkUrl(existing ?? "");
    setLinkOpen(true);
  };

  // Focus the link input once it mounts so the user can start typing.
  useEffect(() => {
    if (linkOpen) linkInputRef.current?.focus();
  }, [linkOpen]);

  const applyLink = () => {
    const trimmed = linkUrl.trim();
    if (trimmed === "") {
      editor.chain().focus().extendMarkRange("link").unsetLink().run();
    } else {
      editor
        .chain()
        .focus()
        .extendMarkRange("link")
        .setLink({ href: trimmed })
        .run();
    }
    setLinkOpen(false);
  };

  const removeLink = () => {
    editor.chain().focus().extendMarkRange("link").unsetLink().run();
    setLinkOpen(false);
  };

  return (
    <div className={styles.toolbar} role='toolbar' aria-label='Text formatting'>
      <div className={styles.toolbarRow}>
        <ToolbarBtn
          ariaLabel='Bold'
          isActive={editor.isActive("bold")}
          onClick={() => editor.chain().focus().toggleBold().run()}>
          <strong>B</strong>
        </ToolbarBtn>
        <ToolbarBtn
          ariaLabel='Underline'
          isActive={editor.isActive("underline")}
          onClick={() => editor.chain().focus().toggleUnderline().run()}>
          <u>U</u>
        </ToolbarBtn>
        <ToolbarBtn
          ariaLabel='Strikethrough'
          isActive={editor.isActive("strike")}
          onClick={() => editor.chain().focus().toggleStrike().run()}>
          <s>S</s>
        </ToolbarBtn>
        <ToolbarBtn
          ariaLabel={linkOpen ? "Close link editor" : "Insert link"}
          isActive={editor.isActive("link") || linkOpen}
          onClick={() => {
            if (linkOpen) {
              setLinkOpen(false);
              return;
            }
            openLinkEditor();
          }}>
          <span aria-hidden='true'>🔗</span>
        </ToolbarBtn>

        <div className={styles.toolbarDivider} aria-hidden='true' />

        <span className={styles.toolbarGroupLabel}>Color</span>
        {COLOR_CHOICES.map((color) => (
          <button
            key={color.label}
            type='button'
            title={color.label}
            aria-label={`Set text color ${color.label}`}
            className={styles.colorSwatch}
            style={{ background: color.value || "transparent" }}
            onMouseDown={(e) => {
              e.preventDefault();
            }}
            onClick={() => {
              if (color.value === "") {
                editor.chain().focus().unsetColor().run();
              } else {
                editor.chain().focus().setColor(color.value).run();
              }
            }}
          />
        ))}

        <div className={styles.toolbarDivider} aria-hidden='true' />

        <span className={styles.toolbarGroupLabel}>Size</span>
        {SIZE_CHOICES.map((size) => (
          <ToolbarBtn
            key={size.label}
            ariaLabel={`Set font size ${size.label}`}
            isActive={editor.isActive("textStyle", { fontSize: size.value })}
            onClick={() =>
              editor.chain().focus().setFontSize(size.value).run()
            }>
            {size.label}
          </ToolbarBtn>
        ))}
      </div>

      {linkOpen && (
        <div className={styles.linkEditor}>
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
        </div>
      )}
    </div>
  );
};

const RichTextInput = ({
  value,
  onChange,
  onBlur,
  placeholder,
  label,
  id,
}: RichTextInputProps) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
      TextStyle,
      Color,
      FontSize,
      Placeholder.configure({ placeholder: placeholder ?? "" }),
    ],
    content: value,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
    editorProps: {
      attributes: {
        // The contenteditable element gets the input-surface class so it
        // visually matches the rest of the form primitives.
        class: styles.editor,
        ...(id ? { id } : {}),
      },
    },
  });

  // Keep editor content in sync if the parent's `value` changes from outside
  // (e.g. switching to a different slide while the editor is mounted).
  useEffect(() => {
    const current = editor.getHTML();
    if (value !== current) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
  }, [editor, value]);

  const [hasFocus, setHasFocus] = useState(false);

  const handleFocusCapture = () => {
    setHasFocus(true);
  };
  const handleBlurCapture = (e: React.FocusEvent<HTMLDivElement>) => {
    // Only collapse the toolbar when focus has left the whole wrapper.
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setHasFocus(false);
      onBlur?.();
    }
  };

  return (
    <div className={styles.wrapper}>
      {label && (
        <label className={styles.label} htmlFor={id}>
          {label}
        </label>
      )}
      <div
        className={styles.surface}
        onFocusCapture={handleFocusCapture}
        onBlurCapture={handleBlurCapture}>
        {hasFocus && <Toolbar editor={editor} />}
        <EditorContent editor={editor} />
      </div>
    </div>
  );
};

export { RichTextInput };
