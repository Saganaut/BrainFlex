// Resolves user-picture URLs across two storage formats:
//   1. External / presigned URLs (Google OAuth, S3 variant URLs) — returned as-is.
//   2. Built-in avatar identifiers stored as `builtin:<value>` — resolved to
//      the Vite-bundled asset URL by looking the value up in AVATAR_OPTIONS.
//
// Built-in avatars are persisted as stable identifiers (not Vite-hashed URLs)
// so a frontend rebuild doesn't orphan every saved avatar. Every place that
// renders a user picture should pass through this helper.
import {
  AVATAR_OPTIONS,
  type AvatarOption,
} from "@/components/Common/Input/AvatarSelector/AvatarSelector";

const BUILTIN_PREFIX = "builtin:";

const optionByValue = new Map<string, AvatarOption>(
  AVATAR_OPTIONS.map((o) => [o.value, o]),
);

export const isBuiltinAvatar = (
  src: string | null | undefined,
): src is string => typeof src === "string" && src.startsWith(BUILTIN_PREFIX);

export const builtinAvatarValue = (
  src: string | null | undefined,
): string | null => {
  if (!isBuiltinAvatar(src)) return null;
  return src.slice(BUILTIN_PREFIX.length);
};

export const builtinAvatarUrl = (value: string): string =>
  `${BUILTIN_PREFIX}${value}`;

export const resolveAvatarSrc = <T extends string | null | undefined>(
  src: T,
): T => {
  if (!isBuiltinAvatar(src)) return src;
  const value = src.slice(BUILTIN_PREFIX.length);
  const option = optionByValue.get(value);
  return (option ? option.src : src) as T;
};
