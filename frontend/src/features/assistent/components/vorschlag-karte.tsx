"use client";

import { Fragment, useId, useLayoutEffect, useRef, type RefObject } from "react";
import { AlertCircle, CircleCheck, Info } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { einsetzen, type Texte } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import type { Mandant } from "@/lib/mandanten";
import { cn } from "@/lib/utils";

import type { Prueffehler, VorschlagFeld, Werte } from "../api";
import {
  aktuelleFehler,
  angezeigteWerte,
  darfAnlegen,
  darfUebernehmen,
  istLeer,
  OFFENE_EINTRAEGE,
  ortVon,
  sichtbareEintraege,
  ueberschrift,
  type Karte,
} from "../gespraech";
import type { Kartensteuerung } from "../hooks";

/**
 * Gesperrt heißt hier **`aria-disabled`**, nicht `disabled`: Ein gesperrter
 * Knopf verlöre unter der Tastatur den Fokus, gerade dann, wenn er eben gedrückt
 * wurde (F11, dieselbe Abwägung wie beim Knopf „Neu laden“). Der Klick prüft die
 * Sperre selbst.
 */
const GESPERRT = "aria-disabled:cursor-not-allowed aria-disabled:opacity-50";

/**
 * **Die Bestätigungskarte** (E‑1067, `docs/assistent-frontend.md` §5).
 *
 * Sie kennt keinen Auftragstyp (E‑1070): Sie zeigt Name, Aktion und jedes Feld,
 * das der Vorschlag mitbringt, mit `label` und `hinweis`, und prüft nichts
 * selbst — Änderungen prüft das Backend (E‑1071). Ihr Zustand liegt im Gespräch
 * und nicht hier (F3); die Komponente ist Anzeige und Bedienung.
 */
export function VorschlagKarte({
  karteId,
  karte,
  mandant,
  steuerung,
}: {
  karteId: string;
  karte: Karte;
  mandant: Mandant;
  steuerung: Kartensteuerung;
}) {
  const texte = useTexte();
  const t = texte.assistent.karte;
  const kennung = useId();
  const { vorschlag } = karte;

  const werte = angezeigteWerte(karte);
  const fehler = aktuelleFehler(karte);
  const sichtbar = sichtbareEintraege(karte);
  const aendern = karte.modus === "aendern";
  const angelegt = karte.anlegen.stand === "angelegt";
  const anlegenLaeuft = karte.anlegen.stand === "laeuft";
  // F7: „Offen“ für OFFEN, jede andere Einordnung roh — die Wörter dafür gehören zu A4.
  const einordnung = karte.anlegen.stand === "angelegt" ? karte.anlegen.einordnung : "";

  /*
   * Der Fokus folgt der Handlung (§7): „Ändern“ ins erste Feld, „Übernehmen“
   * und „Verwerfen“ zurück auf „Ändern“, nach dem Anlegen auf die Zeile mit dem
   * Status. Gesetzt wird er nach dem Rendern, das den Zielknoten erst einhängt —
   * und nur, wenn der Kunde hier gerade gehandelt hat, nicht beim
   * Wiederöffnen der Chatbox.
   */
  const fokusZiel = useRef<"aendern" | "erstesFeld" | "angelegt" | null>(null);
  const aendernKnopf = useRef<HTMLButtonElement>(null);
  const erstesFeld = useRef<HTMLInputElement>(null);
  const statuszeile = useRef<HTMLParagraphElement>(null);

  useLayoutEffect(() => {
    const ziel = fokusZiel.current;
    if (ziel === null) {
      return;
    }
    if (ziel === "angelegt" && !angelegt) {
      if (!anlegenLaeuft) {
        fokusZiel.current = null;
      }
      return;
    }
    const element =
      ziel === "aendern"
        ? aendernKnopf.current
        : ziel === "erstesFeld"
          ? erstesFeld.current
          : statuszeile.current;
    if (element !== null) {
      element.focus();
      fokusZiel.current = null;
    }
  });

  function setzeWert(index: number, feld: string, wert: string) {
    if (karte.entwurf === null) {
      return;
    }
    const entwurf: Werte[] = karte.entwurf.map((eintrag, nummer) =>
      nummer === index ? { ...eintrag, [feld]: wert } : eintrag,
    );
    steuerung.feld(karteId, entwurf);
  }

  const kartenfehler = fehler.filter((eintrag) => ortVon(eintrag, karte).art === "karte");
  const statussatz = statussatzFuer(karte, t);

  return (
    <section
      aria-label={einsetzen(t.bezeichnung, { name: vorschlag.name })}
      data-slot="vorschlag-karte"
      data-modus={karte.modus}
      className="border-border bg-card text-card-foreground relative flex flex-col rounded-lg border"
    >
      <header className="border-border flex flex-col gap-1.5 border-b px-3 py-2">
        <h3 className="leading-snug font-semibold">{vorschlag.name}</h3>
        <div className="text-beiwerk flex flex-wrap items-center gap-x-2 gap-y-1">
          <Badge variant="outline">{vorschlag.aktion}</Badge>
          <span data-teil="mandant" className="flex min-w-0 items-baseline gap-1">
            <span className="text-muted-foreground shrink-0">{t.giltFuer}</span>
            <span className="font-mono font-medium">{mandant.id}</span>
            {mandant.name !== mandant.id ? (
              <span className="text-muted-foreground truncate" title={mandant.name}>
                {mandant.name}
              </span>
            ) : null}
          </span>
        </div>
      </header>

      {kartenfehler.length > 0 ? (
        <div className="border-border flex flex-col gap-1 border-b px-3 py-2">
          {kartenfehler.map((eintrag, nummer) => (
            <Fehlerzeile key={nummer} fehler={eintrag} />
          ))}
        </div>
      ) : null}

      <ol className="divide-border flex flex-col divide-y">
        {werte.slice(0, sichtbar).map((eintrag, index) => {
          const titel = ueberschrift(karte, index);
          const eintragsfehler = fehler.filter((kandidat) => {
            const ort = ortVon(kandidat, karte);
            return ort.art === "eintrag" && ort.index === index;
          });
          return (
            <li key={index} data-eintrag={index} className="px-3 py-2">
              <h4 className="flex items-baseline gap-2">
                {titel === null ? (
                  <span className="font-medium">{einsetzen(t.eintrag, { nummer: index + 1 })}</span>
                ) : (
                  <>
                    <span className="text-muted-foreground text-beiwerk tabular-nums">
                      <span className="sr-only">
                        {einsetzen(t.eintrag, { nummer: index + 1 })}:{" "}
                      </span>
                      <span aria-hidden="true">{index + 1}</span>
                    </span>
                    <span className="font-mono font-medium break-all">{titel}</span>
                  </>
                )}
              </h4>
              {eintragsfehler.map((eintragsFehler, nummer) => (
                <Fehlerzeile key={nummer} fehler={eintragsFehler} />
              ))}
              <dl className="mt-1.5 grid grid-cols-[minmax(0,9rem)_minmax(0,1fr)] items-baseline gap-x-3 gap-y-1.5">
                {vorschlag.felder.map((feld, feldnummer) => {
                  const feldfehler = fehler.filter((kandidat) => {
                    const ort = ortVon(kandidat, karte);
                    return ort.art === "feld" && ort.index === index && ort.feld === feld.name;
                  });
                  return (
                    <Feldzeile
                      key={feld.name}
                      kennung={`${kennung}-${index}-${feldnummer}`}
                      feld={feld}
                      wert={eintrag[feld.name] ?? null}
                      fehler={feldfehler}
                      aendern={aendern}
                      leer={t.leer}
                      eingabeRef={index === 0 && feldnummer === 0 ? erstesFeld : undefined}
                      aufAenderung={(neu) => setzeWert(index, feld.name, neu)}
                    />
                  );
                })}
              </dl>
            </li>
          );
        })}
      </ol>

      {werte.length > OFFENE_EINTRAEGE &&
      !aendern &&
      (sichtbar < werte.length || karte.aufgeklappt) ? (
        <div className="border-border border-t px-3 py-1">
          <Button
            type="button"
            variant="link"
            className="min-h-bedienelement h-auto px-0"
            aria-expanded={karte.aufgeklappt}
            onClick={() => steuerung.aufklappen(karteId, !karte.aufgeklappt)}
          >
            {karte.aufgeklappt
              ? einsetzen(t.wenigerAnzeigen, { anzahl: OFFENE_EINTRAEGE })
              : einsetzen(t.alleAnzeigen, { anzahl: werte.length })}
          </Button>
        </div>
      ) : null}

      <footer className="border-border flex flex-wrap items-center gap-2 border-t px-3 py-2">
        {angelegt ? (
          <p
            ref={statuszeile}
            tabIndex={-1}
            data-teil="angelegt"
            className="flex items-center gap-1.5 font-medium outline-none"
          >
            <CircleCheck aria-hidden="true" className="size-4 shrink-0" />
            {einsetzen(t.angelegt, { status: einordnung === "OFFEN" ? t.offen : einordnung })}
          </p>
        ) : aendern ? (
          <>
            <Button
              type="button"
              className={cn("min-h-bedienelement", GESPERRT)}
              aria-disabled={!darfUebernehmen(karte)}
              onClick={() => {
                if (darfUebernehmen(karte)) {
                  fokusZiel.current = "aendern";
                  steuerung.uebernehmen(karteId);
                }
              }}
            >
              {t.uebernehmen}
            </Button>
            <Button
              type="button"
              variant="outline"
              className="min-h-bedienelement"
              onClick={() => {
                fokusZiel.current = "aendern";
                steuerung.verwerfen(karteId);
              }}
            >
              {t.verwerfen}
            </Button>
            {karte.pruefung.stand === "gestoert" ? (
              <Button
                type="button"
                variant="link"
                className="min-h-bedienelement h-auto px-1"
                onClick={() => steuerung.erneutPruefen(karteId)}
              >
                {t.erneutPruefen}
              </Button>
            ) : null}
          </>
        ) : (
          <>
            <Button
              type="button"
              className={cn("min-h-bedienelement", GESPERRT)}
              aria-disabled={!darfAnlegen(karte)}
              aria-busy={anlegenLaeuft || undefined}
              onClick={() => {
                if (darfAnlegen(karte)) {
                  fokusZiel.current = "angelegt";
                  steuerung.anlegen(karteId);
                }
              }}
            >
              {anlegenLaeuft ? t.anlegenLaeuft : t.anlegen}
            </Button>
            <Button
              ref={aendernKnopf}
              type="button"
              variant="outline"
              className={cn("min-h-bedienelement", GESPERRT)}
              aria-disabled={anlegenLaeuft}
              onClick={() => {
                if (!anlegenLaeuft) {
                  fokusZiel.current = "erstesFeld";
                  steuerung.aendern(karteId);
                }
              }}
            >
              {t.aendern}
            </Button>
          </>
        )}
        <p
          role="status"
          data-teil="pruefstand"
          className="text-beiwerk text-muted-foreground w-full empty:hidden"
        >
          {statussatz}
        </p>
      </footer>
    </section>
  );
}

/** Der Satz unter den Knöpfen: wo Prüfung oder Anlegen stehen. Leer, wenn es nichts zu sagen gibt. */
function statussatzFuer(karte: Karte, t: Texte["assistent"]["karte"]): string {
  if (karte.modus === "aendern") {
    switch (karte.pruefung.stand) {
      case "laeuft":
        return t.pruefungLaeuft;
      case "fehler":
        return t.pruefungFehler;
      case "gestoert":
        return t.pruefungGestoert;
      case "typUnbekannt":
        return t.typUnbekannt;
      default:
        return "";
    }
  }
  switch (karte.anlegen.stand) {
    case "fehler":
      return t.anlegenFehler;
    case "gestoert":
      return t.anlegenGestoert;
    case "typUnbekannt":
      return t.typUnbekannt;
    default:
      return "";
  }
}

/** Eine Zeile Beschriftung und Wert — in der Ansicht als Text, im Änderungsmodus als Feld. */
function Feldzeile({
  kennung,
  feld,
  wert,
  fehler,
  aendern,
  leer,
  eingabeRef,
  aufAenderung,
}: {
  kennung: string;
  feld: VorschlagFeld;
  wert: string | null;
  fehler: Prueffehler[];
  aendern: boolean;
  leer: string;
  eingabeRef: RefObject<HTMLInputElement | null> | undefined;
  aufAenderung: (wert: string) => void;
}) {
  const istLeerWert = istLeer(wert);
  const hinweisId = `${kennung}-hinweis`;
  const fehlerId = `${kennung}-fehler`;
  // E‑1070: Die Bedeutung eines leeren Felds steht darunter — dort, wo der Kunde
  // auf „leer“ schaut, und nicht in einem Tooltip.
  const zeigeHinweis = istLeerWert && feld.hinweis !== null;
  const beschrieben = [zeigeHinweis ? hinweisId : null, fehler.length > 0 ? fehlerId : null]
    .filter((teil) => teil !== null)
    .join(" ");

  return (
    <Fragment>
      <dt className="text-muted-foreground text-beiwerk min-w-0 break-words">
        {aendern ? (
          <Label htmlFor={kennung} className="text-beiwerk text-muted-foreground font-normal">
            {feld.label}
          </Label>
        ) : (
          feld.label
        )}
      </dt>
      <dd data-feld={feld.name} className="min-w-0">
        {aendern ? (
          <Input
            ref={eingabeRef}
            id={kennung}
            value={wert ?? ""}
            autoComplete="off"
            spellCheck={false}
            aria-invalid={fehler.length > 0 || undefined}
            aria-describedby={beschrieben === "" ? undefined : beschrieben}
            className="h-bedienelement font-mono"
            onChange={(ereignis) => aufAenderung(ereignis.target.value)}
          />
        ) : istLeerWert ? (
          <span
            data-teil="leer"
            aria-describedby={zeigeHinweis ? hinweisId : undefined}
            className="border-border text-muted-foreground inline-block rounded-sm border border-dashed px-1.5 italic"
          >
            {leer}
          </span>
        ) : (
          <span data-teil="wert" className="font-mono break-all whitespace-pre-wrap">
            {wert}
          </span>
        )}
        {zeigeHinweis ? (
          <p id={hinweisId} data-teil="hinweis" className="text-beiwerk mt-1 flex gap-1.5">
            <Info aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
            <span>{feld.hinweis}</span>
          </p>
        ) : null}
        {fehler.length > 0 ? (
          <div id={fehlerId}>
            {fehler.map((eintrag, nummer) => (
              <Fehlerzeile key={nummer} fehler={eintrag} />
            ))}
          </div>
        ) : null}
      </dd>
    </Fragment>
  );
}

/** Ein Prüffehler mit dem Klartext des Backends, unverändert (E‑1071) — mit Zeichen, nicht nur Farbe. */
function Fehlerzeile({ fehler }: { fehler: Prueffehler }) {
  return (
    <p data-teil="fehler" className="text-status-fehler text-beiwerk mt-1 flex gap-1.5">
      <AlertCircle aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
      <span>{fehler.meldung}</span>
    </p>
  );
}
