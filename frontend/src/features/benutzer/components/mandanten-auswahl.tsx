"use client";

import { useId, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Fehler, Laden } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";
import { MANDANTEN_SCHLUESSEL, holeMandanten, type Mandant } from "@/lib/mandanten";

import type { Nutzerzeile } from "../api";
import { istLetzteZuordnung, mengeGeaendert, umschalten, wahlmoeglichkeiten } from "../zuordnung";

/**
 * Die Mandantenmenge eines Kontos — **die dritte Ausnahme von Regel M1** (E4).
 *
 * ## Die Menge wird vollständig ersetzt
 *
 * Kein Zusatz, kein Entzug: Die Oberfläche schickt **immer die vollständige
 * Zielmenge**. Bei dreißig Konten und zehn Mandanten ist sie winzig, und eine
 * Mengenersetzung hat genau ein Ergebnis, während eine Differenzbildung zwei
 * Fehlerarten hat.
 *
 * Deshalb ist die Bauform eine **Mehrfachauswahl mit Häkchen** und keine Liste
 * mit Hinzufügen- und Entfernen-Knöpfen: Was auf dem Bildschirm steht, *ist* der
 * Rumpf der Anfrage. Zwei Knöpfe zeigten stattdessen zwei Vorgänge, die es nicht
 * gibt.
 *
 * ## Woher die wählbaren Mandanten kommen
 *
 * Aus `GET /api/mandanten` — **derselben Quelle wie die Mandantenauswahl seit
 * Schritt 3** (`lib/mandanten.ts`), unter demselben Schlüssel und damit meist
 * schon im Zwischenspeicher. Für einen ADMIN sind das alle.
 *
 * **`SYSTEM` und `WOC` werden nicht ausgesiebt.** Beide sind technisch und kein
 * Kunde, und die Mandantenauswahl zeigt sie trotzdem; eine zweite Behandlung nur
 * hier wäre eine Regel im Browser darüber, wer was sehen darf — und sie nähme
 * genau die Zuordnung weg, die jemand braucht, der für einen der beiden
 * zuständig ist.
 *
 * ## Die letzte Zuordnung ist erkennbar und nicht nur aufgefangen (E10)
 *
 * Ist genau ein Häkchen gesetzt, lässt es sich nicht abnehmen, und darunter
 * steht, warum. Der Fall steht vollständig im Entwurf, den der Nutzer vor sich
 * hat — ihn in ein `409 letzte-mandantenzuordnung` laufen zu lassen wäre eine
 * Fehlermeldung für etwas, das die Auswahl selbst sagen kann. **Der Serverfehler
 * bleibt trotzdem übersetzt:** Er ist die verbindliche Prüfung, das hier ist der
 * Handlauf davor.
 *
 * **Ein Tausch bleibt möglich**, und das ist der Grund für die genaue Bedingung:
 * Erst das neue Häkchen setzen, dann ist das alte wieder abwählbar.
 *
 * ## Doppelte Kennungen prüft die Oberfläche nicht
 *
 * Sie kann gar keine erzeugen — Häkchen sind eine Menge. Und selbst wenn: Das
 * Backend zieht sie still zusammen. Eine eigene Prüfung wäre die zweite Stelle
 * für dieselbe Regel.
 *
 * ## Der Entwurf setzt sich nach dem Speichern von selbst zurück
 *
 * Über den `key` am Aufrufer, nicht über einen Effekt: Ändert sich die
 * gespeicherte Menge, hängt React die Auswahl neu ein, und der Entwurf beginnt
 * bei der Antwort. Ein `setState` im Effekt wäre hier der naheliegende und der
 * falsche Weg — er ist im Projekt durch `react-hooks/set-state-in-effect`
 * ausgeschlossen, und er hätte einen Zwischenzustand, in dem zwei Wahrheiten
 * nebeneinander stehen.
 */
/**
 * Wie lange die Mandantenliste als frisch gilt. Dieselbe Zahl und derselbe Grund
 * wie bei den Partnervorschlägen der Katalogpflege: Stammdaten, die sich während
 * einer Pflegesitzung nicht ändern.
 */
const MANDANTEN_HALTBARKEIT = 15 * 60 * 1000;

export function MandantenAuswahl({
  zeile,
  gesperrt,
  aufSpeichern,
  fehler,
}: {
  zeile: Nutzerzeile;
  gesperrt: boolean;
  aufSpeichern: (mandanten: readonly string[]) => void;
  fehler: React.ReactNode;
}) {
  const texte = useTexte();
  const gruppeId = useId();
  const [entwurf, setEntwurf] = useState<readonly string[]>(zeile.tenants);

  /*
   * Dieselbe Abfrage wie die Mandantenauswahl, deshalb meist schon beantwortet.
   *
   * **Länger gehalten als die Voreinstellung**, dieselbe Bauform wie die
   * Partnervorschläge der Katalogpflege (`features/katalog/hooks.ts`
   * `usePartner`): Es sind Stammdaten aus `GlassfishDB.Mandant`, zehn Zeilen,
   * und sie ändern sich nicht, während jemand ein Konto pflegt.
   *
   * > **Der Grund ist gemessen und nicht vorgesorgt** (Sichtprüfung
   * > 26.08.2026). Nach jedem Speichern wechselt der `key` dieser Komponente —
   * > so setzt sich der Entwurf zurück —, React hängt sie neu ein, und
   * > `useQuery` holt beim Einhängen nach, sobald die Antwort älter als
   * > `staleTime` ist. Mit den dreißig Sekunden aus `lib/query-client.ts` ging
   * > deshalb **nach jeder Mengenersetzung** ein zusätzliches
   * > `GET /api/mandanten` hinaus — auf einer Seite, deren ganzer Punkt ist,
   * > dass die Antwort den Zwischenspeicher setzt, statt nachzuholen.
   */
  const mandanten = useQuery<Mandant[]>({
    queryKey: MANDANTEN_SCHLUESSEL,
    queryFn: holeMandanten,
    staleTime: MANDANTEN_HALTBARKEIT,
    gcTime: MANDANTEN_HALTBARKEIT,
  });

  if (mandanten.isPending) {
    return <Laden zeilen={2} />;
  }

  if (mandanten.error) {
    return <Fehler fehler={mandanten.error} aufWiederholen={() => void mandanten.refetch()} />;
  }

  const auswahl = wahlmoeglichkeiten(zeile.tenants, mandanten.data);
  const geaendert = mengeGeaendert(entwurf, zeile.tenants);

  return (
    <div className="flex flex-col gap-2">
      <span id={gruppeId} className="text-basis font-medium">
        {texte.benutzer.mandanten.titel}
      </span>

      <ul
        // `group` und nicht `listbox`: Es sind echte Kontrollkästchen mit
        // eigenen Beschriftungen, und die tragen ihre Semantik schon selbst.
        role="group"
        aria-labelledby={gruppeId}
        className="flex flex-wrap gap-x-4 gap-y-1"
      >
        {auswahl.map((mandant) => {
          const gewaehlt = entwurf.includes(mandant.id);
          const letzte = istLetzteZuordnung(entwurf, mandant.id);
          return (
            <li key={mandant.id} className="min-w-0">
              <label className="min-h-beruehrung flex items-center gap-2">
                <Checkbox
                  checked={gewaehlt}
                  disabled={gesperrt || letzte}
                  onCheckedChange={() => setEntwurf((bisher) => umschalten(bisher, mandant.id))}
                />
                <span className="font-mono">{mandant.id}</span>
                <span className="text-muted-foreground text-beiwerk min-w-0 truncate">
                  {mandant.nichtWaehlbar ? texte.benutzer.mandanten.unbekannt : mandant.name}
                </span>
              </label>
            </li>
          );
        })}
      </ul>

      {entwurf.length === 1 ? (
        <p className="text-muted-foreground text-beiwerk max-w-prose">
          {texte.benutzer.mandanten.letzteZuordnung}
        </p>
      ) : null}

      <div className="flex flex-wrap items-center gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={gesperrt || !geaendert}
          onClick={() => aufSpeichern(entwurf)}
          className="min-h-beruehrung"
        >
          {texte.benutzer.mandanten.speichern}
        </Button>
        {geaendert ? (
          <Button
            type="button"
            variant="ghost"
            disabled={gesperrt}
            onClick={() => setEntwurf(zeile.tenants)}
            className="min-h-beruehrung"
          >
            {texte.benutzer.mandanten.verwerfen}
          </Button>
        ) : null}
      </div>

      {fehler}
    </div>
  );
}
