import stylistic from "@stylistic/eslint-plugin";
import { defineConfig } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
    {
        files: ["**/*.ts"],
        languageOptions: {
            parser: tseslint.parser,
        },
    },
    {
        files: ["**/*.ts"],
        plugins: {
            "@stylistic": stylistic,
        },
        rules: {
            "@stylistic/brace-style": ["error", "stroustrup"],
            "@stylistic/indent": ["error", 4],
            "@stylistic/semi": ["error", "always"],
            "@stylistic/quotes": ["error", "double", { avoidEscape: true }],
            "@stylistic/comma-spacing": ["error", { before: false, after: true }],
            "@stylistic/space-before-blocks": ["error", "always"],
            "@stylistic/space-before-function-paren": ["error", { anonymous: "never", named: "never", asyncArrow: "never", catch: "always" }],
            "@stylistic/keyword-spacing": ["error", { before: true, after: true }],
            "@stylistic/space-infix-ops": "error",
            "@stylistic/arrow-spacing": ["error", { before: true, after: true }],
            "@stylistic/max-len": ["error", { code: 160, ignoreComments: true }],
            "@stylistic/no-trailing-spaces": "error",
            "@stylistic/eol-last": ["error", "always"],
        },
    },
);
