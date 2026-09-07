"use client";

import { useId, useState } from "react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTexte } from "@/i18n/provider";
import { fehleranzeige } from "@/lib/fehlertext";
import { wanduhrzeitFuerEingabe, zeitpunktAusWanduhrzeit } from "@/lib/format";
import type { ProblemFehler } from "@/lib/http";

/** Die Schrittweite der beiden Felder: eine Stunde, die feinste Ebene des Rollups. */
const SCHRITT_SEKUNDEN = 3600;

/**
 * Die beiden Datumsfelder des freien Zeitfensters der Prozessansicht — **neben**
 * dem Zeitraumumschalter, nicht darin (`docs/process-view.md` §41).
 *
 * **Warum daneben:** Der Umschalter ist geteilt mit dem Dashboard
 * (`components/zeitraum-umschalter.tsx`), und das Dashboard ruft ihn ohne
 * vierten Knopf. Stünden die Felder im Umschalter, trüge er Zustand, den nur
 * eine seiner beiden Verwendungen kennt.
 *
 * ## In einer Zeile, in derselben Höhe — nichts springt
 *
 * Die Komponente rendert **keinen eigenen Rahmen**, sondern Kinder der Zeile, in
 * der auch der Umschalter steht: je Feld Beschriftung und Eingabe nebeneinander,
 * beide in Bedienelementhöhe. Beim Klick auf „Frei" wird die Zeile damit breiter
 * und nicht höher, und der Umschalter bleibt stehen. Eine Beschriftung *über*
 * dem Feld und ein Hinweis *darunter* hatten die Zeile wachsen lassen und die
 * Felder gegen die Knöpfe verschoben (Sichtprobe vom 07.09.2026).
 *
 * **Kein Hinweistext ohne Anlass.** Die Felder sagen nichts, solange es nichts
 * zu sagen gibt; eine Meldung erscheint in derselben Zeile hinter den Feldern,
 * und nur dann.
 *
 * ## Was die Felder **nicht** tun
 *
 * Sie rechnen kein Fenster aus, sie runden nichts, sie zeigen keine Ebene und
 * keine Korrektur des eingegebenen Fensters. **Es gibt nichts zu korrigieren** —
 * das ist der Sinn der Zerlegung im Backend. `step=3600` lässt die Eingabe
 * gar nicht erst auf eine krumme Stunde; die Prüfung (`zeitfenster-zu-genau`)
 * fängt die von Hand gebaute Adresse.
 *
 * **`Bis` ist die letzte enthaltene Stunde** — beidseitig geschlossen wie
 * `von`/`bis` der Nachrichtenliste. Die Antwort nennt das Fenster ausschließend;
 * die eine Stunde dazwischen rechnet das Backend, in der Zone der Anwendungsuhr.
 *
 * ## Die `datetime-local`-Falle gilt auch mit `step=3600`
 *
 * Solange die Segmente unvollständig sind, liefert das Feld `value === ""` und
 * feuert **kein `input`** — React sieht kein `onChange`. Herausgegeben wird der
 * Zustand über `validity.badInput`, gelesen an `keyup` und `blur`. Die Lösung
 * ist aus `filterleiste.tsx` übernommen und nicht neu gefunden
 * (`docs/nachrichtenliste.md` §8.2).
 *
 * ## Wo die Meldungen stehen
 *
 * `zeitfenster-unvollstaendig`, `zeitfenster-ungueltig`, `zeitpunkt-ungueltig`,
 * `zeitfenster-zu-genau` und `zeitfenster-zu-gross` stehen **hier**, nicht über
 * der Ansicht: Wer ein freies Fenster ausfüllt, ist mitten in einer Eingabe, und
 * zwischen „Von" und „Bis" liegt zwangsläufig ein Moment mit nur einem
 * Zeitpunkt. **Die Prüfung bleibt im Backend**; hier wird nur entschieden, *wo*
 * die Antwort erscheint (`prozessansicht.ts`, `baumfensterFehler`).
 */
export function BaumfensterFelder({
  von,
  bis,
  aufAenderung,
  fehler,
}: {
  von: Date | null;
  bis: Date | null;
  aufAenderung: (von: Date | null, bis: Date | null) => void;
  /** Die Antwort des Backends, falls sie an die Felder gehört. */
  fehler?: ProblemFehler;
}) {
  const texte = useTexte();
  const zone = useAnzeigezone();
  const vonId = useId();
  const bisId = useId();
  const hinweisId = useId();

  const [angefangen, setAngefangen] = useState({ von: false, bis: false });

  function merkeEingabestand(feld: "von" | "bis", ziel: HTMLInputElement) {
    const halb = ziel.validity.badInput;
    setAngefangen((bisher) => (bisher[feld] === halb ? bisher : { ...bisher, [feld]: halb }));
  }

  /*
   * Dieselbe Reihenfolge wie in der Nachrichtenliste: halb getippt schlägt
   * alles andere; dann die Antwort des Servers; dann der fehlende zweite
   * Zeitpunkt. Ohne Anlass gibt es keinen Text.
   */
  function hinweistext(): string | undefined {
    if (angefangen.von || angefangen.bis) {
      return texte.zeitraum.unvollstaendig;
    }
    if (fehler !== undefined) {
      return fehleranzeige(fehler, texte).text;
    }
    if ((von === null) !== (bis === null)) {
      return texte.zeitraum.beideNoetig;
    }
    return undefined;
  }

  const hinweis = hinweistext();
  const beschriebenDurch = hinweis === undefined ? undefined : hinweisId;

  return (
    <>
      <div className="flex items-center gap-1.5">
        <Label htmlFor={vonId} className="text-beiwerk">
          {texte.zeitraum.von}
        </Label>
        {/* Wanduhrzeit ohne Zone; umgerechnet wird in der Anzeigezone — sonst
            wäre das Fenster gegen die Daten verschoben, sobald jemand nicht in
            der Zone des Servers sitzt. */}
        <Input
          id={vonId}
          type="datetime-local"
          step={SCHRITT_SEKUNDEN}
          className="h-bedienelement w-auto"
          aria-describedby={beschriebenDurch}
          value={wanduhrzeitFuerEingabe(von, zone)}
          onKeyUp={(ereignis) => merkeEingabestand("von", ereignis.currentTarget)}
          onBlur={(ereignis) => merkeEingabestand("von", ereignis.currentTarget)}
          onChange={(ereignis) => {
            merkeEingabestand("von", ereignis.currentTarget);
            aufAenderung(zeitpunktAusWanduhrzeit(ereignis.target.value, zone), bis);
          }}
        />
      </div>
      <div className="flex items-center gap-1.5">
        <Label htmlFor={bisId} className="text-beiwerk">
          {texte.zeitraum.bis}
        </Label>
        <Input
          id={bisId}
          type="datetime-local"
          step={SCHRITT_SEKUNDEN}
          className="h-bedienelement w-auto"
          aria-describedby={beschriebenDurch}
          value={wanduhrzeitFuerEingabe(bis, zone)}
          onKeyUp={(ereignis) => merkeEingabestand("bis", ereignis.currentTarget)}
          onBlur={(ereignis) => merkeEingabestand("bis", ereignis.currentTarget)}
          onChange={(ereignis) => {
            merkeEingabestand("bis", ereignis.currentTarget);
            aufAenderung(von, zeitpunktAusWanduhrzeit(ereignis.target.value, zone));
          }}
        />
      </div>
      {/* Dieselbe ruhige Farbrolle wie am Suchfeld der Liste: Der Nutzer hat
          nichts falsch gemacht, er ist nur noch nicht fertig. */}
      {hinweis === undefined ? null : (
        <p id={hinweisId} className="text-muted-foreground text-beiwerk">
          {hinweis}
        </p>
      )}
    </>
  );
}
