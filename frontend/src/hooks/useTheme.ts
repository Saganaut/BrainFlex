// Hook managing both light/dark mode and the two brand hue variables.
// Persists all three values to localStorage so they survive page refreshes.
import { useEffect, useState } from "react";

type ThemeMode = "light" | "dark";

const STORAGE_KEY = "brainflex-theme";
const HUE_PRIMARY_KEY = "brainflex-hue-primary";
const HUE_ACCENT_KEY = "brainflex-hue-accent";

export const DEFAULT_HUE_PRIMARY = 260;
export const DEFAULT_HUE_ACCENT = 25;

const getSystemTheme = (): ThemeMode => {
  if (typeof window === "undefined") return "light";
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
};

const getStoredTheme = (): ThemeMode | null => {
  if (typeof window === "undefined") return null;
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === "dark" || stored === "light" ? stored : null;
};

const getStoredHue = (key: string, fallback: number): number => {
  if (typeof window === "undefined") return fallback;
  const stored = window.localStorage.getItem(key);
  if (stored === null) return fallback;
  const n = Number(stored);
  return Number.isFinite(n) && n >= 0 && n <= 360 ? n : fallback;
};

const applyThemeClass = (theme: ThemeMode) => {
  const html = document.documentElement;
  if (theme === "dark") {
    html.classList.add("theme-dark");
    html.classList.remove("theme-light");
  } else {
    html.classList.add("theme-light");
    html.classList.remove("theme-dark");
  }
};

const clampHue = (hue: number) => Math.round(Math.max(0, Math.min(360, hue)));

export function useTheme() {
  const [theme, setTheme] = useState<ThemeMode>(() => {
    const stored = getStoredTheme();
    return stored ?? getSystemTheme();
  });

  const [huePrimary, setHuePrimary] = useState<number>(() =>
    getStoredHue(HUE_PRIMARY_KEY, DEFAULT_HUE_PRIMARY),
  );

  const [hueAccent, setHueAccent] = useState<number>(() =>
    getStoredHue(HUE_ACCENT_KEY, DEFAULT_HUE_ACCENT),
  );

  useEffect(() => {
    applyThemeClass(theme);
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  useEffect(() => {
    document.documentElement.style.setProperty(
      "--hue-primary",
      `${huePrimary}deg`,
    );
    window.localStorage.setItem(HUE_PRIMARY_KEY, String(huePrimary));
  }, [huePrimary]);

  useEffect(() => {
    document.documentElement.style.setProperty(
      "--hue-accent",
      `${hueAccent}deg`,
    );
    window.localStorage.setItem(HUE_ACCENT_KEY, String(hueAccent));
  }, [hueAccent]);

  const toggleTheme = () => {
    setTheme((current) => (current === "dark" ? "light" : "dark"));
  };

  const resetHues = () => {
    setHuePrimary(DEFAULT_HUE_PRIMARY);
    setHueAccent(DEFAULT_HUE_ACCENT);
  };

  return {
    theme,
    toggleTheme,
    setTheme,
    huePrimary,
    hueAccent,
    setHuePrimary: (hue: number) => {
      setHuePrimary(clampHue(hue));
    },
    setHueAccent: (hue: number) => {
      setHueAccent(clampHue(hue));
    },
    resetHues,
  };
}
