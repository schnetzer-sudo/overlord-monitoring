"use client";

import Link from "next/link";
import { AlertTriangle, type LucideIcon } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl, formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";
import { statusVordergrund, type Statusart } from "@/lib/status-farbe";

import type { Auffaelligkeit, AuffaelligerProzess, Fenster } from "../api";
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
 * ## Das Zeichen je Zeile (Entscheidung **E‑91**, 04.09.2026)
 *
 * **Am 03.09.2026 ist die Kategoriekennzeichnung je Zeile gefallen**, und die
 * Begründung war richtig: *Eine Plakette, die an jeder Zeile dasselbe Wort
 * sagt, unterscheidet nichts mehr.* Sie hat aber mehr mitgenommen als das Wort
 * — seither steht **nirgends im Bild**, dass es sich um Fehler handelt. Die
 * Überschrift lautet „Zuletzt aufgefallen"; *aufgefallen* ist keine Kategorie.
 *
 * **Das Zeichen kehrt zurück, das Wort nicht.** Es ist dieselbe Abwägung wie in
 * der Statusplakette der Liste: `visuelles-konzept.md` §3 verlangt „zusätzlich
 * eine Beschriftung **oder** ein Zeichen", und die Zeichen unterscheiden sich in
 * ihrer **Form**, nicht in ihrer Farbe. Ein Zeichen kostet eine Zeile nichts an
 * Breite; ein Wort an jeder Zeile kostete sie und sagte zehnmal dasselbe.
 *
 * **Es ist nicht die `StatusPlakette`.** Die liegt in `features/nachrichten`,
 * und ein Feature importiert nicht aus einem Nachbarfeature
 * (`frontend-grundlagen.md` §8) — dieselbe Abgrenzung, aus der die Kacheln ihr
 * Zeichen selbst setzen. **Geteilt ist die Farbe**, über `statusVordergrund` in
 * `lib/status-farbe.ts`, und nicht die Komponente.
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

/**
 * Das Zeichen je Auffälligkeit — **ein Verzeichnis und kein fester Wert**.
 *
 * Heute steht darin ein Eintrag, weil `Auffaelligkeit` eine Aufzählung mit einem
 * Wert ist (offener Punkt 133). Als Verzeichnis geschrieben, weil Regel Q3
 * verlangt, dass Problemkategorien **getrennt** geführt und nie zu „Problem"
 * zusammengefasst werden: Kommt eine zweite zurück, ist hier eine Zeile zu
 * ergänzen — und TypeScript verlangt sie, statt still das Warndreieck
 * weiterzumalen.
 */
const ZEICHEN: Record<Auffaelligkeit, LucideIcon> = {
  FEHLER: AlertTriangle,
};

/**
 * Woher die **Farbe** des Zeichens kommt — und warum über einen Umweg.
 *
 * `lib/status-farbe.ts` hält zwei Zuordnungen: `Statusart` → Rolle und
 * `Problemkategorie` → Rolle. **`FEHLER` steht in der ersten**, und der Kopf
 * jener Datei sagt auch, warum das so bleibt: *„Fehler ist ein Statuswert und
 * läuft über `Statusart`; eine zweite Zuordnung dorthin wäre ein zweiter Weg zu
 * Rot."*
 *
 * `zeile.kategorie` ist aber eine **`Auffaelligkeit`** und keine `Statusart` —
 * zwei Aufzählungen, die heute zufällig einen Namen teilen. Dieses Verzeichnis
 * schreibt die Brücke **einmal und sichtbar** hin, statt den einen Typ in den
 * anderen zu reichen, weil der Aufruf zufällig durchginge. Rot bleibt damit an
 * genau einer Stelle vergeben.
 */
const FARBQUELLE: Record<Auffaelligkeit, Statusart> = {
  FEHLER: "FEHLER",
};

function Zeile({ zeile, fenster }: { zeile: AuffaelligerProzess; fenster: Fenster }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const anzahl = formatiereZahl(zeile.anzahl, sprache);
  const vorlage =
    zeile.anzahl === 1
      ? texte.dashboard.aufgefallen.anzahlEins
      : texte.dashboard.aufgefallen.anzahlViele;

  const Zeichen = ZEICHEN[zeile.kategorie];
  const kategorie = texte.dashboard.aufgefallen.kategorie[zeile.kategorie];

  return (
    <li className="border-border flex items-center gap-3 border-b py-1.5 last:border-b-0">
      <span
        className="text-muted-foreground text-beiwerk shrink-0 tabular-nums"
        title={formatiereZeitpunktGenau(zeile.zuletzt, sprache, zone)}
      >
        {formatiereZeitpunkt(zeile.zuletzt, sprache, zone)}
      </span>

      {/*
       * **Das Zeichen steht zwischen Zeitpunkt und Name** — dieselbe Reihenfolge
       * wie in der Nachrichtenliste, deren Spalten *Zeitpunkt · Status · Ablauf*
       * lauten. Die Zeile, auf die der Verweis führt, sieht damit aus wie die
       * Zeile, von der er ausgeht.
       *
       * **Das Wort steht im `title` und im Vorlese-Markup, nicht im Bild.** Es
       * wäre an jeder Zeile dasselbe — genau der Grund, aus dem die Plakette am
       * 03.09.2026 gefallen ist. Das Zeichen unterscheidet sich dagegen in der
       * **Form**, sobald es je eine zweite Kategorie gibt.
       */}
      <span
        className={`shrink-0 ${statusVordergrund(FARBQUELLE[zeile.kategorie])}`}
        title={kategorie}
      >
        <Zeichen aria-hidden="true" className="size-4" />
        <span className="sr-only">{kategorie}</span>
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
