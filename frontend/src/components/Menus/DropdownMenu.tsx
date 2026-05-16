// Dropdown panel anchored to a trigger element; owns open state and click-outside detection
import React, {
  createContext,
  use,
  useEffect,
  useRef,
  useState,
  type ReactElement,
  type ReactNode,
} from "react";
import styles from "./DropdownMenu.module.css";

const DropdownMenuContext = createContext<() => void>(() => undefined);

type DropdownPosition =
  | "bottom-right"
  | "bottom-left"
  | "top-right"
  | "top-left";

// Anything with clientX/clientY — typically a MouseEvent / React.MouseEvent.
// Only used when `anchorToCursor` is set; otherwise toggle ignores its argument.
interface CursorAnchor {
  clientX: number;
  clientY: number;
}
type ToggleFn = (anchor?: CursorAnchor) => void;

interface DropdownMenuProps {
  trigger: (toggle: ToggleFn) => ReactElement;
  children: ReactNode;
  position?: DropdownPosition;
  className?: string;
  // Opt-in: pin the panel at the cursor passed to toggle (context-menu style)
  // instead of anchoring to the trigger. `position` still names which corner
  // of the panel sits at the cursor.
  anchorToCursor?: boolean;
}

const positionClassMap: Record<DropdownPosition, string> = {
  "bottom-right": styles.bottomRight,
  "bottom-left": styles.bottomLeft,
  "top-right": styles.topRight,
  "top-left": styles.topLeft,
};

const DropdownMenu = ({
  trigger,
  children,
  position = "top-right",
  className,
  anchorToCursor = false,
}: DropdownMenuProps) => {
  const [open, setOpen] = useState(false);
  const [cursor, setCursor] = useState<{ x: number; y: number } | null>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const toggle: ToggleFn = (anchor) => {
    if (anchorToCursor && anchor) {
      setCursor({ x: anchor.clientX, y: anchor.clientY });
      setOpen(true);
      return;
    }
    setCursor(null);
    setOpen((prev) => !prev);
  };
  const closeMenu = () => {
    setOpen(false);
    setCursor(null);
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        wrapperRef.current &&
        !wrapperRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
        setCursor(null);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // In cursor mode the panel is fixed-positioned at the click point; the
  // `position` prop names which corner of the panel sits at the cursor.
  const cursorStyle: React.CSSProperties | undefined = cursor
    ? {
        position: "fixed",
        top: position.startsWith("top") ? cursor.y : undefined,
        bottom: position.startsWith("bottom")
          ? window.innerHeight - cursor.y
          : undefined,
        left: position.endsWith("left") ? cursor.x : undefined,
        right: position.endsWith("right")
          ? window.innerWidth - cursor.x
          : undefined,
      }
    : undefined;

  return (
    <div
      ref={wrapperRef}
      className={[styles.wrapper, className].filter(Boolean).join(" ")}>
      {trigger(toggle)}
      {open && (
        <DropdownMenuContext value={closeMenu}>
          <div
            className={
              cursor
                ? styles.panel
                : [styles.panel, positionClassMap[position]].join(" ")
            }
            style={cursorStyle}>
            {children}
          </div>
        </DropdownMenuContext>
      )}
    </div>
  );
};

interface DropdownMenuItemProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  centered?: boolean;
}

const DropdownMenuItem = ({
  children,
  className,
  centered = false,
  onClick,
  ...rest
}: DropdownMenuItemProps) => {
  const closeMenu = use(DropdownMenuContext);
  return (
    <button
      type='button'
      className={[styles.item, centered && styles.center, className]
        .filter(Boolean)
        .join(" ")}
      onClick={(e) => {
        closeMenu();
        onClick?.(e);
      }}
      {...rest}>
      {children}
    </button>
  );
};

// Wraps a router Link (or any anchor-like child) with item styling and auto-close on click
interface DropdownMenuLinkProps {
  children: ReactNode;
  className?: string;
}

const DropdownMenuLink = ({ children, className }: DropdownMenuLinkProps) => {
  const closeMenu = use(DropdownMenuContext);
  return (
    <div
      role='none'
      className={[styles.item, className].filter(Boolean).join(" ")}
      onClick={closeMenu}>
      {children}
    </div>
  );
};

interface DropdownMenuLabelProps {
  children: ReactNode;
  className?: string;
}

const DropdownMenuLabel = ({ children, className }: DropdownMenuLabelProps) => (
  <span className={[styles.label, className].filter(Boolean).join(" ")}>
    {children}
  </span>
);

const DropdownMenuDivider = () => <div className={styles.divider} />;

export {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLink,
  DropdownMenuLabel,
  DropdownMenuDivider,
};
