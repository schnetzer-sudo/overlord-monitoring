"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import type { ReactNode } from "react";

import { DichteProvider } from "@/dichte/provider";
import type { Dichtestufe } from "@/dichte";
import { SpracheProvider } from "@/i18n/provider";
import type { Sprache, Texte } from "@/i18n";
import { ThemaProvider } from "@/thema/provider";
import type { Themawert } from "@/thema";
import { getQueryClient } from "@/lib/query-client";

/**
 * Die einzige Client-Komponente im Wurzel-Layout.
 *
 * TanStack Query hält den Serverzustand, nuqs den Filterzustand in der URL —
 * damit Ansichten teilbar sind. Sprache, **Dichte und Thema** kommen als Props
 * vom Server: die Sprache, damit nur die aktive Sprachdatei im
 * Auslieferungszustand landet und nicht beide; Dichte und Thema, damit die
 * beiden Umschalter im Nutzermenü die aktive Wahl markieren können, ohne sie
 * zur Laufzeit aus dem Dokument zu lesen.
 *
 * Alles darunter bleibt standardmäßig Server-Komponente; "use client" steht so
 * weit unten im Baum wie möglich.
 */
export function Providers({
  sprache,
  texte,
  dichte,
  thema,
  children,
}: {
  sprache: Sprache;
  texte: Texte;
  dichte: Dichtestufe;
  thema: Themawert;
  children: ReactNode;
}) {
  const queryClient = getQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <SpracheProvider sprache={sprache} texte={texte}>
        <DichteProvider dichte={dichte}>
          <ThemaProvider thema={thema}>
            <NuqsAdapter>{children}</NuqsAdapter>
          </ThemaProvider>
        </DichteProvider>
      </SpracheProvider>
    </QueryClientProvider>
  );
}
