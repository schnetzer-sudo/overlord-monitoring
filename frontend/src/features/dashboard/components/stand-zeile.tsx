"use client";

import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt } from "@/lib/format";

import type { Stand } from "../api";

/**
 * Wann der Rollup zuletzt gelaufen ist — **absolut, nicht relativ**
 * (Entscheidung E‑o).
 *
 * ## Warum keine relative Zeit
 *
 * Die Antwort trägt **kein `jetzt`-Feld**, und `formatiereRelativ` rechnet
 * gegen die Uhr des **Browsers** (`lib/format.ts`). Im Profil `dev` steht die
 * Anwendungsuhr auf dem 30.12.2025 und die Browseruhr Monate später — dort
 * stünde „vor acht Monaten", und das wäre eine Aussage über die verstellte Uhr
 * und nicht über den Rollup. `fenster.bis` als Bezug zu nehmen hilft nicht: Bei
 * `12M` liegt es bis zu einen Monat von der Uhr entfernt.
 *
 * > **Bekannte Grenze:** „Wie alt sind diese Zahlen" bleibt damit eine
 * > Kopfrechnung. Ein `jetzt`-Feld nachzurüsten wäre eine Backend-Änderung und
 * > gehört nicht in einen Frontend-Schritt.
 *
 * ## Drei Lagen, und jede sagt etwas anderes
 *
 * | Antwort | Was dasteht |
 * |---|---|
 * | `stand === null` | Es hat noch keinen abgeschlossenen, fehlerfreien Lauf gegeben |
 * | `beendetAm === null` | dasselbe — ein Lauf ohne Ende ist keiner, über den sich etwas sagen lässt |
 * | beides gesetzt | Zeitpunkt und Laufart |
 *
 * **Die Laufart wird übersetzt, wenn sie bekannt ist, und sonst roh gezeigt**
 * (Regel Q4). `VOLL` und `DELTA` sind die beiden, die `rollup/LaufArt` kennt;
 * ein dritter Wert bekäme hier keinen geratenen Text.
 */
export function StandZeile({ stand }: { stand: Stand | null }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  if (stand === null || stand.beendetAm === null) {
    return <p className="text-muted-foreground text-beiwerk">{texte.dashboard.stand.ohneLauf}</p>;
  }

  const satz = einsetzen(texte.dashboard.stand.satz, {
    zeitpunkt: formatiereZeitpunkt(stand.beendetAm, sprache, zone),
  });

  return (
    <p className="text-muted-foreground text-beiwerk tabular-nums">
      {satz}
      {stand.art === null ? null : ` · ${laufart(stand.art, texte)}`}
    </p>
  );
}

function laufart(art: string, texte: ReturnType<typeof useTexte>): string {
  switch (art) {
    case "VOLL":
      return texte.dashboard.stand.artVOLL;
    case "DELTA":
      return texte.dashboard.stand.artDELTA;
    default:
      return art;
  }
}
