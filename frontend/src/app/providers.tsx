"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import type { ReactNode } from "react";

import { SpracheProvider } from "@/i18n/provider";
import type { Sprache, Texte } from "@/i18n";
import { getQueryClient } from "@/lib/query-client";

/**
 * Die einzige Client-Komponente im Wurzel-Layout.
 *
 * TanStack Query hält den Serverzustand, nuqs den Filterzustand in der URL —
 * damit Ansichten teilbar sind. Die Sprache kommt als Prop vom Server, damit nur
 * die aktive Sprachdatei im Auslieferungszustand landet und nicht beide.
 *
 * Alles darunter bleibt standardmäßig Server-Komponente; "use client" steht so
 * weit unten im Baum wie möglich.
 */
export function Providers({
  sprache,
  texte,
  children,
}: {
  sprache: Sprache;
  texte: Texte;
  children: ReactNode;
}) {
  const queryClient = getQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <SpracheProvider sprache={sprache} texte={texte}>
        <NuqsAdapter>{children}</NuqsAdapter>
      </SpracheProvider>
    </QueryClientProvider>
  );
}
