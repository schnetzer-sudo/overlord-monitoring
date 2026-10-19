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

import { Button } from "@/components/ui/button";
import { useAnzeigezone } from "@/components/zeitzone";
import { Fehler } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl, formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";
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
  einordnungVon,
  ergebnisVon,
  loeschbar,
  mehrereTypen,
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
      <h2 id={titelId} className="text-basis font-semibold">
        {t.titel}
      </h2>

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
  const mitTyp = mehrereTypen(auftraege);
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
            mitTyp={mitTyp}
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
  mitTyp,
  offen,
  alleEintraege,
  meldung,
  frageOffen,
  schritt,
  loesche,
  loeschtGerade,
}: {
  auftrag: Auftrag;
  mitTyp: boolean;
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
  const kopf = [...(mitTyp ? [auftrag.name] : []), auftrag.aktion, eintraegeText].join(" · ");

  const zugeklappt = schluesselZugeklappt(auftrag.eintraege, (nummer) =>
    einsetzen(t.eintrag, { nummer: formatiereZahl(nummer, sprache) }),
  );
  const schluesselText = [
    zugeklappt.schluessel.join(", "),
    zugeklappt.weitere === 0
      ? null
      : zugeklappt.weitere === 1
        ? t.undWeiterer
        : einsetzen(t.undWeitere, { anzahl: formatiereZahl(zugeklappt.weitere, sprache) }),
  ]
    .filter((teil) => teil !== null)
    .join(" ");

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
        <span className="flex min-w-0 items-center gap-2">
          {offen ? (
            <ChevronDown aria-hidden="true" className="text-muted-foreground size-4 shrink-0" />
          ) : (
            <ChevronRight aria-hidden="true" className="text-muted-foreground size-4 shrink-0" />
          )}
          <span
            className={cn(
              "text-beiwerk flex shrink-0 items-center gap-1 font-medium",
              auftragVordergrund(einordnung),
            )}
          >
            <Zeichen aria-hidden="true" className="size-4 shrink-0" />
            <span>{wort}</span>
          </span>
          <span className="text-beiwerk text-muted-foreground min-w-0 truncate" title={kopf}>
            · {kopf}
          </span>
        </span>
        <span className="flex min-w-0 items-baseline gap-3 pl-6">
          {einordnung === "TEILWEISE_ERLEDIGT" ? (
            <span
              className={cn(
                "text-beiwerk min-w-0 flex-1 truncate font-medium",
                auftragVordergrund(einordnung),
              )}
            >
              {einsetzen(t.nichtErfolgreich, {
                anzahl: formatiereZahl(nichtErfolgreich(auftrag.eintraege), sprache),
                gesamt: formatiereZahl(anzahl, sprache),
              })}
            </span>
          ) : (
            <span className="text-beiwerk min-w-0 flex-1 truncate font-mono" title={schluesselText}>
              {schluesselText}
            </span>
          )}
          <span
            className="text-beiwerk text-muted-foreground shrink-0 tabular-nums"
            title={formatiereZeitpunktGenau(auftrag.eingestelltAm, sprache, zone)}
          >
            {formatiereZeitpunkt(auftrag.eingestelltAm, sprache, zone)}
          </span>
        </span>
      </button>

      {offen ? (
        <div id={inhaltId} className="text-beiwerk flex flex-col gap-2 pt-1 pb-3 pl-6">
          <p className="font-medium">{auftrag.name}</p>
          <p className="text-muted-foreground flex flex-wrap gap-x-3">
            <span title={formatiereZeitpunktGenau(auftrag.eingestelltAm, sprache, zone)}>
              {einsetzen(t.eingestellt, {
                zeit: formatiereZeitpunkt(auftrag.eingestelltAm, sprache, zone),
              })}
            </span>
            {auftrag.ausgefuehrtAm === null ? null : (
              <span title={formatiereZeitpunktGenau(auftrag.ausgefuehrtAm, sprache, zone)}>
                {einsetzen(t.ausgefuehrt, {
                  zeit: formatiereZeitpunkt(auftrag.ausgefuehrtAm, sprache, zone),
                })}
              </span>
            )}
          </p>

          {einordnung === "FEHLGESCHLAGEN" && auftrag.fehlertext ? (
            <div>
              <p className="font-medium">{t.meldungDerPlattform}</p>
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
              className="focus-visible:ring-ring rounded-md font-medium focus-visible:ring-2 focus-visible:outline-none"
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

function Hinweis({ einordnung }: { einordnung: Auftragseinordnung }) {
  const t = useTexte().dashboard.auftraege;
  switch (einordnung) {
    case "OFFEN":
    case "IN_AUSFUEHRUNG":
    case "UNSTIMMIG":
    case "UNBEKANNT":
      return <p>{t.hinweis[einordnung]}</p>;
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
      <ul className="flex flex-col gap-1">
        {sichtbare(auftrag.eintraege, alle, SICHTBARE_EINTRAEGE).map((eintrag, index) => (
          <Eintrag
            // Die Reihenfolge ist die des Auftrags und ändert sich nie; der
            // Schlüssel allein reichte nicht, weil ein Typ ohne Schlüssel
            // `null` liefert.
            key={index}
            eintrag={eintrag}
            nummer={index + 1}
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
  nummer,
  felder,
  unstimmig,
}: {
  eintrag: Auftragseintrag;
  nummer: number;
  felder: Auftrag["felder"];
  unstimmig: boolean;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const t = texte.dashboard.auftraege;
  const ergebnis = ergebnisVon(eintrag);

  return (
    <li className={cn("rounded-md px-2 py-1", ergebnis === null ? "" : ergebnisFlaeche(ergebnis))}>
      <p className="font-mono break-all">
        {eintrag.schluessel ?? einsetzen(t.eintrag, { nummer: formatiereZahl(nummer, sprache) })}
      </p>
      <dl className="flex flex-wrap gap-x-3">
        {beschrifteteWerte(felder, eintrag.werte).map((wert) => (
          <div key={wert.name} className="flex gap-1">
            <dt className="text-muted-foreground">{wert.label}</dt>
            <dd className="font-mono break-all">{wert.wert}</dd>
          </div>
        ))}
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
      className="border-border flex flex-col gap-2 rounded-md border p-2"
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
