"use client";

import { useEffect, useState } from "react";
import { RotateCw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Fehler } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereDauer, formatiereZahl } from "@/lib/format";

import type { Vorschlagslauf } from "../api";
import { useVorschlagslauf } from "../hooks";

/**
 * Der eine Knopf, der drei Schritte fährt (E13, E14, E15) — **und der Handlauf
 * daran.**
 *
 * ## Kein Fortschrittsbalken
 *
 * Ein Balken verspricht bekannten Fortschritt. Dieser Aufruf hat keinen: Er
 * fährt drei Schritte in **einer** Transaktion und antwortet erst am Ende
 * (`docs/prozess-katalog-backend.md` §10, Punkt 7). Ein Balken, der sich nach
 * Gefühl füllt, ist eine Behauptung über etwas, das niemand misst.
 *
 * **Sichtbar ist stattdessen die verstrichene Zeit.** Sie behauptet nichts, sie
 * berichtet — und sie ist genau die Angabe, die der Nutzer braucht, um zu
 * entscheiden, ob er wartet.
 *
 * ## Die Dauer bleibt nach dem Lauf stehen
 *
 * Sie ist nicht nur Beiwerk, sondern die **Messung**: Der Schreibweg des Laufs
 * ist ungemessen — bis zu 733 Zeilen je Druck, und
 * `docs/prozess-katalog-backend.md` §10 nennt für die Testkopie 10 bis 25 s je
 * `COMMIT` gegen M80s Schranke von unter zehn Sekunden für 1.490 Zeilen. Wer
 * den Knopf drückt, liest die Zahl hier ab.
 *
 * ## Alle acht Zahlen, auch die Nullen
 *
 * Ein Lauf, der nichts bewegt hat, muss von einem erfolgreichen unterscheidbar
 * sein. Ohne `bestandGeprueft` und `ohneNachrichten` sähe ein Bestandslauf, der
 * wegen eines Fehlers null Zeilen anfasst, genauso aus wie einer, der 733
 * aufgefrischt hat.
 */
export function LaufKnopf({ gesperrt }: { gesperrt: boolean }) {
  const texte = useTexte();
  const lauf = useVorschlagslauf();
  const { verstrichen, starte, halte } = useLaufzeit(lauf.isPending);

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2" aria-busy={lauf.isPending}>
        <Button
          type="button"
          disabled={lauf.isPending || gesperrt}
          onClick={() => {
            starte();
            lauf.mutate(undefined, { onSettled: halte });
          }}
          className="min-h-beruehrung"
        >
          <RotateCw aria-hidden="true" />
          {lauf.isPending ? texte.katalog.lauf.laeuft : texte.katalog.lauf.starten}
        </Button>

        {lauf.isPending ? (
          /*
           * `aria-hidden`: Ein Zähler, der jede Sekunde eine neue Ansage
           * auslöste, machte den Lauf für ein Vorleseprogramm unbenutzbar. Dass
           * etwas läuft, sagt `aria-busy` am Container und der gesperrte Knopf.
           */
          <span aria-hidden="true" className="text-muted-foreground text-beiwerk">
            {einsetzen(texte.katalog.lauf.laeuftSeit, {
              dauer: formatiereDauer(verstrichen, texte.katalog.lauf.dauer),
            })}
          </span>
        ) : (
          <span className="text-muted-foreground text-beiwerk max-w-prose">
            {texte.katalog.lauf.hinweis}
          </span>
        )}
      </div>

      {lauf.error ? <Fehler fehler={lauf.error} /> : null}

      {lauf.data ? <Laufergebnis ergebnis={lauf.data} dauer={verstrichen} /> : null}
    </div>
  );
}

/**
 * Die verstrichene Zeit eines laufenden Aufrufs, sekundenweise.
 *
 * Bezugspunkt ist die Uhr des **Browsers** — hier gemessen wird eine Dauer und
 * kein fachlicher Zeitpunkt. Für Zeitpunkte gilt weiterhin die Anzeigezone aus
 * der Selbstauskunft (`docs/frontend-grundlagen.md` §4).
 *
 * Nach dem Lauf bleibt der Wert stehen: Er ist die Messung, die der Handlauf zu
 * diesem Knopf verlangt.
 */
function useLaufzeit(laeuft: boolean) {
  const [start, setStart] = useState<number | null>(null);
  const [jetzt, setJetzt] = useState(0);

  useEffect(() => {
    if (!laeuft) {
      return;
    }
    const kennung = window.setInterval(() => setJetzt(Date.now()), 1000);
    return () => window.clearInterval(kennung);
  }, [laeuft]);

  return {
    verstrichen: start === null ? 0 : Math.max(0, (jetzt - start) / 1000),
    starte: () => {
      const beginn = Date.now();
      setStart(beginn);
      setJetzt(beginn);
    },
    /**
     * Der Halt gehört an das **Ereignis** und nicht in einen Effekt: Das Ende
     * des Aufrufs ist etwas, das passiert, und kein Zustand, mit dem sich etwas
     * abgleichen ließe. Ohne ihn bliebe die Anzeige bis zu eine Sekunde hinter
     * der Wahrheit zurück — und genau diese Sekunde ist die gemessene.
     */
    halte: () => setJetzt(Date.now()),
  };
}

/** Die acht Zahlen, in drei Gruppen — so wie das Backend sie zählt. */
function Laufergebnis({ ergebnis, dauer }: { ergebnis: Vorschlagslauf; dauer: number }) {
  const texte = useTexte();
  const sprache = useSprache();
  const beschriftung = texte.katalog.lauf.zahlen;

  const gruppen: readonly (readonly (readonly [string, number])[])[] = [
    [
      [beschriftung.angelegt, ergebnis.angelegt],
      [beschriftung.aufgefrischt, ergebnis.aufgefrischt],
      [beschriftung.unberuehrt, ergebnis.unberuehrt],
    ],
    [
      [beschriftung.regelA, ergebnis.regelA],
      [beschriftung.regelB, ergebnis.regelB],
      [beschriftung.keine, ergebnis.keine],
    ],
    [
      [beschriftung.bestandGeprueft, ergebnis.bestandGeprueft],
      [beschriftung.ohneNachrichten, ergebnis.ohneNachrichten],
    ],
  ];

  return (
    <div
      role="status"
      className="border-border bg-card flex flex-col gap-2 rounded-lg border px-3 py-2"
    >
      <p className="text-beiwerk">
        <span className="font-medium">{texte.katalog.lauf.ergebnisTitel}</span>{" "}
        <span className="text-muted-foreground">
          {einsetzen(texte.katalog.lauf.ergebnisDauer, {
            dauer: formatiereDauer(dauer, texte.katalog.lauf.dauer),
          })}
        </span>
      </p>
      <div className="flex flex-wrap gap-x-6 gap-y-2">
        {gruppen.map((gruppe) => (
          <dl key={gruppe[0][0]} className="text-beiwerk flex flex-col gap-0.5">
            {gruppe.map(([name, wert]) => (
              <div key={name} className="flex gap-2">
                <dt className="text-muted-foreground">{name}</dt>
                <dd className="font-medium tabular-nums">{formatiereZahl(wert, sprache)}</dd>
              </div>
            ))}
          </dl>
        ))}
      </div>
    </div>
  );
}
