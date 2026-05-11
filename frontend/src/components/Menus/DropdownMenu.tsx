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

interface DropdownMenuProps {
  trigger: (toggle: () => void) => ReactElement;
  children: ReactNode;
  align?: "left" | "right";
  className?: string;
}

const DropdownMenu = ({
  trigger,
  children,
  align = "right",
  className,
}: DropdownMenuProps) => {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const toggle = () => {
    setOpen((prev) => !prev);
  };
  const closeMenu = () => {
    setOpen(false);
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        wrapperRef.current &&
        !wrapperRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <div
      ref={wrapperRef}
      className={[styles.wrapper, className].filter(Boolean).join(" ")}>
      {trigger(toggle)}
      {open && (
        <DropdownMenuContext value={closeMenu}>
          <div
            className={[
              styles.panel,
              align === "left" ? styles.alignLeft : styles.alignRight,
            ].join(" ")}>
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
