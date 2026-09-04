"use client";

import Link from "next/link";

import { useAnzeigezone } from "@/components/zeitzone";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";

import type { AuffaelligeNachricht } from "../api";
import { nachrichtZiel } from "../verweise";

/**
 * „Zuletzt aufgefallen" — die auffälligen Nachrichten im Fenster, neueste
 * zuerst.
 *
 * ## ⚠️ Die Kategoriekennzeichnung je Zeile ist am 03.09.2026 verwaist
 *
 * Hier trug jede Zeile eine **Plakette mit der Kategorie** — *Fehler* oder
 * *Überfällig* —, und das war die eigentliche Aussage des Blocks: zwei
 * gleichrangige Problemkategorien untereinander (Regel Q3). **Seit E‑71 gibt es
 * eine.** Der Block ist ein einziges Statement und trägt nur noch Fehler; eine
 * Plakette, die an jeder Zeile dasselbe Wort sagt, unterscheidet nichts mehr und
 * behauptet eine Auswahl, die es nicht gibt.
 *
 * **Die Überschrift bleibt und trägt die Aussage jetzt allein.** Sie steht
 * einmal über dem Block statt einmal je Zeile — dieselbe Auskunft, an der
 * Stelle, an der sie noch etwas unterscheidet.
 *
 * > **Mit der Plakette ist auch der Rohstatus je Zeile gefallen.** Er stand
 * > ausschließlich in ihrem `title` und im Vorlese-Markup, nie sichtbar. Ihn
 * > sichtbar nachzuziehen wäre eine neue Gestaltungsentscheidung über diesen
 * > Block und keine Aufräumarbeit; er steht im Detail, einen Klick entfernt.
 * > Als offener Punkt vermerkt (`docs/dashboard.md` §11).
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

function Zeile({ zeile }: { zeile: AuffaelligeNachricht }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  return (
    <li className="border-border flex items-center gap-3 border-b py-1.5 last:border-b-0">
      <span
        className="text-muted-foreground text-beiwerk shrink-0 tabular-nums"
        title={formatiereZeitpunktGenau(zeile.zeitpunkt, sprache, zone)}
      >
        {formatiereZeitpunkt(zeile.zeitpunkt, sprache, zone)}
      </span>

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
