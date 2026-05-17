// Behavior hooks for HuePicker. Split so the JSX stays a thin presentational
// layer: useHuePicker owns the popover open/close + click-outside dismiss,
// and useColorAreaDrag owns the click-and-drag pointer math inside the
// 2D color area.
import {
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type MouseEvent as ReactMouseEvent,
} from "react";

const useHuePicker = (onChange: (hue: number) => void) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (e: PointerEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) setIsOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
    };
  }, [isOpen]);

  const handleNumberInput = (e: ChangeEvent<HTMLInputElement>) => {
    const n = Number(e.target.value);
    if (Number.isFinite(n)) onChange(Math.max(0, Math.min(360, n)));
  };

  const toggleOpen = () => {
    setIsOpen((o) => !o);
  };

  return {
    isOpen,
    containerRef,
    toggleOpen,
    handleNumberInput,
  };
};

const useColorAreaDrag = (
  currentValue: number,
  onChange: (hue: number) => void,
) => {
  const areaRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  const getHueFromEvent = (e: ReactMouseEvent) => {
    if (!areaRef.current) return currentValue;
    const rect = areaRef.current.getBoundingClientRect();
    return Math.round(
      Math.max(0, Math.min(360, ((e.clientX - rect.left) / rect.width) * 360)),
    );
  };

  const onMouseDown = (e: ReactMouseEvent) => {
    isDragging.current = true;
    onChange(getHueFromEvent(e));
  };

  const onMouseMove = (e: ReactMouseEvent) => {
    if (isDragging.current) onChange(getHueFromEvent(e));
  };

  const stopDrag = () => {
    isDragging.current = false;
  };

  return { areaRef, onMouseDown, onMouseMove, stopDrag };
};

export { useHuePicker, useColorAreaDrag };
