"use client";

import { useId, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useQueryStates } from "nuqs";
import { Plus, Tag } from "lucide-react";

import { useSuchsignal } from "@/components/suchsignal";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";

import type { BamTyp } from "../api";
import { useBamTypen } from "../hooks";
import {
  HOECHSTENS_BEGRIFFE,
  SUCHE_PARAMETER,
  alsParameter,
  begriffeAus,
  ergaenze,
  type Suchbegriff,
  type Suchzustand,
} from "../suche";

/**
 * **Das Suchfeld in der Kopfzeile — der Einstiegspunkt des Werkzeugs.**
 *
 * ## Warum es hier steht und nicht auf einer Seite
 *
 * Der Platz ist seit Schritt 3 reserviert (`docs/visuelles-konzept.md` §5): ein
 * leerer Bereich fester Breite links neben dem Mandantenumschalter, mit dem
 * Vermerk „bis Schritt 7 ihn füllt". Die Suche ist nach dem Leitsatz der
 * Haupteinstieg — stünde ihr Platz nicht von Anfang an fest, drängte sie sich
 * später zwischen Mandant, Sprache und Nutzermenü.
 *
 * **Es gibt keinen Navigationseintrag dazu.** Das Feld steht auf jeder Seite; ein
 * Menüpunkt daneben wäre eine zweite Tür in denselben Raum.
 *
 * ## Gesucht wird auf Eingabe, nicht beim Tippen
 *
 * Keine Entprellung, kein Vorschlagsmenü, keine Suche je Zeichen. Der Zugriff
 * über den **Wert** ist der teuerste Pfad dieses Projekts: M35 misst für den
 * schlimmsten Wert 8,66 Sekunden beim Jahresfenster, auf einer *ruhenden*
 * Testkopie. Ein Feld, das bei jedem Zeichen sucht, feuert das mehrfach ab.
 *
 * **Und kein Aufklappmenü unter dem Feld.** Eine Trefferzeile trägt Zeitpunkt,
 * Status, Ablauf, Treffertyp und Kettenhinweis — das ist eine Tabellenzeile und
 * kein Vorschlagseintrag.
 *
 * ## Beide Suchfelder sagen, worin sie suchen
 *
 * `/nachrichten` hat ein zweites Feld, und es filtert Prozess-, Projekt- und
 * Ablaufnamen. Zwei Felder auf demselben Bildschirm, die verschiedene Dinge tun
 * und verschieden fehlschlagen, sind eine Falle — besonders für den Nutzer, der
 * kein EDI-Spezialist ist. Deshalb heißt dieses hier „Belegnummer suchen" und
 * jenes „Prozess, Projekt oder Ablauf durchsuchen"; die Beschriftung des
 * Listenfilters ist in Teil 3 dafür angepasst worden
 * (`docs/nachrichtenliste.md` §8.2).
 *
 * ## Woher es weiß, was schon gesucht wird
 *
 * Aus der URL, gelesen über dieselben Parser wie die Trefferansicht — **und
 * geschrieben über den Router**, nicht über sie. Der Unterschied ist der Grund:
 * Ein Filterzustand ändert die aktuelle Seite, dieses Feld führt auf eine
 * *andere* (`/suche`). Auf jeder anderen Seite ist die Liste der Begriffe leer,
 * und das Feld beginnt eine neue Suche.
 */
export function Suchfeld() {
  const texte = useTexte();
  const router = useRouter();
  const pfad = usePathname();
  const { meldeDoppelt } = useSuchsignal();
  const feldId = useId();

  const [eingabe, setEingabe] = useState("");
  const [typ, setTyp] = useState<number | null>(null);

  /*
   * **Zwei Wege, und der Unterschied ist nicht Geschmack, sondern ein Fehler,
   * den es gab.**
   *
   * Auf `/suche` schreibt das Feld über **`nuqs`** — denselben Weg, den die
   * Marken und das Zeitfenster nehmen. Ein `router.replace` daneben sah in der
   * URL richtig aus und kam in der Ansicht nicht an: `nuqs` führt seinen eigenen
   * Stand, und eine Navigation, die an ihm vorbeigeht, hebt ihn nicht auf.
   * Aufgefallen in der Sichtprüfung am 13.08.2026 — die URL trug zwei Begriffe,
   * die Marken zeigten einen.
   *
   * Von jeder anderen Seite aus **muss** es der Router sein: `nuqs` ändert die
   * Abfrage der aktuellen Route und kann nicht auf eine andere führen.
   */
  const [zustand, setzeZustand] = useQueryStates(SUCHE_PARAMETER, { history: "replace" });
  const aufSuche = pfad === ROUTEN.suche;
  const begriffe = aufSuche ? begriffeAus(zustand as Suchzustand) : [];

  const typenAnfrage = useBamTypen();
  const typen = typenAnfrage.data ?? [];
  const gewaehlt = typen.find((eintrag) => eintrag.typ === typ) ?? null;

  const voll = begriffe.length >= HOECHSTENS_BEGRIFFE;
  const leer = eingabe.trim() === "";

  function suche(ereignis: React.FormEvent) {
    ereignis.preventDefault();
    if (leer || voll) {
      return;
    }
    const neuer: Suchbegriff = { typ, wert: eingabe.trim() };
    const ergaenzung = ergaenze(begriffe, neuer);

    if (ergaenzung.doppelt !== null) {
      // Es entsteht keine zweite Marke — die vorhandene meldet sich stattdessen.
      // Die Eingabe bleibt stehen: Wer sie versehentlich wiederholt hat, will sie
      // wahrscheinlich ändern und nicht neu tippen.
      meldeDoppelt(ergaenzung.doppelt);
      return;
    }

    setEingabe("");
    if (aufSuche) {
      // Ein Begriff mehr ist ein Filter und keine Station — `history: "replace"`
      // steht am Hook. Das Zeitfenster und die geöffnete Nachricht bleiben
      // unberührt.
      //
      // **Der Modus fällt dagegen auf `exakt` zurück, und zwar hier wie beim
      // Entfernen einer Marke** (`hooks.ts` `setzeBegriffe`): Ein Begriff mehr
      // ist eine **neue Frage**, und die wird zuerst genau beantwortet. Der
      // Anlass für die Präfixsuche — das leere Ergebnis — gilt dann nicht mehr,
      // und sie ist die teuerste Zugriffsform dieses Projekts (M50).
      void setzeZustand({ begriff: ergaenzung.begriffe, modus: null });
    } else {
      // Von anderswo ist es eine Station: Dorthin will man mit Zurück zurück.
      router.push(suchziel(ergaenzung.begriffe));
    }
  }

  return (
    <form onSubmit={suche} className="flex w-full min-w-0 items-center gap-1" role="search">
      {typen.length > 0 ? <Typwahl typen={typen} gewaehlt={gewaehlt} aufWahl={setTyp} /> : null}

      {/* Die Beschriftung benennt, **worin** gesucht wird — sichtbar tut das der
          Platzhalter, für Vorleseprogramme dieses Label. */}
      <Label htmlFor={feldId} className="sr-only">
        {texte.suche.feld.bezeichnung}
      </Label>
      <Input
        id={feldId}
        type="search"
        enterKeyHint="search"
        inputMode="text"
        value={eingabe}
        onChange={(ereignis) => setEingabe(ereignis.target.value)}
        placeholder={texte.suche.feld.platzhalter}
        className="h-bedienelement min-w-0 flex-1"
      />
      <Button
        type="submit"
        variant="outline"
        size="icon"
        className="min-h-bedienelement shrink-0"
        disabled={leer || voll}
        // Der Grund steht am Knopf **und** über der Trefferliste. Wer hier an die
        // Grenze stößt, sieht die Marken ohnehin vor sich; ein Satz, der nur im
        // Tooltip stünde, erreichte den Touchscreen nie.
        title={
          voll
            ? einsetzen(texte.suche.marken.grenzeErreicht, { anzahl: HOECHSTENS_BEGRIFFE })
            : texte.suche.feld.hinzufuegen
        }
        aria-label={texte.suche.feld.hinzufuegen}
      >
        <Plus aria-hidden="true" />
      </Button>
    </form>
  );
}

/**
 * Die Belegart zum Begriff — **Verfeinerung, keine Pflicht.**
 *
 * Die Vorgabe ist **kein** Typ, und das ist gemessen: M36 weist die Typangabe mit
 * +1,5 bis +4 Prozent aus, also im Rauschen — sie beschleunigt nicht. Typlos
 * kostet auch kaum etwas: `NEXANS` trägt zwar zehn kuratierte Zeilen, aber nur
 * **drei verschiedene** Sollängen, und aufgefüllt wird nur nach oben (M47).
 *
 * **Ohne konfigurierte Typen erscheint die Auswahl gar nicht.** `EDITIONLINGERI`,
 * `SYSTEM` und `WOC` haben keinen (M40); dort gibt es keinen leeren Platzhalter
 * und keine leere Liste, sondern nichts — dieselbe Regel wie bei der leeren
 * Spalte in `docs/nachrichtenliste.md` §8.1.
 *
 * **Die Beschriftungen bleiben ungekürzt.** M45 hat das Kürzen der Endungen
 * ausgeschlossen: Ohne sie fallen 62 Beschreibungen auf 57, und zwei
 * `Abladestelle`-Typen stünden untereinander mit identischer Überschrift. Im
 * Auswahlmenü ist Platz dafür; am Schalter kürzt der Name und steht vollständig
 * im `title`.
 */
function Typwahl({
  typen,
  gewaehlt,
  aufWahl,
}: {
  typen: BamTyp[];
  gewaehlt: BamTyp | null;
  aufWahl: (typ: number | null) => void;
}) {
  const texte = useTexte();
  const beschriftung =
    gewaehlt === null
      ? texte.suche.typwahl.alle
      : einsetzen(texte.suche.typwahl.gewaehlt, { belegart: gewaehlt.bezeichnung });

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="outline"
          className="min-h-bedienelement max-w-28 shrink-0 gap-1 px-2"
          title={beschriftung}
          aria-label={beschriftung}
        >
          <Tag aria-hidden="true" className="shrink-0 opacity-70" />
          {gewaehlt === null ? null : (
            <span className="text-beiwerk truncate">{gewaehlt.bezeichnung}</span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="max-h-96 w-72 overflow-y-auto">
        <DropdownMenuRadioGroup
          value={gewaehlt === null ? "" : String(gewaehlt.typ)}
          onValueChange={(wert) => aufWahl(wert === "" ? null : Number(wert))}
        >
          <DropdownMenuRadioItem value="">{texte.suche.typwahl.alle}</DropdownMenuRadioItem>
          {typen.map((eintrag) => (
            <DropdownMenuRadioItem key={eintrag.typ} value={String(eintrag.typ)}>
              {eintrag.bezeichnung}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * Das Ziel einer **neuen** Suche: `/suche` mit den Begriffen als
 * **wiederholtem** Parameter — und mit sonst nichts.
 *
 * **Kein Zeitfenster wird mitgenommen.** Wer auf `/nachrichten` ein freies
 * Fenster eingestellt hat und dann eine Belegnummer tippt, bekommt die Vorgabe
 * der Suche und nicht das Fenster der Liste: Die beiden Ansichten benutzen
 * dieselben Parameternamen für zwei verschiedene Fragen, und der Nutzer mit
 * einer Belegnummer hat kein Datum (`docs/bam-suche.md` §2).
 *
 * Auf `/suche` selbst wird diese Funktion **nicht** gebraucht — dort ändert
 * `nuqs` genau einen Schlüssel und lässt den Rest stehen.
 */
function suchziel(begriffe: Suchbegriff[]): string {
  const parameter = new URLSearchParams();
  for (const begriff of begriffe) {
    parameter.append("begriff", alsParameter(begriff));
  }
  return `${ROUTEN.suche}?${parameter.toString()}`;
}
