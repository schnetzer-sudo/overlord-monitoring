"use client";

import { useEffect, useId, useRef, type Dispatch } from "react";
import {
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  CircleAlert,
  CircleCheck,
  CircleDashed,
  CircleHelp,
  Clock,
  PlayCircle,
  Trash2,
  type LucideIcon,
} from "lucide-react";

import { NeuLaden } from "@/components/neu-laden";
import { Button } from "@/components/ui/button";
import { useAnzeigezone } from "@/components/zeitzone";
import { Fehler } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import {
  formatiereRohzeitpunkt,
  formatiereZahl,
  formatiereZeitpunkt,
  formatiereZeitpunktGenau,
} from "@/lib/format";
import {
  auftragVordergrund,
  ergebnisFlaeche,
  ergebnisVordergrund,
  type Auftragseinordnung,
  type Zeilenergebnis,
} from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { Auftrag, Auftragseintrag } from "../api";
import {
  SICHTBARE_AUFTRAEGE,
  SICHTBARE_EINTRAEGE,
  beschrifteteWerte,
  dritteZeile,
  einordnungVon,
  ergebnisVon,
  istKurz,
  loeschbar,
  nichtErfolgreich,
  schluesselZugeklappt,
  sichtbare,
  weitere,
  type Blockschritt,
  type Blockzustand,
  type Zeilenmeldung,
} from "../auftraege";
import type { AuftraegeBlockZustand } from "../hooks";

/**
 * **„Ihre Aufträge“** — was aus den Aufträgen des Mandanten geworden ist
 * (Linie `assistent`, A4, `docs/assistent-frontend.md` §12).
 *
 * **Ob der Block steht, entscheidet nicht er**, sondern die Übersicht über
 * `blockSichtbar` (E‑1098, F18): ohne Auftrag kein Rahmen, beim ersten Laden
 * kein Platzhalter. Steht er, zeigt er entweder die Liste oder den Fehler.
 *
 * **Kein Stand wird hier abgeleitet** — die Einordnung kommt aus dem Backend.
 * Jeder Stand hat Zeichen **und** Wort (E‑1105); die Farbe kommt aus
 * `lib/status-farbe.ts` und ist die halbe Aussage, nie die ganze.
 *
 * **Reiner Text** (E‑1107): `fehlertext`, Rückgabe, Schlüssel und Werte stehen
 * als Textknoten. Was wie HTML aussieht, steht wörtlich da.
 *
 * **Gesiezt** (E‑1124) — der Block setzt fort, was im Gespräch mit dem
 * Assistenten entstanden ist.
 */
export function AuftraegeBlock({ block }: { block: AuftraegeBlockZustand }) {
  const texte = useTexte();
  const t = texte.dashboard.auftraege;
  const titelId = useId();
  const obenRef = useRef<HTMLParagraphElement>(null);
  const { abfrage, zustand, schritt } = block;

  // Der Fokus folgt dem Satz oben (F22): Der Knopf, der eben gedrückt wurde,
  // ist mit seiner Zeile verschwunden. Gezählt über `obenNummer`, damit auch
  // ein zweiter gleicher Satz den Fokus bekommt. Kein Zustand wird hier
  // gesetzt — nur der Fokus.
  useEffect(() => {
    if (zustand.obenNummer > 0) {
      obenRef.current?.focus();
    }
  }, [zustand.obenNummer]);

  return (
    <section aria-labelledby={titelId} className="flex flex-col gap-3">
      {/*
       * **Der eigene Knopf lädt nur die Liste** (E‑1183), „Neu laden“ der
       * Übersicht lädt sie weiterhin mit (E‑1109). Kein Takt (E‑164).
       */}
      <div className="flex items-center justify-between gap-2">
        <h2 id={titelId} className="text-basis font-semibold">
          {t.titel}
        </h2>
        <NeuLaden
          name={t.neuLaden}
          laedt={abfrage.isFetching}
          aufNeuLaden={() => void block.neuLaden()}
        />
      </div>

      {zustand.oben === null ? null : (
        <p
          ref={obenRef}
          role="status"
          tabIndex={-1}
          className="text-beiwerk focus-visible:ring-ring rounded-md focus-visible:ring-2 focus-visible:outline-none"
        >
          {zustand.oben === "geloescht" ? t.geloescht : t.verschwunden}
        </p>
      )}

      {abfrage.isError ? (
        <Fehler
          fehler={abfrage.error}
          text={t.ladenFehler}
          aufWiederholen={() => void abfrage.refetch()}
        />
      ) : abfrage.data === undefined ? null : (
        <Liste
          auftraege={abfrage.data}
          zustand={zustand}
          schritt={schritt}
          loesche={block.loesche}
          loeschtGerade={block.loeschtGerade}
        />
      )}
    </section>
  );
}

function Liste({
  auftraege,
  zustand,
  schritt,
  loesche,
  loeschtGerade,
}: {
  auftraege: readonly Auftrag[];
  zustand: Blockzustand;
  schritt: Dispatch<Blockschritt>;
  loesche: (taskId: string) => void;
  loeschtGerade: string | null;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const t = texte.dashboard.auftraege;
  const mehr = weitere(auftraege, SICHTBARE_AUFTRAEGE);

  if (auftraege.length === 0) {
    return null;
  }

  return (
    <>
      <ul className="flex flex-col">
        {sichtbare(auftraege, zustand.alleAuftraege, SICHTBARE_AUFTRAEGE).map((auftrag) => (
          <Zeile
            key={auftrag.taskId}
            auftrag={auftrag}
            offen={zustand.offen.includes(auftrag.taskId)}
            alleEintraege={zustand.alleEintraege.includes(auftrag.taskId)}
            meldung={zustand.zeilen[auftrag.taskId] ?? null}
            frageOffen={zustand.frage === auftrag.taskId}
            schritt={schritt}
            loesche={loesche}
            loeschtGerade={loeschtGerade}
          />
        ))}
      </ul>
      {mehr === 0 ? null : (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="min-h-bedienelement self-start"
          onClick={() => schritt({ art: "alleAuftraege", an: !zustand.alleAuftraege })}
        >
          {zustand.alleAuftraege
            ? t.wenigerAnzeigen
            : mehr === 1
              ? t.weitereEins
              : einsetzen(t.weitereViele, { anzahl: formatiereZahl(mehr, sprache) })}
        </Button>
      )}
    </>
  );
}

/**
 * Das Zeichen je Stand — **jedes in eigener Form** (F17), damit der Stand auch
 * ohne Farbe zu unterscheiden ist. Angelehnt an die Statusplakette der
 * Nachrichten: Uhr für wartend, Abspielen für läuft. Ein Verzeichnis und kein
 * Zweig: Kommt eine Einordnung dazu, verlangt TypeScript ihr Zeichen.
 */
const STANDZEICHEN: Record<Auftragseinordnung, LucideIcon> = {
  OFFEN: Clock,
  IN_AUSFUEHRUNG: PlayCircle,
  ERLEDIGT: CircleCheck,
  TEILWEISE_ERLEDIGT: CircleAlert,
  FEHLGESCHLAGEN: AlertTriangle,
  UNSTIMMIG: CircleDashed,
  UNBEKANNT: CircleHelp,
};

/** Das Zeichen je Ergebnis eines Eintrags (E‑1106). */
const ERGEBNISZEICHEN: Record<Zeilenergebnis, LucideIcon> = {
  OK: CircleCheck,
  NICHT_OK: CircleAlert,
  UNBEKANNT: CircleAlert,
};

const GESPERRT = "aria-disabled:cursor-not-allowed aria-disabled:opacity-50";

function Zeile({
  auftrag,
  offen,
  alleEintraege,
  meldung,
  frageOffen,
  schritt,
  loesche,
  loeschtGerade,
}: {
  auftrag: Auftrag;
  offen: boolean;
  alleEintraege: boolean;
  meldung: Zeilenmeldung | null;
  frageOffen: boolean;
  schritt: Dispatch<Blockschritt>;
  loesche: (taskId: string) => void;
  loeschtGerade: string | null;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();
  const t = texte.dashboard.auftraege;
  const inhaltId = useId();
  const meldungRef = useRef<HTMLParagraphElement>(null);

  const einordnung = einordnungVon(auftrag);
  const Zeichen = STANDZEICHEN[einordnung];
  const wort = einordnung === "UNBEKANNT" ? auftrag.status : t.stand[einordnung];
  const anzahl = auftrag.eintraege.length;
  const eintraegeText =
    anzahl === 1
      ? t.eintraegeEins
      : einsetzen(t.eintraegeViele, { anzahl: formatiereZahl(anzahl, sprache) });
  // Aufgeklappt gibt es keine dritte Zeile: Die Einträge zeigen Schlüssel und
  // Ergebnis selbst (E‑1188).
  const dritte = offen ? null : dritteZeile(auftrag);

  // Der Satz in der Zeile bekommt den Fokus, wenn er erscheint (F22) — der
  // Knopf „Löschen“ ist dann gerade verschwunden oder wieder zurückgesetzt.
  useEffect(() => {
    if (meldung !== null) {
      meldungRef.current?.focus();
    }
  }, [meldung]);

  return (
    <li className="border-border border-b last:border-b-0">
      <button
        type="button"
        aria-expanded={offen}
        aria-controls={offen ? inhaltId : undefined}
        onClick={() => schritt({ art: "umschalten", taskId: auftrag.taskId })}
        className="focus-visible:ring-ring hover:bg-muted/50 flex w-full min-w-0 flex-col gap-0.5 rounded-md py-1.5 text-left focus-visible:ring-2 focus-visible:outline-none"
      >
        {/*
         * **Der Kopf** (E‑1188), zugeklappt und aufgeklappt gleich. Zeile 1:
         * Stand und Typname in der Grundgröße. Das Wort des Stands bricht nie
         * um, ein langer Typname schon (F29) — umbrochen steht er unter dem
         * Zeichen, nicht unter dem Pfeil. Der Pfeil steht mittig zur ersten
         * Zeile: (1,375 rem Zeilenhöhe − 1 rem) / 2 = `mt-0.75`.
         */}
        <span className="flex min-w-0 items-start gap-2">
          {offen ? (
            <ChevronDown
              aria-hidden="true"
              className="text-muted-foreground mt-0.75 size-4 shrink-0"
            />
          ) : (
            <ChevronRight
              aria-hidden="true"
              className="text-muted-foreground mt-0.75 size-4 shrink-0"
            />
          )}
          <span className="text-basis min-w-0 font-medium">
            <span className={cn("whitespace-nowrap", auftragVordergrund(einordnung))}>
              <Zeichen aria-hidden="true" className="mr-1 inline-block size-4 align-[-0.2em]" />
              <span>{wort}</span>
            </span>
            {/* Das geschützte Leerzeichen hält den Punkt beim Stand. */}
            <span className="text-muted-foreground">{" · "}</span>
            <span>{auftrag.name}</span>
          </span>
        </span>
        {/*
         * Zeile 2: Aktion und Zahl der Einträge, rechts der Zeitpunkt des
         * Einstellens in der Anzeigezone (E‑1122), die Sekunden im `title`.
         */}
        <span className="text-beiwerk text-muted-foreground flex min-w-0 items-start gap-3 pl-6">
          <span className="min-w-0 flex-1">
            {auftrag.aktion} · {eintraegeText}
          </span>
          <span
            className="shrink-0 whitespace-nowrap tabular-nums"
            title={formatiereZeitpunktGenau(auftrag.eingestelltAm, sprache, zone)}
          >
            {formatiereZeitpunkt(auftrag.eingestelltAm, sprache, zone)}
          </span>
        </span>
        {/*
         * Zeile 3, nur zugeklappt: die Zählung oder die Schlüssel. Ohne beides
         * gibt es sie nicht, auch nicht leer (E‑1185, E‑1188).
         */}
        {dritte === "zaehlung" ? (
          <span
            className={cn("text-beiwerk min-w-0 pl-6 font-medium", auftragVordergrund(einordnung))}
          >
            {einsetzen(t.nichtErfolgreich, {
              anzahl: formatiereZahl(nichtErfolgreich(auftrag.eintraege), sprache),
              gesamt: formatiereZahl(anzahl, sprache),
            })}
          </span>
        ) : dritte === "schluessel" ? (
          <Schluesselzeile eintraege={auftrag.eintraege} />
        ) : null}
      </button>

      {offen ? (
        <div id={inhaltId} className="flex flex-col gap-2 pt-1 pb-3 pl-6">
          {/*
           * **Kein Typname über dem Inhalt** (E‑1189): Er steht im Kopf, ebenso
           * der Zeitpunkt des Einstellens. Der Name des Anlegenden ist ein
           * Textknoten in der Schrift der Umgebung (F58); er kann lang sein und
           * eine Adresse ohne Leerzeichen: `min-w-0` lässt die Spalte ihn
           * umbrechen, statt dass er aus ihr hinausläuft.
           */}
          <p className="text-beiwerk text-muted-foreground flex flex-wrap gap-x-3">
            <span className="min-w-0 break-words">
              {einsetzen(t.eingestellt, { name: auftrag.angelegtVon })}
            </span>
            {auftrag.ausgefuehrtAm === null ? null : (
              <span
                className="tabular-nums"
                title={formatiereZeitpunktGenau(auftrag.ausgefuehrtAm, sprache, zone)}
              >
                {einsetzen(t.ausgefuehrt, {
                  zeit: formatiereZeitpunkt(auftrag.ausgefuehrtAm, sprache, zone),
                })}
              </span>
            )}
          </p>

          {einordnung === "FEHLGESCHLAGEN" && auftrag.fehlertext ? (
            <div>
              <p className="text-beiwerk text-muted-foreground">{t.meldungDerPlattform}</p>
              <p className="break-words whitespace-pre-wrap">{auftrag.fehlertext}</p>
            </div>
          ) : null}

          <Hinweis einordnung={einordnung} />

          <Eintraege
            auftrag={auftrag}
            alle={alleEintraege}
            unstimmig={einordnung === "UNSTIMMIG"}
            aufAlle={(an) => schritt({ art: "alleEintraege", taskId: auftrag.taskId, an })}
          />

          {meldung === null ? null : (
            <p
              ref={meldungRef}
              role="status"
              tabIndex={-1}
              className="text-beiwerk focus-visible:ring-ring rounded-md font-medium focus-visible:ring-2 focus-visible:outline-none"
            >
              {meldung === "uebernommen" ? t.uebernommen : t.gestoert}
            </p>
          )}

          {loeschbar(auftrag) ? (
            <Loeschen
              taskId={auftrag.taskId}
              frageOffen={frageOffen}
              hatMeldung={meldung !== null}
              schritt={schritt}
              loesche={loesche}
              loeschtGerade={loeschtGerade}
            />
          ) : null}
        </div>
      ) : null}
    </li>
  );
}

/**
 * Die dritte Zeile mit den Schlüsseln (E‑1102, E‑1188): die ersten zwei,
 * kommagetrennt in Festbreitenschrift, bei mehr „und <n> weitere“ in der
 * Schrift der Umgebung (F29), einzeilig mit Auslassung; der volle Text steht
 * im `title`.
 */
function Schluesselzeile({ eintraege }: { eintraege: Auftrag["eintraege"] }) {
  const t = useTexte().dashboard.auftraege;
  const sprache = useSprache();
  const zugeklappt = schluesselZugeklappt(eintraege);
  if (zugeklappt === null) {
    return null;
  }
  const schluessel = zugeklappt.schluessel.join(", ");
  const undWeitere =
    zugeklappt.weitere === 0
      ? null
      : zugeklappt.weitere === 1
        ? t.undWeiterer
        : einsetzen(t.undWeitere, { anzahl: formatiereZahl(zugeklappt.weitere, sprache) });
  return (
    <span
      className="text-beiwerk min-w-0 truncate pl-6"
      title={undWeitere === null ? schluessel : `${schluessel} ${undWeitere}`}
    >
      {/*
       * Leerzeichen im Schlüssel bleiben stehen (E‑1141): `pre`, nicht
       * `pre-wrap` — die Zeile ist einzeilig gekürzt (E‑1102), und `pre-wrap`
       * bräche sie an den Leerzeichen um (F39).
       */}
      <span className="font-mono whitespace-pre">{schluessel}</span>
      {undWeitere === null ? null : ` ${undWeitere}`}
    </span>
  );
}

function Hinweis({ einordnung }: { einordnung: Auftragseinordnung }) {
  const t = useTexte().dashboard.auftraege;
  switch (einordnung) {
    case "OFFEN":
    case "IN_AUSFUEHRUNG":
    case "UNSTIMMIG":
    case "UNBEKANNT":
      return <p className="text-basis">{t.hinweis[einordnung]}</p>;
    case "ERLEDIGT":
    case "TEILWEISE_ERLEDIGT":
    case "FEHLGESCHLAGEN":
      return null;
  }
}

function Eintraege({
  auftrag,
  alle,
  unstimmig,
  aufAlle,
}: {
  auftrag: Auftrag;
  alle: boolean;
  unstimmig: boolean;
  aufAlle: (an: boolean) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const t = texte.dashboard.auftraege;
  const anzahl = auftrag.eintraege.length;

  return (
    <>
      {/*
       * **Die Einträge in einer Fläche** (E‑1189): gedämpft gegen die Karte,
       * mit Rahmen und runden Ecken, bei jedem Typ gleich. Zwischen zwei
       * Einträgen eine dünne Linie (E‑1186, seit E‑1189 auch mit Schlüssel);
       * `divide-y` zieht sie nur zwischen zweien, bei einem einzigen steht
       * keine. `overflow-hidden` hält den getönten Grund eines Eintrags in
       * den runden Ecken der Fläche.
       */}
      <ul className="bg-muted border-border divide-border flex flex-col divide-y overflow-hidden rounded-md border">
        {sichtbare(auftrag.eintraege, alle, SICHTBARE_EINTRAEGE).map((eintrag, index) => (
          <Eintrag
            // Die Reihenfolge ist die des Auftrags und ändert sich nie; der
            // Schlüssel allein reichte nicht, weil ein Typ ohne Schlüssel
            // `null` liefert.
            key={index}
            eintrag={eintrag}
            felder={auftrag.felder}
            unstimmig={unstimmig}
          />
        ))}
      </ul>
      {anzahl <= SICHTBARE_EINTRAEGE ? null : (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="min-h-bedienelement self-start"
          onClick={() => aufAlle(!alle)}
        >
          {alle
            ? t.wenigerAnzeigen
            : einsetzen(t.alleEintraege, { anzahl: formatiereZahl(anzahl, sprache) })}
        </Button>
      )}
    </>
  );
}

function Eintrag({
  eintrag,
  felder,
  unstimmig,
}: {
  eintrag: Auftragseintrag;
  felder: Auftrag["felder"];
  unstimmig: boolean;
}) {
  const t = useTexte().dashboard.auftraege;
  const sprache = useSprache();
  const ergebnis = ergebnisVon(eintrag);
  // Der Schlüssel ist das erste Feld, mit eigener Beschriftung (E‑1190) —
  // keine Überschrift — und steht immer über die volle Breite, wie kurz er
  // auch ist: Er ist eine Kennung (E‑1192). Ohne Schlüssel steht nichts an
  // seiner Stelle (E‑1185).
  const werte = [
    ...(eintrag.schluessel === null
      ? []
      : [{ name: "schluessel", label: t.schluessel, wert: eintrag.schluessel, breit: true }]),
    ...beschrifteteWerte(felder, eintrag.werte).map((wert) => ({ ...wert, breit: false })),
  ];

  return (
    <li
      className={cn(
        "text-beiwerk @container/eintrag flex flex-col gap-2 px-3 py-2",
        ergebnis === null ? "" : ergebnisFlaeche(ergebnis),
      )}
    >
      {/*
       * **Das Feldraster** (E‑1190, E‑1192): je Feld das `label` aus der Kopie
       * oben, gedämpft, der Wert darunter, nie fett. **Kurze Werte stehen zu
       * zweit nebeneinander**, lange und der Schlüssel über die volle Breite.
       * Kurz heißt: höchstens 17 Zeichen, entschieden am angezeigten Wert — die
       * Definition kennt keine Feldart (`docs/tasks.md` §3.2, Punkt 1051), und
       * am Namen wird nichts entschieden.
       *
       * **Zwei Spalten erst ab 17,75 rem** im Eintrag: Jede Spalte ist dann
       * mindestens 8,5 rem breit, und darin steht ein Wert aus 17 Zeichen
       * Festbreitenschrift (8,29 rem) ebenso ganz wie das längste `label` der
       * Definitionen, „Lieferantennummer“ (7,24 rem; M1017). Die Maße sind rem
       * und gehen mit der Anzeigegröße mit; schmaler bleibt es einspaltig.
       * Ein längeres `label` einer künftigen Definition bricht in seiner Spalte
       * um, statt in die Nachbarin zu laufen.
       *
       * **Der Wert bricht nicht um.** Festbreitenschrift, einzeilig; passt er
       * nicht in die Zeile — eine Nachrichten-ID bei 1.280 px (M1016) —,
       * endet er mit Auslassungspunkten, und der volle Wert steht im `title`
       * und kommt beim Kopieren mit. Leerzeichen bleiben stehen: `pre`, nicht
       * `pre-wrap` (E‑1141, F39). Ein Zeitpunkt in der Schreibweise der
       * Plattform steht im Format aller Zeitpunkte des Blocks (E‑1191).
       */}
      <dl className="grid grid-cols-1 gap-x-3 gap-y-2 @min-[17.75rem]/eintrag:grid-cols-2">
        {werte.map((wert) => {
          const gezeigt = formatiereRohzeitpunkt(wert.wert, sprache);
          return (
            <div
              key={wert.name}
              className={cn(
                "flex min-w-0 flex-col gap-0.5",
                wert.breit || !istKurz(gezeigt) ? "col-span-full" : "",
              )}
            >
              <dt className="text-muted-foreground break-words">{wert.label}</dt>
              <dd
                className="overflow-hidden font-mono text-ellipsis whitespace-pre"
                title={gezeigt}
              >
                {gezeigt}
              </dd>
            </div>
          );
        })}
      </dl>
      {ergebnis === null ? (
        unstimmig ? (
          <p className="text-muted-foreground">{t.keineRueckmeldung}</p>
        ) : null
      ) : (
        <Ergebnis ergebnis={ergebnis} rohwert={eintrag.rueckgabe} />
      )}
    </li>
  );
}

function Ergebnis({ ergebnis, rohwert }: { ergebnis: Zeilenergebnis; rohwert: string | null }) {
  const t = useTexte().dashboard.auftraege;
  const Zeichen = ERGEBNISZEICHEN[ergebnis];
  return (
    <p className={cn("flex items-center gap-1", ergebnisVordergrund(ergebnis))}>
      <Zeichen aria-hidden="true" className="size-4 shrink-0" />
      <span className="break-words">{rohwert}</span>
      <span className="sr-only">({t.ergebnis[ergebnis]})</span>
    </p>
  );
}

/**
 * **„Auftrag löschen“ und die Rückfrage an derselben Stelle** (E‑1111,
 * E‑1112). Ob die Rückfrage offen ist, steht im Zustand des Blocks und nicht
 * hier: Nach dem Löschen wird sie dort geschlossen, gleich wie es ausging.
 *
 * **Fokus:** Beim Öffnen auf „Abbrechen“, beim Abbrechen zurück auf „Auftrag
 * löschen“. Endet das Löschen mit einem Satz, bekommt der den Fokus (F22).
 */
function Loeschen({
  taskId,
  frageOffen,
  hatMeldung,
  schritt,
  loesche,
  loeschtGerade,
}: {
  taskId: string;
  frageOffen: boolean;
  hatMeldung: boolean;
  schritt: Dispatch<Blockschritt>;
  loesche: (taskId: string) => void;
  loeschtGerade: string | null;
}) {
  const t = useTexte().dashboard.auftraege;
  const frageId = useId();
  const knopf = useRef<HTMLButtonElement>(null);
  const abbrechen = useRef<HTMLButtonElement>(null);
  const warOffen = useRef(false);
  const laeuft = loeschtGerade === taskId;
  const gesperrt = loeschtGerade !== null;

  useEffect(() => {
    if (frageOffen) {
      abbrechen.current?.focus();
    } else if (warOffen.current && !hatMeldung) {
      knopf.current?.focus();
    }
    warOffen.current = frageOffen;
  }, [frageOffen, hatMeldung]);

  if (!frageOffen) {
    return (
      <div className="flex justify-end">
        <Button
          ref={knopf}
          type="button"
          variant="outline"
          size="sm"
          className={cn("min-h-bedienelement", GESPERRT)}
          aria-disabled={gesperrt || undefined}
          onClick={() => {
            if (!gesperrt) {
              schritt({ art: "frage", taskId });
            }
          }}
        >
          <Trash2 aria-hidden="true" />
          {t.loeschen}
        </Button>
      </div>
    );
  }

  return (
    <div
      role="group"
      aria-labelledby={frageId}
      className="text-beiwerk border-border flex flex-col gap-2 rounded-md border p-2"
      onKeyDown={(ereignis) => {
        if (ereignis.key === "Escape" && !laeuft) {
          ereignis.stopPropagation();
          schritt({ art: "frage", taskId: null });
        }
      }}
    >
      <p id={frageId}>{t.frage}</p>
      <div className="flex flex-wrap justify-end gap-2">
        <Button
          ref={abbrechen}
          type="button"
          variant="ghost"
          size="sm"
          className={cn("min-h-bedienelement", GESPERRT)}
          aria-disabled={laeuft || undefined}
          onClick={() => {
            if (!laeuft) {
              schritt({ art: "frage", taskId: null });
            }
          }}
        >
          {t.abbrechen}
        </Button>
        <Button
          type="button"
          size="sm"
          className={cn("min-h-bedienelement", GESPERRT)}
          aria-disabled={gesperrt || undefined}
          aria-busy={laeuft || undefined}
          onClick={() => {
            if (!gesperrt) {
              loesche(taskId);
            }
          }}
        >
          {laeuft ? t.loeschenLaeuft : t.bestaetigen}
        </Button>
      </div>
    </div>
  );
}
