import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import prettier from "eslint-config-prettier/flat";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  // Muss nach den Next-Konfigurationen stehen: schaltet alle ESLint-Regeln ab,
  // die sich mit Prettier ueberschneiden. Formatierung macht Prettier, nicht ESLint.
  prettier,
  globalIgnores([
    // Voreinstellungen von eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
