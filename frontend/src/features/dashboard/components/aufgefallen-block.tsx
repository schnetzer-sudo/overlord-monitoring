"use client";

import Link from "next/link";
import { AlertTriangle, Clock } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { useAnzeigezone } from "@/components/zeitzone";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";
import { problemKlassenOhneKontur, statusKlassenOhneKontur } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { AuffaelligeNachricht, Auffaelligkeit } from "../api";
import { nachrichtZiel } from "../verweise";

/**
 * „Zuletzt aufgefallen" — Fehler **und** Überfällige im Fenster, neueste zuerst.
 *
 * ## Die Plakette trägt `kategorie`, nicht `status`
 *
 * Das ist die eigentliche Aussage dieses Blocks: Hier stehen **zwei
 * Problemkategorien untereinander**, und beide sind gleichrangig (Regel Q3).
 * Ein Rohstatus an dieser Stelle beantwortete eine andere Frage — *welcher
 * Fehler* statt *ist es einer*. Der Rohwert geht trotzdem nicht verloren: Er
 * steht im `title` der Plakette, damit man ihn gegen das Altwerkzeug halten kann.
 *
 * **Die beiden Mengen sind disjunkt.** *Überfällig* setzt voraus, dass die
 * Nachricht **nicht** in einem Endstatus ist, und *Fehler* ist einer — das
 * Backend leitet die Kategorie daraus ab und rät nichts (`docs/dashboard.md`
 * §7a). Deshalb gibt es hier keine Zeile mit zwei Plaketten.
 *
 * ## Die Gestalt ist dieselbe wie bei den Kacheln (Entscheidung E‑u)
 *
 * Fläche und Vordergrund, **keine Kontur** — und hier ist der Grund am
 * dichtesten zu sehen: Die beiden Kategorien wechseln sich Zeile für Zeile ab.
 * Mit Kontur trüge jede zweite Zeile einen sichtbar gezeichneten Ring und die
 * Liste sähe gestreift aus, ohne dass die Streifen etwas bedeuteten.
 * **Für Kachel und Zeile fällt die Entscheidung deshalb gleich aus**; der
 * Fehler wäre derselbe, nur in der Zeile dichter
 * ([`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md) §3).
 *
 * ## Zeitpunkte absolut (Entscheidung E‑o)
 *
 * In der Anzeigezone und **nicht relativ**. Die Antwort trägt kein `jetzt`-Feld,
 * und der Browser rechnete gegen seine eigene Uhr — im Profil `dev` stünde dort
 * „vor acht Monaten". Der genaue Wert mit Sekunden steht im `title`.
 *
 * ## Der Verweis führt auf die eigene Route
 *
 * `/nachrichten/<id>` und nicht `/nachrichten?nachricht=<id>`: Das zweite
 * öffnete das Panel *neben der Liste* und brächte damit eine Liste mit, die
 * niemand angefragt hat — samt ihrem Standardfenster, das mit dem des Dashboards
 * nichts zu tun hat.
 */
export function AufgefallenBlock({ zeilen }: { zeilen: readonly AuffaelligeNachricht[] }) {
  const texte = useTexte();

  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-basis font-semibold">{texte.dashboard.aufgefallen.titel}</h2>

      {zeilen.length === 0 ? (
        <p className="text-muted-foreground text-beiwerk">{texte.dashboard.aufgefallen.leer}</p>
      ) : (
        <ul className="flex flex-col">
          {zeilen.map((zeile) => (
            <Zeile key={zeile.messageId} zeile={zeile} />
          ))}
        </ul>
      )}
    </div>
  );
}

const ZEICHEN: Record<Auffaelligkeit, typeof AlertTriangle> = {
  FEHLER: AlertTriangle,
  UEBERFAELLIG: Clock,
};

/**
 * Die Farbe der Plakette — **über die Kategorie, an genau einer Stelle
 * zugeordnet**.
 *
 * `FEHLER` läuft über die Statusrolle, `UEBERFAELLIG` über die Problemrolle;
 * das sind die beiden Tabellen in `lib/status-farbe.ts` und nicht zwei Wege zu
 * derselben Farbe. Ein zweiter Weg zu Rot ist genau das, was die Datei dort
 * verhindert.
 */
function plakettenKlassen(kategorie: Auffaelligkeit): string {
  return kategorie === "FEHLER"
    ? statusKlassenOhneKontur("FEHLER")
    : problemKlassenOhneKontur("UEBERFAELLIG");
}

function Zeile({ zeile }: { zeile: AuffaelligeNachricht }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();
  const Zeichen = ZEICHEN[zeile.kategorie];
  const beschriftung =
    zeile.kategorie === "FEHLER" ? texte.einordnung.FEHLER : texte.problem.ueberfaellig;

  return (
    <li className="border-border flex items-center gap-3 border-b py-1.5 last:border-b-0">
      <span
        className="text-muted-foreground text-beiwerk shrink-0 tabular-nums"
        title={formatiereZeitpunktGenau(zeile.zeitpunkt, sprache, zone)}
      >
        {formatiereZeitpunkt(zeile.zeitpunkt, sprache, zone)}
      </span>

      <Badge
        variant="outline"
        className={cn(
          "h-auto shrink-0 gap-1.5 border px-2 py-0.5",
          plakettenKlassen(zeile.kategorie),
        )}
        title={`${texte.nachrichten.rohwert}: ${zeile.status}`}
      >
        <Zeichen aria-hidden="true" />
        {beschriftung}
        <span className="sr-only">
          {" "}
          — {texte.nachrichten.rohwert}: {zeile.status}
        </span>
      </Badge>

      {/*
       * **Prozess bzw. `sosName`, und beide dürfen fehlen.** „Nicht zugeordnet
       * heißt nicht zugeordnet" (Regel Q4) — geraten wird hier nichts, auch
       * nicht aus der Kennung.
       */}
      <Link
        href={nachrichtZiel(zeile.messageId)}
        title={texte.dashboard.aufgefallen.zeileOeffnen}
        className="focus-visible:ring-ring text-beiwerk min-w-0 flex-1 truncate rounded-md hover:underline focus-visible:ring-2 focus-visible:outline-none"
      >
        {zeile.sosName ?? zeile.processId ?? (
          <span className="text-muted-foreground italic">
            {texte.dashboard.aufgefallen.ohneProzess}
          </span>
        )}
      </Link>
    </li>
  );
}
