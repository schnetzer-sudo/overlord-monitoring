import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * Bewusst klein: kein jsdom, keine Testing Library, kein React-Plugin.
 *
 * Geprüft werden die Entscheidungen, nicht das Markup — der Ablauf nach dem
 * Anmelden, die Gleichheit beider Sprachdateien, die Wortwahl bei 404, das
 * Leeren des Zwischenspeichers und die Zeitstempel ohne Zeitzonenverschiebung.
 * Das sind alles reine Funktionen. Ein gerenderter Baum brächte hier nichts
 * außer Laufzeit und Abhängigkeiten.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts"],
  },
});
