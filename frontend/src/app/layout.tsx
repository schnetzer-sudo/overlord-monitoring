import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { aktiveDichte } from "@/dichte/server";
import { texteFuer } from "@/i18n";
import { aktiveSprache, aktiveTexte } from "@/i18n/server";
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
 * Wurzel-Layout. Liest **zwei Eigenschaften des Nutzers** aus Cookies, beide
 * einmal und beide serverseitig:
 *
 * | | am Dokument | im Kontext darunter |
 * |---|---|---|
 * | Sprache | `lang` | Sprachdatei für `useTexte` |
 * | Dichte | `data-dichte` | Stufe für den Umschalter |
 *
 * Kein Sprachpräfix in der URL — die Sprache ist eine Eigenschaft des Nutzers,
 * nicht der Ansicht.
 *
 * **`data-dichte` steht am `<html>` und nirgends sonst.** `globals.css` hängt
 * daran `--dichte-wurzel` und damit die Wurzelschriftgröße; `rem` misst gegen
 * das Wurzelelement, ein Wrapper darunter trüge nichts. Und weil es hier
 * serverseitig gesetzt wird, steht die Stufe **vor dem ersten Paint** fest — mit
 * `localStorage` flackerte die Anwendung bei jedem Aufruf einmal in der falschen
 * Größe.
 */
export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const sprache = await aktiveSprache();
  const dichte = await aktiveDichte();

  return (
    <html
      lang={sprache}
      data-dichte={dichte}
      className={cn("font-sans", geist.variable, geistMono.variable)}
    >
      <body>
        <Providers sprache={sprache} texte={texteFuer(sprache)} dichte={dichte}>
          {children}
        </Providers>
      </body>
    </html>
  );
}
