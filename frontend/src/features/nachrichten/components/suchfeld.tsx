"use client";

import { useId, useState, type ReactNode } from "react";
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
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { ROUTEN } from "@/lib/routen";
import { cn } from "@/lib/utils";

import type { Suchfelder } from "../api";
import { useSuchfelder } from "../hooks";
import {
  HOECHSTENS_BEGRIFFE,
  SUCHE_PARAMETER,
  alsFeldParameter,
  alsParameter,
  alsZustand,
  ergaenze,
  markenAus,
  type Suchmarke,
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
 * ## Eine Fläche, zwei Quellen (E‑99) — als zwei Untermenüs (E‑112)
 *
 * Seit Teil 2 der Property-Suche zeigt die Auswahl neben dem Feld **zwei
 * Gruppen** aus `GET /api/bam/suchfelder`: die *Belegarten* des Mandanten und
 * die *Technischen Eigenschaften* — technische Namen, unverändert (E‑105). Wer
 * `Message.SNDPRN` nicht versteht, sieht `Message.SNDPRN`; es gibt keine
 * Beschriftung, keine Übersetzung, keinen Erklärtext daneben. Die Ordnung
 * innerhalb der Gruppen ist die des Endpunkts, hier wird nicht nachsortiert.
 *
 * **Seit dem 09.09.2026 ist jede Gruppe ein Untermenü.** Als flache Liste
 * hatte das Menü für `NEXANS` 55 Einträge, und die zweite Gruppe stand
 * unterhalb des Sichtbereichs — sie wurde nicht gefunden. Die oberste Ebene
 * trägt jetzt drei Einträge: den typlosen und je einen Auslöser pro gefüllter
 * Gruppe; die Einträge selbst stehen dahinter (`Auswahlmenue`).
 *
 * **Der typlose Eintrag bleibt und behält seinen Platz:** Ein Wert ohne Auswahl
 * sucht Belegnummern unter jedem Typ — und **er erreicht nie ein Feld** (E‑100).
 * Ein Feld muss gewählt sein, bevor eine Feld-Marke entsteht; die beiden
 * Problemtypen `feldname-fehlt` und `feldbegriff-ohne-trenner` kann diese
 * Oberfläche deshalb nicht erzeugen. Er steht **außerhalb** beider Untermenüs,
 * auch außerhalb des Untermenüs „Belegarten" — das ist bekannt und so gewollt.
 *
 * **Die Auswahl erscheint, sobald eine der beiden Gruppen etwas enthält.** Bis
 * Teil 2 erschien sie gar nicht, wenn der Mandant keine Belegart konfiguriert
 * hatte (`docs/bam-suche.md` §10) — für die Feldgruppe gilt das nicht, sie ist
 * für keinen Mandanten leer (die acht Typ‑0‑Einträge sind global, M154). Eine
 * leere Gruppe wird **weggelassen**: kein Untermenü, kein leerer Auslöser.
 *
 * ## Gesucht wird auf Eingabe, nicht beim Tippen
 *
 * Keine Entprellung, kein Vorschlagsmenü, keine Suche je Zeichen. Der Zugriff
 * über den **Wert** ist der teuerste Pfad dieses Projekts: M35 misst für den
 * schlimmsten Wert 8,66 Sekunden beim Jahresfenster, auf einer *ruhenden*
 * Testkopie — und die Property-Suche liest über den Wertindex bis zu 5,4 s
 * über ein Jahr (`docs/property-suche.md` §6.3). Ein Feld, das bei jedem
 * Zeichen sucht, feuert das mehrfach ab.
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
 * (`docs/nachrichtenliste.md` §8.2). **Ist eine Eigenschaft gewählt, sagt der
 * Platzhalter „Wert suchen"** — „Belegnummer" wäre dann eine falsche Auskunft.
 *
 * ## Woher es weiß, was schon gesucht wird
 *
 * Aus der URL, gelesen über dieselben Parser wie die Trefferansicht — **und
 * geschrieben über den Router**, nicht über sie. Der Unterschied ist der Grund:
 * Ein Filterzustand ändert die aktuelle Seite, dieses Feld führt auf eine
 * *andere* (`/suche`). Auf jeder anderen Seite ist die Liste der Marken leer,
 * und das Feld beginnt eine neue Suche.
 *
 * **Die gewählte Belegart oder die gewählte Eigenschaft und die begonnene
 * Eingabe stehen nicht in der URL.** Sie beschreiben keinen Ausschnitt, sondern
 * eine begonnene Eingabe — dieselbe Prüfung wie beim halb ausgefüllten freien
 * Zeitfenster der Liste (`docs/frontend-grundlagen.md` §8).
 */
export function Suchfeld() {
  const texte = useTexte();
  const router = useRouter();
  const pfad = usePathname();
  const { meldeDoppelt } = useSuchsignal();
  const feldId = useId();

  const [eingabe, setEingabe] = useState("");
  const [auswahl, setAuswahl] = useState<Auswahl>(null);

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
  const marken = aufSuche ? markenAus(zustand as Suchzustand) : [];

  const angebot = useSuchfelder().data ?? LEERES_ANGEBOT;
  // Nur, was das Angebot dieses Mandanten kennt, ist wirksam: Nach einem
  // Mandantenwechsel kann die Auswahl auf einen Typ oder ein Feld zeigen, das es
  // hier nicht gibt — dann gilt „keine Auswahl", sichtbar und beim Abschicken.
  const wirksam = imAngebot(auswahl, angebot);

  const voll = marken.length >= HOECHSTENS_BEGRIFFE;
  const leer = eingabe.trim() === "";

  function suche(ereignis: React.FormEvent) {
    ereignis.preventDefault();
    if (leer || voll) {
      return;
    }
    const wert = eingabe.trim();
    // Ein Feld muss gewählt sein, bevor eine Feld-Marke entsteht (E-100); ohne
    // Auswahl ist es eine Belegnummer unter jedem Typ.
    const neue: Suchmarke =
      wirksam?.art === "feld"
        ? { art: "feld", feld: { name: wirksam.name, wert } }
        : { art: "bam", begriff: { typ: wirksam?.typ ?? null, wert } };
    const ergaenzung = ergaenze(marken, neue);

    if (ergaenzung.doppelt !== null) {
      // Es entsteht keine zweite Marke — die vorhandene meldet sich stattdessen.
      // Die Eingabe bleibt stehen: Wer sie versehentlich wiederholt hat, will sie
      // wahrscheinlich ändern und nicht neu tippen.
      meldeDoppelt(ergaenzung.doppelt);
      return;
    }

    setEingabe("");
    if (aufSuche) {
      // Eine Marke mehr ist ein Filter und keine Station — `history: "replace"`
      // steht am Hook. Das Zeitfenster und die geöffnete Nachricht bleiben
      // unberührt.
      //
      // **Der Modus fällt dagegen auf `exakt` zurück, und zwar hier wie beim
      // Entfernen einer Marke** (`hooks.ts` `setzeMarken`): Eine Marke mehr
      // ist eine **neue Frage**, und die wird zuerst genau beantwortet. Der
      // Anlass für die Präfixsuche — das leere Ergebnis — gilt dann nicht mehr,
      // und sie ist die teuerste Zugriffsform dieses Projekts (M50).
      void setzeZustand({ ...alsZustand(ergaenzung.marken), modus: null });
    } else {
      // Von anderswo ist es eine Station: Dorthin will man mit Zurück zurück.
      router.push(suchziel(ergaenzung.marken));
    }
  }

  return (
    <form onSubmit={suche} className="flex w-full min-w-0 items-center gap-1" role="search">
      {angebot.bam.length > 0 || angebot.felder.length > 0 ? (
        <Auswahlmenue angebot={angebot} gewaehlt={wirksam} aufWahl={setAuswahl} />
      ) : null}

      {/* Die Beschriftung benennt, **worin** gesucht wird — sichtbar tut das der
          Platzhalter, für Vorleseprogramme dieses Label. */}
      <Label htmlFor={feldId} className="sr-only">
        {wirksam?.art === "feld"
          ? einsetzen(texte.suche.typwahl.gewaehltesFeld, { feld: wirksam.name })
          : texte.suche.feld.bezeichnung}
      </Label>
      <Input
        id={feldId}
        type="search"
        enterKeyHint="search"
        inputMode="text"
        value={eingabe}
        onChange={(ereignis) => setEingabe(ereignis.target.value)}
        placeholder={
          wirksam?.art === "feld" ? texte.suche.feld.platzhalterFeld : texte.suche.feld.platzhalter
        }
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
 * Was neben dem Feld gewählt ist: eine Belegart, ein Feld — oder nichts.
 *
 * **Komponentenzustand und nicht URL**: Es ist eine begonnene Eingabe und kein
 * Ausschnitt (`docs/frontend-grundlagen.md` §8). Abgelegt wird sie erst als Teil
 * einer Marke.
 */
type Auswahl = { art: "bam"; typ: number } | { art: "feld"; name: string } | null;

const LEERES_ANGEBOT: Suchfelder = { bam: [], felder: [] };

/** Die Auswahl, sofern das Angebot dieses Mandanten sie kennt — sonst „keine". */
function imAngebot(auswahl: Auswahl, angebot: Suchfelder): Auswahl {
  if (auswahl === null) {
    return null;
  }
  if (auswahl.art === "bam") {
    return angebot.bam.some((eintrag) => eintrag.typ === auswahl.typ) ? auswahl : null;
  }
  return angebot.felder.some((eintrag) => eintrag.name === auswahl.name) ? auswahl : null;
}

/** Der Wert eines Eintrags im Auswahlmenü — die Art voran, damit `9012` als Typ und als Name nie zusammenfallen. */
function auswahlwert(auswahl: Auswahl): string {
  if (auswahl === null) {
    return "";
  }
  return auswahl.art === "bam" ? `bam:${auswahl.typ}` : `feld:${auswahl.name}`;
}

function ausAuswahlwert(wert: string): Auswahl {
  if (wert.startsWith("bam:")) {
    return { art: "bam", typ: Number(wert.slice(4)) };
  }
  if (wert.startsWith("feld:")) {
    return { art: "feld", name: wert.slice(5) };
  }
  return null;
}

/**
 * Die Auswahl zum Begriff — **Belegart oder Eigenschaft, Verfeinerung oder
 * Weiche.**
 *
 * Eine Belegart ist Verfeinerung und keine Pflicht: M36 weist die Typangabe mit
 * +1,5 bis +4 Prozent aus, also im Rauschen — sie beschleunigt nicht. Typlos
 * kostet auch kaum etwas: `NEXANS` trägt zwar zehn kuratierte Zeilen, aber nur
 * **drei verschiedene** Sollängen, und aufgefüllt wird nur nach oben (M47).
 *
 * Eine Eigenschaft dagegen ist eine **Weiche**: Mit ihr sucht der Wert eine
 * Spalte oder eine Eigenschaft und nie eine Belegnummer, ohne sie nie eine
 * (E‑100). Beide stehen in einem Menü, weil es **eine** Suchfläche ist (E‑99) —
 * als zwei Gruppen, damit niemand eine Eigenschaft für eine Belegart hält.
 *
 * ## Zwei Untermenüs statt einer Liste (E‑112, 09.09.2026)
 *
 * ```
 * Alle Belegarten            ✓
 * ──────────────────────────
 * Belegarten                 ▸   → alle Belegarten des Mandanten
 * Technische Eigenschaften   ▸   → alle Feldnamen des Mandanten
 * ```
 *
 * Die flache Liste mit Überschriften hatte für `NEXANS` 55 Einträge; die zweite
 * Gruppe stand unter dem Sichtbereich und wurde nicht gefunden. Jetzt gilt:
 *
 * - **Genau eine Auswahl über alles hinweg**, wie bisher — eine `RadioGroup`
 *   umschließt den typlosen Eintrag und beide Untermenüs; die Wahl im einen
 *   hebt die im anderen auf, und das Häkchen steht beim gewählten Eintrag im
 *   Untermenü.
 * - **Der Auslöser der Gruppe, in der die Auswahl liegt, trägt den gewählten
 *   Eintrag als gedämpften Zusatztext** hinter der Beschriftung — damit sichtbar
 *   bleibt, wo die Auswahl steckt, ohne das Untermenü zu öffnen. Er trägt
 *   **kein `aria-checked`**: Er ist ein `menuitem` mit `aria-haspopup` und darf
 *   nicht zugleich Radioeintrag sein. Der Zusatztext ist Inhalt des Auslösers
 *   und damit Teil seines zugänglichen Namens (`Untermenue`).
 * - **Eine leere Gruppe bekommt keinen Auslöser**, nicht einen leeren; die
 *   Untermenüs entstehen auch dann, wenn nur eine Gruppe gefüllt ist — das
 *   Bedienmuster hängt nicht an der Konfiguration des Mandanten.
 * - Ordnung, Laufweite und Inhalte der Einträge sind unverändert; Tastatur und
 *   Berührung kommen aus Radix (`ArrowRight`/`Enter` öffnen, `ArrowLeft`
 *   schließt, Zeiger öffnet beim Überfahren), nichts davon ist nachgebaut.
 *
 * **Die Beschriftungen bleiben ungekürzt.** M45 hat das Kürzen der Endungen
 * ausgeschlossen: Ohne sie fallen 62 Beschreibungen auf 57, und zwei
 * `Abladestelle`-Typen stünden untereinander mit identischer Überschrift. Im
 * Untermenü ist Platz dafür; am Schalter kürzt der Name und steht vollständig
 * im `title`. Für die Feldnamen gilt dasselbe: unverändert, technisch (E‑105).
 *
 * **„Technische Eigenschaften" ist die Beschriftung des Blocks im
 * Nachrichtendetail** (E‑113) — sie benennt eine Art und keinen Speicherort:
 * Acht der Einträge sind Spalten (Typ 0), keine `MessageProperty`-Zeilen, und
 * erscheinen in jenem Block nie.
 */
function Auswahlmenue({
  angebot,
  gewaehlt,
  aufWahl,
}: {
  angebot: Suchfelder;
  gewaehlt: Auswahl;
  aufWahl: (auswahl: Auswahl) => void;
}) {
  const texte = useTexte();

  const gewaehlteBelegart =
    gewaehlt?.art === "bam"
      ? (angebot.bam.find((eintrag) => eintrag.typ === gewaehlt.typ)?.bezeichnung ??
        String(gewaehlt.typ))
      : null;
  const gewaehltesFeld = gewaehlt?.art === "feld" ? gewaehlt.name : null;
  const kurz = gewaehlteBelegart ?? gewaehltesFeld;
  const beschriftung =
    gewaehlt === null
      ? texte.suche.typwahl.alle
      : gewaehlt.art === "bam"
        ? einsetzen(texte.suche.typwahl.gewaehlt, { belegart: gewaehlteBelegart ?? "" })
        : einsetzen(texte.suche.typwahl.gewaehltesFeld, { feld: gewaehlt.name });

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
          {kurz === null ? null : <span className="text-beiwerk truncate">{kurz}</span>}
        </Button>
      </DropdownMenuTrigger>
      {/* Drei Einträge auf dieser Ebene — die Breite braucht der Zusatztext am
          Auslöser, nicht die Zahl der Einträge. */}
      <DropdownMenuContent align="start" className="w-80">
        <DropdownMenuRadioGroup
          value={auswahlwert(gewaehlt)}
          onValueChange={(wert) => aufWahl(ausAuswahlwert(wert))}
        >
          {/* Der typlose Eintrag zuerst und außerhalb beider Untermenüs: Er ist
              die Vorgabe, und seine Beschriftung sagt, was er tut — Belegnummern
              unter jeder Belegart, nie ein Feld. */}
          <DropdownMenuRadioItem value="">{texte.suche.typwahl.alle}</DropdownMenuRadioItem>
          <DropdownMenuSeparator />

          {/* Eine leere Gruppe wird weggelassen — kein Untermenü, kein leerer Auslöser. */}
          {angebot.bam.length > 0 ? (
            <Untermenue
              beschriftung={texte.suche.typwahl.gruppeBelegarten}
              gewaehlt={gewaehlteBelegart}
            >
              {angebot.bam.map((eintrag) => (
                <DropdownMenuRadioItem
                  key={`bam:${eintrag.typ}`}
                  value={auswahlwert({ art: "bam", typ: eintrag.typ })}
                >
                  {eintrag.bezeichnung}
                </DropdownMenuRadioItem>
              ))}
            </Untermenue>
          ) : null}

          {angebot.felder.length > 0 ? (
            <Untermenue
              beschriftung={texte.suche.typwahl.gruppeFelder}
              gewaehlt={gewaehltesFeld}
              festeLaufweite
            >
              {angebot.felder.map((eintrag) => (
                <DropdownMenuRadioItem
                  key={`feld:${eintrag.name}`}
                  value={auswahlwert({ art: "feld", name: eintrag.name })}
                  className="font-mono"
                >
                  {eintrag.name}
                </DropdownMenuRadioItem>
              ))}
            </Untermenue>
          ) : null}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/**
 * Ein Untermenü der Auswahl: der Auslöser mit der Beschriftung der Gruppe und —
 * liegt die Auswahl in dieser Gruppe — dem gewählten Eintrag als gedämpftem
 * Zusatztext dahinter; dahinter die Einträge, in ihrer Höhe an den Bildschirm
 * gebunden und darin scrollend (40 Belegarten bei `NEXANS`). **22 rem breit**, zwei
 * mehr als das Hauptmenü: Bei 20 rem brachen zwei der 35 Zeichen langen
 * Beschreibungen um, bei 22 rem stehen alle 40 einzeilig (Sichtprüfung
 * 09.09.2026, `docs/property-suche.md` §14).
 *
 * **Das Leerzeichen zwischen den beiden Spannen ist Absicht.** Im Flex-Layout
 * wird ein Textknoten aus reinem Leerraum nicht gezeichnet — den Abstand macht
 * `gap` —, im Text des Auslösers steht er aber, und damit auch im zugänglichen
 * Namen: Ein Vorleseprogramm liest „Belegarten Lieferschein-Nr._L_SAP" und
 * nicht ein zusammengezogenes Wort. Kein `aria-label` daneben, das den Namen
 * ein zweites Mal führte und dem Inhalt davonliefe.
 */
function Untermenue({
  beschriftung,
  gewaehlt,
  festeLaufweite = false,
  children,
}: {
  beschriftung: string;
  /** Der gewählte Eintrag dieser Gruppe — `null`, wenn die Auswahl nicht hier liegt. */
  gewaehlt: string | null;
  /** Feldnamen sind technische Namen und stehen in fester Laufweite (E‑105). */
  festeLaufweite?: boolean;
  children: ReactNode;
}) {
  return (
    <DropdownMenuSub>
      <DropdownMenuSubTrigger>
        <span className="shrink-0">{beschriftung}</span>
        {gewaehlt === null ? null : (
          <>
            {" "}
            <span
              className={cn("text-muted-foreground flex-1 truncate", festeLaufweite && "font-mono")}
            >
              {gewaehlt}
            </span>
          </>
        )}
      </DropdownMenuSubTrigger>
      <DropdownMenuSubContent className="max-h-(--radix-dropdown-menu-content-available-height) w-88 overflow-y-auto">
        {children}
      </DropdownMenuSubContent>
    </DropdownMenuSub>
  );
}

/**
 * Das Ziel einer **neuen** Suche: `/suche` mit den Marken als **wiederholten**
 * Parametern `begriff` und `feld` — und mit sonst nichts.
 *
 * **Kein Zeitfenster wird mitgenommen.** Wer auf `/nachrichten` ein freies
 * Fenster eingestellt hat und dann eine Belegnummer tippt, bekommt die Vorgabe
 * der Suche und nicht das Fenster der Liste: Die beiden Ansichten benutzen
 * dieselben Parameternamen für zwei verschiedene Fragen, und der Nutzer mit
 * einer Belegnummer hat kein Datum (`docs/bam-suche.md` §2).
 *
 * Auf `/suche` selbst wird diese Funktion **nicht** gebraucht — dort ändert
 * `nuqs` genau die beiden Schlüssel und lässt den Rest stehen.
 */
function suchziel(marken: Suchmarke[]): string {
  const parameter = new URLSearchParams();
  const { begriff, feld } = alsZustand(marken);
  for (const eintrag of begriff ?? []) {
    parameter.append("begriff", alsParameter(eintrag));
  }
  for (const eintrag of feld ?? []) {
    parameter.append("feld", alsFeldParameter(eintrag));
  }
  return `${ROUTEN.suche}?${parameter.toString()}`;
}
