"use client";

import { useId, useState } from "react";

import { AuswahlFeld, type Auswahleintrag } from "@/components/auswahl-feld";
import { useAnzeigezone } from "@/components/zeitzone";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { fehleranzeige } from "@/lib/fehlertext";
import {
  datumUndUhrzeitFuerEingabe,
  stundeneintraege,
  zeitpunktAusDatumUndUhrzeit,
  type DatumUndUhrzeit,
  type Fenstergrenze,
  type Fenstervertrag,
} from "@/lib/format";
import type { ProblemFehler } from "@/lib/http";

const GRENZEN = ["von", "bis"] as const satisfies readonly Fenstergrenze[];

/** Was Datum und Stundenwahl eines Zeitpunkts führen, und woher es kam. */
type Feldstand = {
  /** Datum und Uhrzeit, wie sie in den Feldern stehen. */
  entwurf: DatumUndUhrzeit;
  /** Der Zeitpunkt, den der Entwurf ergibt — als Zahl, `null` ohne Datum. */
  quelle: number | null;
  /** Der Wert von außen, den die Felder zuletzt gesehen haben. */
  gesehen: number | null;
  /** Steigt, wenn ein Wert von außen kommt: Das Datumsfeld hängt sich dann neu ein. */
  fassung: number;
  /** Das Datum ist halb getippt — der Zustand, den nur `validity.badInput` herausgibt. */
  halb: boolean;
};

type Stand = { zone: string; von: Feldstand; bis: Feldstand };

const zahl = (wert: Date | null): number | null => (wert === null ? null : wert.getTime());

/**
 * **Das freie Zeitfenster als Felder — ein Baustein für alle drei Stellen**
 * *(17.11.2026, E‑1198, `docs/nachrichtenliste.md` §8.2)*: das freie Fenster
 * der Nachrichtenliste, das Zeitfenster der Belegsuche und das freie Fenster der
 * Prozessansicht.
 *
 * ## Je Zeitpunkt ein Datum und eine Stundenwahl
 *
 * Bis dahin stand an allen drei Stellen ein `datetime-local`. Das liefert erst
 * einen Wert, wenn Datum **und** Uhrzeit vollständig sind; wer nur ein Datum
 * eintrug, bekam eine Meldung statt eines Ergebnisses (gemeldet vom
 * Auftraggeber am 17.11.2026). **Jetzt genügt das Datum.** Ohne Stunde beginnt
 * „Von“ um 00:00 und endet „Bis“ am Ende des Tages **im Vertrag des
 * Einhängepunkts** (`vertrag`, `lib/format.ts`): 23:59:59 in Liste und Suche,
 * 23:00 in der Prozessansicht, deren `bis` die letzte enthaltene Stunde ist
 * (E‑94).
 *
 * **Seit E‑1203 (am selben Tag) überall die Stundenwahl, keine Minuten.** Liste
 * und Suche trugen zuerst ein `input type="time"`, dessen aufgeklappte Fläche
 * der Browser in seinen eigenen Farben zeichnet (Punkt 180); der Auftraggeber
 * wollte alle drei Stellen wie die Prozessansicht. „Von“ nennt die volle
 * Stunde, „Bis“ ihr Ende; im Sekundenvertrag ist „Bis 14:59“ 14:59:59 (E‑1204,
 * `stundeneintraege`).
 *
 * | | Liste, Suche (`untereinander`) | Prozessansicht (`in-der-zeile`) |
 * |---|---|---|
 * | Datum | `input type="date"` | `input type="date"` |
 * | Stunde | Stundenwahl aus `components/auswahl-feld.tsx` | dieselbe |
 * | Beschriftung | über den Feldern | links daneben, in der Zeile des Umschalters |
 * | Meldung | unter den Feldern | hinter den Feldern, in derselben Zeile |
 *
 * ## Die Felder führen einen Entwurf — und die Adresse gewinnt, wenn sie sich ändert
 *
 * Datum und Stunde stehen im **Komponentenzustand**, nicht nur in der Adresse:
 * Eine Stunde ohne Datum ist noch kein Zeitpunkt, und eine Wahl, die ihren Wert
 * nur aus der Adresse läse, verlöre sie beim nächsten Rendern. In die Adresse
 * geht ein Zeitpunkt, sobald das Datum steht.
 *
 * **Ändert sich der Wert von außen** — ein Link, das Zurück des Browsers, „Auf
 * ein Jahr erweitern“, „Vorgabe wiederherstellen“ —, übernehmen die Felder ihn,
 * und das Datumsfeld hängt sich neu ein (`fassung`): Ein halb getipptes Datum
 * leert sich nur so, denn `value` ist dabei schon leer, und React setzte es
 * nicht noch einmal. Kommt dagegen der Wert zurück, den die Felder selbst
 * geschrieben haben, bleibt der Entwurf, wie er ist.
 *
 * **Die Stunde bleibt leer („Ganzer Tag“) genau dann, wenn sie der Vorgabe
 * entspricht** — aus der Adresse gelesen und nach jeder Eingabe. Wer „Von
 * 00:00“ oder bei „Bis“ die letzte Stunde wählt, sieht danach „Ganzer Tag“: Es
 * heißt dasselbe. Ein Wert, den die Stundenwahl nicht kennt — ein alter Link mit
 * Minuten —, steht, wie er ist (Regel Q4).
 *
 * ## Eine halbe Eingabe ist keine Auswahl (E‑1202)
 *
 * Ein halb getipptes Datum liefert `value === ""` und feuert kein `input`;
 * herausgegeben wird der Zustand allein über `validity.badInput`, gelesen an
 * `keyup` und `blur` — der Befund vom 07.08.2026, übernommen und nicht neu
 * gefunden. **Die Adresse bleibt dabei, wie sie war**; die Meldung steht am
 * Feld.
 *
 * ## Wo die Meldungen stehen, in dieser Reihenfolge
 *
 * 1. Ein halbes oder fehlendes Datum (auch: Stunde ohne Datum).
 * 2. Die Antwort des Servers, falls sie an die Felder gehört (`fehler`) — die
 *    Prüfung bleibt im Backend; das Frontend rechnet keine Jahresgrenze nach.
 * 3. Erst einer der beiden Zeitpunkte steht.
 *
 * **Kein Hinweistext ohne Anlass** (`docs/process-view.md` §45, E‑1199): Was
 * „ohne Stunde“ heißt, sagt die Stundenwahl selbst („Ganzer Tag“, „Bis“ mit dem
 * Ende der Stunde).
 */
export function Zeitfensterfelder({
  von,
  bis,
  aufAenderung,
  fehler,
  vertrag,
  anordnung,
}: {
  von: Date | null;
  bis: Date | null;
  aufAenderung: (von: Date | null, bis: Date | null) => void;
  /** Die Antwort des Backends, falls sie an die Felder gehört. */
  fehler?: ProblemFehler;
  /** Wie der Einhängepunkt sein `bis` liest — und damit, wo der ganze Tag endet. */
  vertrag: Fenstervertrag;
  /** Liste und Suche untereinander, die Prozessansicht in der Zeile ihres Umschalters. */
  anordnung: "untereinander" | "in-der-zeile";
}) {
  const texte = useTexte();
  const zone = useAnzeigezone();
  const kennung = useId();
  const hinweisId = `${kennung}-hinweis`;
  const werte: Record<Fenstergrenze, Date | null> = { von, bis };

  function vonAussen(grenze: Fenstergrenze, fassung: number): Feldstand {
    const wert = werte[grenze];
    return {
      entwurf: datumUndUhrzeitFuerEingabe(wert, grenze, vertrag, zone),
      quelle: zahl(wert),
      gesehen: zahl(wert),
      fassung,
      halb: false,
    };
  }

  const [stand, setStand] = useState<Stand>(() => ({
    zone,
    von: vonAussen("von", 0),
    bis: vonAussen("bis", 0),
  }));

  /*
   * Ein Wert von außen gewinnt — angepasst **während des Renderns** und nicht in
   * einem Effekt (dasselbe Muster wie am Suchfeld der Liste): Ein Effekt zeigte
   * für einen Durchgang den alten Entwurf. Eine neue Anzeigezone liest beide
   * Zeitpunkte neu, denn die Wanduhrzeit hängt an ihr.
   */
  let aktuell = stand;
  for (const grenze of GRENZEN) {
    const feld = aktuell[grenze];
    const wert = zahl(werte[grenze]);
    if (aktuell.zone !== zone) {
      aktuell = { ...aktuell, [grenze]: vonAussen(grenze, feld.fassung + 1) };
    } else if (wert !== feld.gesehen) {
      aktuell = {
        ...aktuell,
        [grenze]:
          wert === feld.quelle ? { ...feld, gesehen: wert } : vonAussen(grenze, feld.fassung + 1),
      };
    }
  }
  if (aktuell.zone !== zone) {
    aktuell = { ...aktuell, zone };
  }
  if (aktuell !== stand) {
    setStand(aktuell);
  }

  /**
   * Übernimmt einen neuen Entwurf und schreibt den Zeitpunkt in die Adresse,
   * sobald das Datum steht. Steht er, wird die Stunde so geschrieben, wie die
   * Adresse sie zurückgäbe — leer, wenn sie die Vorgabe ist.
   */
  function uebernimm(grenze: Fenstergrenze, entwurf: DatumUndUhrzeit) {
    const zeitpunkt =
      entwurf.datum === ""
        ? null
        : zeitpunktAusDatumUndUhrzeit(entwurf.datum, entwurf.uhrzeit, grenze, vertrag, zone);
    const geglaettet =
      zeitpunkt === null ? entwurf : datumUndUhrzeitFuerEingabe(zeitpunkt, grenze, vertrag, zone);
    const quelle = zahl(zeitpunkt);
    setStand((bisher) => ({
      ...bisher,
      [grenze]: { ...bisher[grenze], entwurf: geglaettet, quelle, halb: false },
    }));
    if (quelle !== zahl(werte[grenze])) {
      aufAenderung(grenze === "von" ? zeitpunkt : von, grenze === "bis" ? zeitpunkt : bis);
    }
  }

  /**
   * Was das Datumsfeld gerade herausgibt — an `change`, `keyup` und `blur`.
   * `keyup` und `blur` fangen auch den Weg von halb zu leer, der kein `input`
   * auslöst. Eine halbe Eingabe ändert die Adresse nicht (E‑1202).
   */
  function uebernimmDatum(grenze: Fenstergrenze, feld: HTMLInputElement) {
    const bisher = stand[grenze];
    const halb = feld.validity.badInput;
    if (halb || feld.value === bisher.entwurf.datum) {
      if (halb !== bisher.halb) {
        setStand((vorher) => ({ ...vorher, [grenze]: { ...vorher[grenze], halb } }));
      }
      return;
    }
    uebernimm(grenze, { ...bisher.entwurf, datum: feld.value });
  }

  function hinweistext(): string | undefined {
    const felder = GRENZEN.map((grenze) => aktuell[grenze]);
    if (
      felder.some((feld) => feld.halb || (feld.entwurf.datum === "" && feld.entwurf.uhrzeit !== ""))
    ) {
      return texte.zeitraum.datumUnvollstaendig;
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
  const beschriftung: Record<Fenstergrenze, string> = {
    von: texte.zeitraum.von,
    bis: texte.zeitraum.bis,
  };

  function datumsfeld(grenze: Fenstergrenze) {
    const feld = aktuell[grenze];
    return (
      // Wanduhrzeit ohne Zone; umgerechnet wird in der Anzeigezone — sonst wäre
      // das Fenster gegen die Daten verschoben, sobald jemand nicht in der Zone
      // des Servers sitzt.
      <Input
        key={`datum-${feld.fassung}`}
        id={`${kennung}-${grenze}-datum`}
        type="date"
        className="h-bedienelement w-auto"
        aria-describedby={beschriebenDurch}
        value={feld.entwurf.datum}
        onChange={(ereignis) => uebernimmDatum(grenze, ereignis.currentTarget)}
        onKeyUp={(ereignis) => uebernimmDatum(grenze, ereignis.currentTarget)}
        onBlur={(ereignis) => uebernimmDatum(grenze, ereignis.currentTarget)}
      />
    );
  }

  function stundenwahl(grenze: Fenstergrenze) {
    const feld = aktuell[grenze];
    const id = `${kennung}-${grenze}-stunde`;
    const name = einsetzen(texte.zeitraum.stunde, { grenze: beschriftung[grenze] });
    const eintraege: Auswahleintrag<string>[] = [
      { wert: "", text: texte.zeitraum.ganzerTag },
      // „Von“ nennt die volle Stunde, „Bis“ ihr Ende; der Wert hängt am Vertrag
      // (E‑1204, `lib/format.ts`).
      ...stundeneintraege(grenze, vertrag),
    ];
    return (
      <>
        <Label htmlFor={id} className="sr-only">
          {name}
        </Label>
        {/* Breit genug für „Ganzer Tag“ samt Pfeil; die Höhe der Zeile, in der sie steht. */}
        <div className="w-32 shrink-0">
          <AuswahlFeld
            id={id}
            beschriftung={name}
            wert={feld.entwurf.uhrzeit}
            eintraege={eintraege}
            hoehe="bedienelement"
            aufWahl={(wert) => uebernimm(grenze, { ...stand[grenze].entwurf, uhrzeit: wert })}
          />
        </div>
      </>
    );
  }

  /* Dieselbe ruhige Farbrolle wie am Suchfeld der Liste: Der Nutzer hat nichts
     falsch gemacht, er ist nur noch nicht fertig. */
  const hinweisAbsatz =
    hinweis === undefined ? null : (
      <p
        id={hinweisId}
        // Bewusst ohne `cn()`: tailwind-merge hält `text-beiwerk` für eine
        // Textfarbe und würfe `text-muted-foreground` weg (Sichtprüfung vom
        // 17.11.2026).
        className={
          anordnung === "untereinander"
            ? "text-muted-foreground text-beiwerk max-w-prose"
            : "text-muted-foreground text-beiwerk"
        }
      >
        {hinweis}
      </p>
    );

  if (anordnung === "in-der-zeile") {
    /*
     * **Kinder der Zeile, kein eigener Rahmen** (`docs/process-view.md` §45):
     * Beschriftung und Felder nebeneinander in Bedienelementhöhe — beim Klick
     * auf „Frei“ wird die Zeile breiter, nicht höher, und der Umschalter bleibt
     * stehen.
     */
    return (
      <>
        {GRENZEN.map((grenze) => (
          <div key={grenze} className="flex items-center gap-1.5">
            <Label htmlFor={`${kennung}-${grenze}-datum`} className="text-beiwerk">
              {beschriftung[grenze]}
            </Label>
            <div className="flex items-center gap-1">
              {datumsfeld(grenze)}
              {stundenwahl(grenze)}
            </div>
          </div>
        ))}
        {hinweisAbsatz}
      </>
    );
  }

  /*
   * Datum und Stunde eines Zeitpunkts stehen eng beieinander, die beiden
   * Zeitpunkte mit Abstand: Gelesen werden zwei Grenzen, nicht vier Felder. Am
   * schmalen Fenster bricht „Bis“ als Ganzes in die nächste Zeile.
   */
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex flex-wrap items-end gap-x-3 gap-y-2">
        {GRENZEN.map((grenze) => (
          <div key={grenze} className="flex flex-col gap-0.5">
            <Label htmlFor={`${kennung}-${grenze}-datum`} className="text-beiwerk">
              {beschriftung[grenze]}
            </Label>
            <div className="flex items-center gap-1">
              {datumsfeld(grenze)}
              {stundenwahl(grenze)}
            </div>
          </div>
        ))}
      </div>
      {hinweisAbsatz}
    </div>
  );
}
