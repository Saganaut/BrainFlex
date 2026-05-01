export function camelToNormalCase(str: string): string {
  if (!str) return str;

  let result = str.charAt(0).toUpperCase();

  for (let i = 1; i < str.length; i++) {
    const char = str.charAt(i);
    if (char === char.toUpperCase() && char !== char.toLowerCase()) {
      // It's an uppercase letter
      result += " " + char.toLowerCase();
    } else {
      result += char;
    }
  }

  return result;
}
