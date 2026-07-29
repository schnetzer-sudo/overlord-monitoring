import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

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
 * Wurzel-Layout. Liest die Sprache **einmal** aus dem Cookie: für `lang` am
 * Dokument und für den Sprachkontext darunter. Kein Sprachpräfix in der URL —
 * die Sprache ist eine Eigenschaft des Nutzers, nicht der Ansicht.
 */
export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const sprache = await aktiveSprache();

  return (
    <html lang={sprache} className={cn("font-sans", geist.variable, geistMono.variable)}>
      <body>
        <Providers sprache={sprache} texte={texteFuer(sprache)}>
          {children}
        </Providers>
      </body>
    </html>
  );
}
