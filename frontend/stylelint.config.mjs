/** @type {import("stylelint").Config} */
export default {
  extends: ["stylelint-config-standard"],
  plugins: ["stylelint-value-no-unknown-custom-properties"],
  rules: {
    "csstools/value-no-unknown-custom-properties": [
      true,
      {
        importFrom: ["./src/tokens.css"],
      },
    ],
    // using lowerCamelCase for compatibility with css modules
    "selector-class-pattern": [
      "^[a-z][a-zA-Z0-9]+$",
      {
        message: "Expected class selector to be lowerCamelCase",
      },
    ],
  },
};
