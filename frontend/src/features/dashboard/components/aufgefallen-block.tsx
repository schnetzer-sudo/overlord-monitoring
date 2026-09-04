"use client";

import Link from "next/link";

import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl, formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";

import type { AuffaelligerProzess, Fenster } from "../api";
import { prozessFehlerZiel } from "../verweise";

/**
 * „Zuletzt aufgefallen" — **die auffälligen Prozesse im Fenster**, der jüngste
 * zuerst (Entscheidung **E‑90**, 04.09.2026).
 *
 * ## ⚠️ Der Block listete Nachrichten, wo er Prozesse listen sollte
 *
 * Bis heute stand hier **eine Zeile je Nachricht**. Am laufenden System sah das
 * so aus: zehn Zeilen, zehnmal derselbe Zeitstempel, zehnmal derselbe Ablauf.
 * Der Grund steht in den Daten und nicht im Bau — bei `NEXANS` über 48 Stunden
 * stammen **49 der 50 Fehler aus einem einzigen Prozess** (M146). Ein Prozess
 * füllte die Liste allein, und **keine Zeile trug eine eigene Auskunft**.
 *
 * Seither trägt jede Zeile **Anzahl** und **jüngsten Zeitpunkt** je Prozess.
 *
 * > ### Es sind selten zehn, und das ist die Auskunft und kein Mangel
 * >
 * > Über den *gesamten* Bestand der Testkopie hat `NEXANS` Fehler in **drei**
 * > Prozessen, `SUTTONS` in **einem**, `VOTG` in **einem** (M146). Der Block
 * > zeigt damit zwei bis drei Zeilen statt zehn — und sagt damit etwas, das die
 * > zehn gleichen Zeilen davor verschwiegen haben: **Es ist immer derselbe
 * > Prozess.** Der Deckel von zehn bleibt, er greift auf diesen Daten nur nicht.
 *
 * ## Was die Zeile verloren hat, und wo es steht
 *
 * Die Kennung der einzelnen Nachricht und ihr Rohstatus. Beide gehören zu einer
 * Nachricht, und eine Zeile, die einen ganzen Prozess zusammenfasst, hat keinen
 * Rohstatus — sie kann zwanzig verschiedene enthalten. Der Verweis führt
 * deshalb in die **Liste**, gefiltert auf genau die Menge, die die Zahl daneben
 * nennt: dieser Prozess, `FEHLER`, dasselbe Fenster. **Offener Punkt 136 ist
 * damit gegenstandslos** statt erledigt — er verlangte den Rohstatus je Zeile
 * zurück, und die Zeile, um die es ging, gibt es nicht mehr.
 *
 * ## Zeitpunkte absolut (Entscheidung E‑o)
 *
 * In der Anzeigezone und **nicht relativ**. Die Antwort trägt kein `jetzt`-Feld,
 * und der Browser rechnete gegen seine eigene Uhr — im Profil `dev` stünde dort
 * „vor acht Monaten". Der genaue Wert mit Sekunden steht im `title`.
 */
export function AufgefallenBlock({
  zeilen,
  fenster,
}: {
  zeilen: readonly AuffaelligerProzess[];
  fenster: Fenster;
}) {
  const texte = useTexte();

  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-basis font-semibold">{texte.dashboard.aufgefallen.titel}</h2>

      {zeilen.length === 0 ? (
        <p className="text-muted-foreground text-beiwerk">{texte.dashboard.aufgefallen.leer}</p>
      ) : (
        <ul className="flex flex-col">
          {zeilen.map((zeile) => (
            <Zeile key={zeile.processId} zeile={zeile} fenster={fenster} />
          ))}
        </ul>
      )}
    </div>
  );
}

function Zeile({ zeile, fenster }: { zeile: AuffaelligerProzess; fenster: Fenster }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const anzahl = formatiereZahl(zeile.anzahl, sprache);
  const vorlage =
    zeile.anzahl === 1
      ? texte.dashboard.aufgefallen.anzahlEins
      : texte.dashboard.aufgefallen.anzahlViele;

  return (
    <li className="border-border flex items-center gap-3 border-b py-1.5 last:border-b-0">
      <span
        className="text-muted-foreground text-beiwerk shrink-0 tabular-nums"
        title={formatiereZeitpunktGenau(zeile.zuletzt, sprache, zone)}
      >
        {formatiereZeitpunkt(zeile.zuletzt, sprache, zone)}
      </span>

      {/*
       * **Der Klarname des Prozesses, und er darf fehlen.** „Nicht zugeordnet
       * heißt nicht zugeordnet" (Regel Q4) — geraten wird hier nichts, auch
       * nicht aus der Kennung. Fehlt der Name, steht die Kennung da; fehlte
       * auch die, käme die Zeile gar nicht erst durch die Mandantenkette.
       */}
      <Link
        href={prozessFehlerZiel(fenster, zeile.processId)}
        title={einsetzen(texte.dashboard.aufgefallen.zeileOeffnen, { anzahl })}
        className="focus-visible:ring-ring text-beiwerk min-w-0 flex-1 truncate rounded-md hover:underline focus-visible:ring-2 focus-visible:outline-none"
      >
        {zeile.processName ?? zeile.processId}
      </Link>

      {/*
       * **Die Zahl steht rechts und trägt ihr Wort im `aria-label`.** Sichtbar
       * ist die Ziffer allein — der Blockkopf sagt bereits, worum es geht, und
       * das Wort „Fehler" an jeder Zeile sagte es zehnmal. Für ein
       * Vorleseprogramm ist die nackte Zahl aber keine Auskunft; dort steht der
       * ganze Satz.
       */}
      <span
        className="text-beiwerk shrink-0 font-medium tabular-nums"
        aria-label={einsetzen(vorlage, { anzahl })}
      >
        {anzahl}
      </span>
    </li>
  );
}
