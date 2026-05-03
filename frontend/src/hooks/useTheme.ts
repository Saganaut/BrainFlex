import { useEffect, useState } from "react";

type ThemeMode = "light" | "dark";
const STORAGE_KEY = "brainflex-theme";

const getSystemTheme = (): ThemeMode => {
  if (typeof window === "undefined") {
    return "light";
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
};

const getStoredTheme = (): ThemeMode | null => {
  if (typeof window === "undefined") {
    return null;
  }
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === "dark" || stored === "light" ? stored : null;
};

const applyTheme = (theme: ThemeMode) => {
  const html = document.documentElement;

  if (theme === "dark") {
    html.classList.add("theme-dark");
    html.classList.remove("theme-light");
  } else {
    html.classList.add("theme-light");
    html.classList.remove("theme-dark");
  }
};

export function useTheme() {
  const [theme, setTheme] = useState<ThemeMode>(() => {
    const stored = getStoredTheme();
    return stored ?? getSystemTheme();
  });

  useEffect(() => {
    applyTheme(theme);
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () =>
    setTheme((current) => (current === "dark" ? "light" : "dark"));

  return { theme, toggleTheme, setTheme };
}
