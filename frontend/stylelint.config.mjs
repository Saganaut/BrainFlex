/** @type {import("stylelint").Config} */
export default {
  extends: ["stylelint-config-standard"],
  plugins: ["stylelint-value-no-unknown-custom-properties"],
  rules: {
    "csstools/value-no-unknown-custom-properties": [
      true,
      {
        importFrom: ["./src/index.css"],
      },
    ],
  },
};
