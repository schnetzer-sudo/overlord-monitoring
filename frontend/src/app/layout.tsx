import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { aktiveDichte } from "@/dichte/server";
import { texteFuer } from "@/i18n";
import { aktiveSprache, aktiveTexte } from "@/i18n/server";
import { aktivesThema } from "@/thema/server";
import { cn } from "@/lib/utils";

import { Providers } from "./providers";
import "./globals.css";

/** Neutrale Grotesk für den Fließtext … */
const geist = Geist({ subsets: ["latin"], variable: "--font-sans" });
/** … und dieselbe Familie mit fester Laufweite für Codes, IDs und Kennungen. */
const geistMono = Geist_Mono({ subsets: ["latin"], variable: "--font-mono" });

export async function generateMetadata(): Promise<Metadata> {
  const texte = await aktiveTexte();
  return {
    title: { default: texte.anwendung.name, template: `%s · ${texte.anwendung.name}` },
    description: texte.anwendung.beschreibung,
  };
}

/**
 * Wurzel-Layout. Liest **drei Eigenschaften des Nutzers** aus Cookies, jede
 * einmal und jede serverseitig:
 *
 * | | am Dokument | im Kontext darunter |
 * |---|---|---|
 * | Sprache | `lang` | Sprachdatei für `useTexte` |
 * | Dichte | `data-dichte` | Stufe für den Umschalter |
 * | Thema | `data-thema` | Wahl für den Umschalter |
 *
 * Kein Sprachpräfix in der URL — die Sprache ist eine Eigenschaft des Nutzers,
 * nicht der Ansicht.
 *
 * **`data-dichte` und `data-thema` stehen am `<html>` und nirgends sonst.**
 * Für die Dichte ist das zwingend: `globals.css` hängt daran `--dichte-wurzel`
 * und damit die Wurzelschriftgröße, und `rem` misst gegen das Wurzelelement —
 * ein Wrapper darunter trüge nichts. Für das Thema ist es dieselbe Bauform am
 * selben Element statt einer zweiten (E‑59): eine Klasse `dark` daneben wäre
 * eine Menge, in der ein dritter Wert *System* keine Stelle hat.
 *
 * Und weil beide hier **serverseitig** gesetzt werden, stehen sie **vor dem
 * ersten Paint** fest — mit `localStorage` flackerte die Anwendung bei jedem
 * Aufruf einmal in der falschen Größe bzw. einmal hell. Nachgewiesen an der
 * Byteposition im ausgelieferten Markup (`docs/dichte-umschalter.md` §5.5 für
 * die Dichte, `docs/dunkelmodus.md` M136 für das Thema).
 */
export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const sprache = await aktiveSprache();
  const dichte = await aktiveDichte();
  const thema = await aktivesThema();

  return (
    <html
      lang={sprache}
      data-dichte={dichte}
      data-thema={thema}
      className={cn("font-sans", geist.variable, geistMono.variable)}
    >
      <body>
        <Providers sprache={sprache} texte={texteFuer(sprache)} dichte={dichte} thema={thema}>
          {children}
        </Providers>
      </body>
    </html>
  );
}
