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
  zeitpunktAusDatumUndUhrzeit,
  type DatumUndUhrzeit,
  type Fenstergrenze,
  type Fenstervertrag,
} from "@/lib/format";
import type { ProblemFehler } from "@/lib/http";

const GRENZEN = ["von", "bis"] as const satisfies readonly Fenstergrenze[];

/** Die 24 Stunden der Stundenwahl, zweistellig. */
const STUNDEN = Array.from({ length: 24 }, (_, stunde) => String(stunde).padStart(2, "0"));

type Teil = "datum" | "uhrzeit";

/** Was die beiden Felder eines Zeitpunkts führen, und woher es kam. */
type Feldstand = {
  /** Datum und Uhrzeit, wie sie in den Feldern stehen. */
  entwurf: DatumUndUhrzeit;
  /** Der Zeitpunkt, den der Entwurf ergibt — als Zahl, `null` ohne Datum. */
  quelle: number | null;
  /** Der Wert von außen, den die Felder zuletzt gesehen haben. */
  gesehen: number | null;
  /** Steigt, wenn ein Wert von außen kommt: Die Felder hängen sich dann neu ein. */
  fassung: number;
  /** Halb getippt — der Zustand, den nur `validity.badInput` herausgibt. */
  halb: Record<Teil, boolean>;
};

type Stand = { zone: string; von: Feldstand; bis: Feldstand };

const zahl = (wert: Date | null): number | null => (wert === null ? null : wert.getTime());

/**
 * **Das freie Zeitfenster als Felder — ein Baustein für alle drei Stellen**
 * *(17.11.2026, E‑1198, `docs/nachrichtenliste.md` §8.2)*: das freie Fenster
 * der Nachrichtenliste, das Zeitfenster der Belegsuche und das freie Fenster der
 * Prozessansicht.
 *
 * ## Je Zeitpunkt ein Datum und eine freiwillige Uhrzeit
 *
 * Bis dahin stand an allen drei Stellen ein `datetime-local`. Das liefert erst
 * einen Wert, wenn Datum **und** Uhrzeit vollständig sind; wer nur ein Datum
 * eintrug, bekam eine Meldung statt eines Ergebnisses (gemeldet vom
 * Auftraggeber am 17.11.2026). **Jetzt genügt das Datum.** Ohne Uhrzeit beginnt
 * „Von“ um 00:00 und endet „Bis“ am Ende des Tages **im Vertrag des
 * Einhängepunkts** (`vertrag`, `lib/format.ts`): 23:59:59 in Liste und Suche,
 * 23:00 in der Prozessansicht, deren `bis` die letzte enthaltene Stunde ist
 * (E‑94).
 *
 * | | Liste, Suche (`untereinander`) | Prozessansicht (`in-der-zeile`) |
 * |---|---|---|
 * | Datum | `input type="date"` | `input type="date"` |
 * | Uhrzeit | `input type="time"`, Minuten | Stundenwahl aus `components/auswahl-feld.tsx`, „Bis“ nennt das Ende der Stunde |
 * | Beschriftung | über den Feldern | links daneben, in der Zeile des Umschalters |
 * | Meldung | unter den Feldern | hinter den Feldern, in derselben Zeile |
 *
 * ## Die Felder führen einen Entwurf — und die Adresse gewinnt, wenn sie sich ändert
 *
 * Datum und Uhrzeit stehen im **Komponentenzustand**, nicht nur in der Adresse:
 * Eine Uhrzeit ohne Datum ist noch kein Zeitpunkt, und ein gesteuertes Feld, das
 * seinen Wert nur aus der Adresse läse, verlöre sie beim nächsten Rendern. In
 * die Adresse geht ein Zeitpunkt, sobald das Datum steht.
 *
 * **Ändert sich der Wert von außen** — ein Link, das Zurück des Browsers, „Auf
 * ein Jahr erweitern“, „Vorgabe wiederherstellen“ —, übernehmen die Felder ihn
 * und hängen sich neu ein (`fassung`): Ein halb getipptes Datum leert sich nur
 * so, denn `value` ist dabei schon leer, und React setzte es nicht noch einmal.
 * Kommt dagegen der Wert zurück, den die Felder selbst geschrieben haben, bleibt
 * der Entwurf, wie er ist — sonst sprängen die Felder beim Tippen.
 *
 * **Die Uhrzeit bleibt leer genau dann, wenn sie der Vorgabe entspricht.** Aus
 * der Adresse gelesen sowieso; nach einer Eingabe beim Verlassen des Felds und
 * nach einer Wahl in der Stundenwahl. Wer „Von 00:00“ tippt, sieht danach ein
 * leeres Feld — es heißt dasselbe.
 *
 * ## Eine halbe Eingabe ist keine Auswahl (E‑1202)
 *
 * Ein halb getipptes Datum oder eine halbe Uhrzeit liefert `value === ""` und
 * feuert kein `input`; herausgegeben wird der Zustand allein über
 * `validity.badInput`, gelesen an `keyup` und `blur` — der Befund vom
 * 07.08.2026, übernommen und nicht neu gefunden. **Die Adresse bleibt dabei,
 * wie sie war**; die Meldung steht am Feld. **Eine leere Uhrzeit ist keine halbe
 * Eingabe**, sondern der ganze Tag.
 *
 * ## Wo die Meldungen stehen, in dieser Reihenfolge
 *
 * 1. Ein halbes oder fehlendes Datum (auch: Uhrzeit ohne Datum).
 * 2. Eine halbe Uhrzeit.
 * 3. Die Antwort des Servers, falls sie an die Felder gehört (`fehler`) — die
 *    Prüfung bleibt im Backend; das Frontend rechnet keine Jahresgrenze nach.
 * 4. Erst einer der beiden Zeitpunkte steht.
 *
 * **Kein Hinweistext ohne Anlass** (`docs/process-view.md` §45, E‑1199): Was „ohne
 * Uhrzeit“ heißt, sagt die Stundenwahl selbst („Ganzer Tag“, „Bis“ mit dem Ende
 * der Stunde); das Uhrzeitfeld sagt es in seinem Namen.
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
      halb: { datum: false, uhrzeit: false },
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
   * Übernimmt, was ein Feld gerade herausgibt — an `change`, `keyup` und `blur`.
   * `keyup` und `blur` fangen auch den Weg von halb zu leer, der kein `input`
   * auslöst.
   */
  function uebernimm(grenze: Fenstergrenze, teil: Teil, wert: string, halbJetzt: boolean) {
    const feld = stand[grenze];
    const halb = { ...feld.halb, [teil]: halbJetzt };
    if (halbJetzt) {
      if (!feld.halb[teil]) {
        setStand((bisher) => ({ ...bisher, [grenze]: { ...bisher[grenze], halb } }));
      }
      return;
    }
    const entwurf = { ...feld.entwurf, [teil]: wert };
    if (entwurf.datum === feld.entwurf.datum && entwurf.uhrzeit === feld.entwurf.uhrzeit) {
      if (feld.halb[teil]) {
        setStand((bisher) => ({ ...bisher, [grenze]: { ...bisher[grenze], halb } }));
      }
      return;
    }
    const zeitpunkt =
      entwurf.datum === ""
        ? null
        : zeitpunktAusDatumUndUhrzeit(entwurf.datum, entwurf.uhrzeit, grenze, vertrag, zone);
    const quelle = zahl(zeitpunkt);
    setStand((bisher) => ({ ...bisher, [grenze]: { ...bisher[grenze], entwurf, quelle, halb } }));
    // Steht der andere Teil desselben Zeitpunkts halb da, wartet die Adresse auf ihn.
    const anderesHalb = teil === "datum" ? halb.uhrzeit : halb.datum;
    if (!anderesHalb && quelle !== zahl(werte[grenze])) {
      aufAenderung(grenze === "von" ? zeitpunkt : von, grenze === "bis" ? zeitpunkt : bis);
    }
  }

  /** Schreibt die Uhrzeit so, wie die Adresse sie zurückgäbe — leer, wenn sie die Vorgabe ist. */
  function glaette(grenze: Fenstergrenze) {
    setStand((bisher) => {
      const feld = bisher[grenze];
      if (feld.halb.datum || feld.halb.uhrzeit || feld.quelle === null) {
        return bisher;
      }
      const entwurf = datumUndUhrzeitFuerEingabe(new Date(feld.quelle), grenze, vertrag, zone);
      return entwurf.datum === feld.entwurf.datum && entwurf.uhrzeit === feld.entwurf.uhrzeit
        ? bisher
        : { ...bisher, [grenze]: { ...feld, entwurf } };
    });
  }

  function hinweistext(): string | undefined {
    const felder = GRENZEN.map((grenze) => aktuell[grenze]);
    if (
      felder.some(
        (feld) => feld.halb.datum || (feld.entwurf.datum === "" && feld.entwurf.uhrzeit !== ""),
      )
    ) {
      return texte.zeitraum.datumUnvollstaendig;
    }
    if (felder.some((feld) => feld.halb.uhrzeit)) {
      return texte.zeitraum.uhrzeitUnvollstaendig;
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

  function stundeneintraege(grenze: Fenstergrenze): Auswahleintrag<string>[] {
    return [
      { wert: "", text: texte.zeitraum.ganzerTag },
      // „Bis“ ist die letzte enthaltene Stunde (E‑94) — genannt wird ihr Ende,
      // gespeichert die volle Stunde.
      ...STUNDEN.map((stunde) => ({
        wert: `${stunde}:00`,
        text: grenze === "bis" ? `${stunde}:59` : `${stunde}:00`,
      })),
    ];
  }

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
        onChange={(ereignis) =>
          uebernimm(
            grenze,
            "datum",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          )
        }
        onKeyUp={(ereignis) =>
          uebernimm(
            grenze,
            "datum",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          )
        }
        onBlur={(ereignis) => {
          uebernimm(
            grenze,
            "datum",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          );
          glaette(grenze);
        }}
      />
    );
  }

  function uhrzeitfeld(grenze: Fenstergrenze) {
    const feld = aktuell[grenze];
    return (
      <Input
        key={`uhrzeit-${feld.fassung}`}
        id={`${kennung}-${grenze}-uhrzeit`}
        type="time"
        aria-label={einsetzen(texte.zeitraum.uhrzeit, { grenze: beschriftung[grenze] })}
        className="h-bedienelement w-auto"
        aria-describedby={beschriebenDurch}
        value={feld.entwurf.uhrzeit}
        onChange={(ereignis) =>
          uebernimm(
            grenze,
            "uhrzeit",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          )
        }
        onKeyUp={(ereignis) =>
          uebernimm(
            grenze,
            "uhrzeit",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          )
        }
        onBlur={(ereignis) => {
          uebernimm(
            grenze,
            "uhrzeit",
            ereignis.currentTarget.value,
            ereignis.currentTarget.validity.badInput,
          );
          glaette(grenze);
        }}
      />
    );
  }

  function stundenwahl(grenze: Fenstergrenze) {
    const feld = aktuell[grenze];
    const id = `${kennung}-${grenze}-stunde`;
    const name = einsetzen(texte.zeitraum.stunde, { grenze: beschriftung[grenze] });
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
            eintraege={stundeneintraege(grenze)}
            hoehe="bedienelement"
            aufWahl={(wert) => {
              uebernimm(grenze, "uhrzeit", wert, false);
              glaette(grenze);
            }}
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
   * Datum und Uhrzeit eines Zeitpunkts stehen eng beieinander, die beiden
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
              {uhrzeitfeld(grenze)}
            </div>
          </div>
        ))}
      </div>
      {hinweisAbsatz}
    </div>
  );
}
