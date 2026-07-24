"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import type { ReactNode } from "react";

import { getQueryClient } from "@/lib/query-client";

/**
 * Die einzige Client-Komponente im Wurzel-Layout.
 *
 * <p>TanStack Query haelt den Serverzustand, nuqs den Filterzustand in der URL —
 * damit Ansichten teilbar sind. Alles darunter bleibt standardmaessig
 * Server-Komponente; "use client" steht so weit unten im Baum wie moeglich.
 */
export function Providers({ children }: { children: ReactNode }) {
  const queryClient = getQueryClient();

  return (
    <QueryClientProvider client={queryClient}>
      <NuqsAdapter>{children}</NuqsAdapter>
    </QueryClientProvider>
  );
}
