"use client";

import Link from "next/link";
import { useState } from "react";
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  CircleOff,
  Clock,
  Mails,
  PlayCircle,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereDauer, formatiereZahl } from "@/lib/format";
import type { Rollupzeitraum } from "@/lib/rollupzeitraum";
import { statusKlassen, statusKlassenOhneKontur } from "@/lib/status-farbe";
import type { Statusart } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { Fehlerart, Fenster, Kacheln as Kachelwerte, OffeneKachel } from "../api";
import { fehlerartText } from "../beschriftung";
import { fehlerZiel, laeuftZiel, wartendZiel } from "../verweise";

/**
 * Die Kacheln — **Fehler, Läuft, Wartend, Nachrichten, in dieser Reihenfolge**
 * (Entscheidung **E‑78**).
 *
 * Die Reihenfolge folgt dem Leitsatz: erst was zu tun ist, dann was in Arbeit
 * ist, dann die Zählung. **Und *Wartend* steht vor *Nachrichten*, damit sein
 * Wegfall die Reihe von hinten auf drei zusammenzieht**, statt eine Lücke in die
 * Mitte zu schlagen.
 *
 * ## Es sind drei oder vier, und der Unterschied ist eine Auskunft (E‑81)
 *
 * | Zustand | Antwort | Anzeige |
 * |---|---|---|
 * | **strukturell abwesend** | `wartend` fehlt | **keine Kachel** — kein Platzhalter, keine gedämpfte Kachel, kein „nicht verfügbar" |
 * | **nicht ermittelbar** | `ermittelbar: false` | **Kachel da**, Text nach E‑q, **nicht klickbar** |
 * | **ermittelt** | `ermittelbar: true` | die Zahl, bei `anzahl > 0` zusätzlich das Alter |
 *
 * **Abwesenheit ist eine Auskunft über den Mandanten** („hat keine Abläufe, die
 * suspendieren"), `ermittelbar: false` eine über **uns** („wissen es gerade
 * nicht"). Verschwände die Kachel bei einem Fehlschlag, würde ein Ausfall
 * stillschweigend in eine strukturelle Behauptung übersetzt — der schlimmste der
 * drei denkbaren Fehler hier. Für *Läuft* gilt dasselbe ohne den ersten Fall:
 * Die Kachel ist immer da, laufen kann jeder Mandant.
 *
 * ## Genau eine Kachel trägt eine Fläche (Entscheidung **E‑79**)
 *
 * **E‑u war für zwei Problemkacheln geschrieben; seit E‑71 gibt es eine** —
 * *Fehler*. *Läuft*, *Wartend* und *Nachrichten* tragen keine Fläche. Das ist
 * keine Aufweichung von E‑u, sondern seine Schärfung: „gefüllt heißt, hier ist
 * etwas zu tun" wird eindeutig, weil es nur noch einen Fall gibt. Trügen die
 * beiden Zustandskacheln ebenfalls Fläche, wäre die Unterscheidung wieder
 * aufgelöst — und `visuelles-konzept.md` §7a hat gemessen, wie wenig dafür nötig
 * ist: **0,025** Unterschied wurden als Rangfolge gelesen.
 *
 * **Damit sie trotzdem nicht wie nackte Zahlen aussehen**, tragen sie
 * `--status-offen` in einem kleinen Träger — der **Statusplakette**, in der
 * Gestalt und über die Verwendung, die auch die Liste nimmt
 * (`statusKlassen` in `lib/status-farbe.ts`). Die Kachel sieht damit aus wie die
 * Zeilen, auf die sie führt. **Der Träger steht an der Stelle des Kopfes und
 * nicht daneben:** Er trägt Zeichen *und* Wort, und ein Kopf darüber sagte
 * dasselbe Wort ein zweites Mal.
 *
 * ## Was klickt, und mit welchem Zeitraum (E‑m, **E‑80**)
 *
 * | Kachel | Ziel | Zeitraum |
 * |---|---|---|
 * | **Fehler** | `status=FEHLER` | erbt |
 * | **Läuft** | `status=LAEUFT` | **erbt** — eine laufende Nachricht ist höchstens so alt wie die Wächterfrist |
 * | **Wartend** | `status=WARTEND` | **bringt seinen mit**, aus `aeltesteSekunden` (`verweise.ts`) |
 * | **Nachrichten** | — | nicht klickbar |
 *
 * **Verlinkt ist ein Bereich der Kachel und nicht die ganze.** In der
 * Fehlerkachel steht darunter die Schaltfläche für die Aufschlüsselung, und ein
 * `<button>` in einem `<a>` ist kein gültiges Markup.
 */
export function Kacheln({
  kacheln,
  fenster,
  zeitraum,
}: {
  kacheln: Kachelwerte;
  fenster: Fenster;
  zeitraum: Rollupzeitraum;
}) {
  const texte = useTexte();
  const wartend = kacheln.wartend;

  return (
    <div className="flex flex-col gap-2">
      {/*
       * **Die Umbruchregel ist erweitert und nicht ersetzt.** `sm:grid-cols-2`
       * bleibt; nur die Spaltenzahl am breiten Fenster folgt der Zahl der
       * Kacheln — sonst ließe die Reihe bei drei Kacheln eine leere vierte
       * Spalte stehen, und eine Lücke sähe aus wie eine fehlende Zahl.
       * Zusammengesetzte Klassennamen entstehen dabei nicht: Tailwind sucht den
       * Quelltext ab, und beide Formen stehen vollständig da.
       */}
      <div
        className={cn(
          "grid gap-3 sm:grid-cols-2",
          wartend === undefined ? "xl:grid-cols-3" : "xl:grid-cols-4",
        )}
      >
        <FehlerKachel fehler={kacheln.fehler} fenster={fenster} />
        <ZustandKachel
          art="LAEUFT"
          kachel={kacheln.laeuft}
          ziel={laeuftZiel(fenster)}
          verweisText={texte.dashboard.kacheln.laeuftVerweis}
        />
        {wartend === undefined ? null : (
          <ZustandKachel
            art="WARTEND"
            kachel={wartend}
            ziel={wartendZiel(fenster, zeitraum, wartend.aeltesteSekunden)}
            verweisText={texte.dashboard.kacheln.wartendVerweis}
            ohneVerweisText={texte.dashboard.kacheln.wartendOhneVerweis}
          />
        )}
        <NachrichtenKachel anzahl={kacheln.nachrichten} />
      </div>

      {/*
       * Sichtbar und nicht in einem `title`: Auf einem Berührungsgerät gibt es
       * kein Überfahren, und dort erführe es niemand (bekannte Grenze 3).
       */}
      <p className="text-muted-foreground text-beiwerk">
        {texte.dashboard.kacheln.nachrichtenHinweis}
      </p>
    </div>
  );
}

/** Der gemeinsame Rahmen. Die Farbe kommt als Klassenkette von außen. */
function Kachel({ klassen, children }: { klassen?: string; children: React.ReactNode }) {
  return (
    <Card size="sm" className={cn("gap-2 px-4", klassen)}>
      {children}
    </Card>
  );
}

function Kopf({ zeichen: Zeichen, titel }: { zeichen: typeof AlertTriangle; titel: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <Zeichen aria-hidden="true" className="size-4 shrink-0" />
      <span className="text-basis font-medium">{titel}</span>
    </div>
  );
}

function Zahl({ wert }: { wert: number }) {
  const sprache = useSprache();
  return (
    <span className="text-3xl leading-none font-semibold tabular-nums">
      {formatiereZahl(wert, sprache)}
    </span>
  );
}

/**
 * Der anklickbare Teil einer Kachel.
 *
 * **Ein echter Verweis und keine Schaltfläche.** „Schick mir mal den Link" ist
 * bei diesem Werkzeug die eigentliche Anwendung; ein Ziel, das man weder mit der
 * mittleren Maustaste öffnen noch kopieren kann, verfehlt das.
 */
function Verweis({
  ziel,
  bezeichnung,
  children,
}: {
  ziel: string;
  bezeichnung: string;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={ziel}
      aria-label={bezeichnung}
      title={bezeichnung}
      className="focus-visible:ring-ring -mx-1 rounded-md px-1 hover:underline focus-visible:ring-2 focus-visible:outline-none"
    >
      {children}
    </Link>
  );
}

function FehlerKachel({ fehler, fenster }: { fehler: Kachelwerte["fehler"]; fenster: Fenster }) {
  const texte = useTexte();
  const [offen, setOffen] = useState(false);
  const hatArten = fehler.arten.length > 0;

  return (
    <Kachel klassen={statusKlassenOhneKontur("FEHLER")}>
      <Kopf zeichen={AlertTriangle} titel={texte.dashboard.kacheln.fehler} />
      <Verweis ziel={fehlerZiel(fenster)} bezeichnung={texte.dashboard.kacheln.fehlerVerweis}>
        <Zahl wert={fehler.anzahl} />
      </Verweis>

      {hatArten ? (
        <div className="flex flex-col gap-1">
          <button
            type="button"
            onClick={() => setOffen((bisher) => !bisher)}
            aria-expanded={offen}
            className="focus-visible:ring-ring text-beiwerk -mx-1 flex w-fit items-center gap-1 rounded-md px-1 opacity-90 hover:underline focus-visible:ring-2 focus-visible:outline-none"
          >
            {offen
              ? texte.dashboard.kacheln.artenZuklappen
              : texte.dashboard.kacheln.artenAufklappen}
            {offen ? (
              <ChevronUp aria-hidden="true" className="size-3.5" />
            ) : (
              <ChevronDown aria-hidden="true" className="size-3.5" />
            )}
          </button>
          {offen ? <Fehlerarten arten={fehler.arten} /> : null}
        </div>
      ) : null}
    </Kachel>
  );
}

/**
 * Die Aufschlüsselung nach Art — **inline in der Kachel, aufklappbar**.
 *
 * **Beschriftet wird über den Rohwert und nicht über die gelieferte `art`.** Der
 * Endpunkt setzt für `COMMIT_REJECTED` einen deutschen Festtext
 * (`MessageStatusClassifier.ABGELEHNT_VOM_PARTNER`); stünde der hier
 * unverändert, läse ihn auch ein englischer Nutzer. Für jeden anderen Rohwert
 * ist `art` kein Anzeigetext, sondern ein **Wert** — der Namensteil hinter
 * `ERROR_` oder der Rohwert selbst — und der wird nicht übersetzt (Regel Q4).
 *
 * **Der Rohwert steht daneben**, als `title`: Ohne ihn wäre *„Vom Partner
 * abgelehnt"* eine Zeichenkette, an der sich nichts mehr festmachen ließe.
 */
function Fehlerarten({ arten }: { arten: readonly Fehlerart[] }) {
  const texte = useTexte();
  const sprache = useSprache();

  return (
    <dl className="text-beiwerk grid grid-cols-[1fr_auto] gap-x-3 tabular-nums">
      {arten.map((art) => (
        <ArtZeile
          key={art.rohwert}
          rohwert={art.rohwert}
          name={fehlerartText(art, texte)}
          anzahl={formatiereZahl(art.anzahl, sprache)}
        />
      ))}
    </dl>
  );
}

function ArtZeile({ rohwert, name, anzahl }: { rohwert: string; name: string; anzahl: string }) {
  return (
    <>
      <dt className="truncate opacity-90" title={rohwert}>
        {name}
      </dt>
      <dd className="text-right font-medium">{anzahl}</dd>
    </>
  );
}

/** Die beiden offenen Zustände tragen dieselben Zeichen wie die Plakette der Liste. */
const ZUSTANDSZEICHEN = { LAEUFT: PlayCircle, WARTEND: Clock } as const;

/**
 * Eine der beiden Zustandskacheln — *Läuft* und *Wartend*.
 *
 * **Dieselbe Gestalt für beide, weil es dieselbe Auskunft in zwei Zuständen
 * ist.** Der einzige Unterschied steht außen: *Wartend* kann fehlen (E‑74) und
 * bringt sein Fenster mit (E‑80), *Läuft* nicht und nicht.
 *
 * ## Die zweite Zeile ist eine Prüfung, keine Verzierung
 *
 * **E‑75 hat `aeltesteSekunden` dafür gebaut.** Die Regel, die *Überfällig*
 * gestürzt hat, ist eine **fachliche Auskunft und keine Messung** (E‑71); bis
 * die offene Prüfung gegen die Produktion gefahren ist, ist diese Zeile der Ort,
 * an dem sie beobachtbar bleibt: *„ältester seit 40 Tagen"* widerlegt die eine
 * Woche, und *Läuft* über einer halben Stunde heißt, der Wächter hängt.
 *
 * **Sie entfällt bei `anzahl = 0`** — kein „—", kein „keine". Ohne Zeile gibt es
 * kein Alter, und die Null steht für sich.
 */
function ZustandKachel({
  art,
  kachel,
  ziel,
  verweisText,
  ohneVerweisText,
}: {
  art: Extract<Statusart, "LAEUFT" | "WARTEND">;
  kachel: OffeneKachel;
  /** `null` heißt Notbremse: Die Spanne läge über einem Jahr (`verweise.ts`). */
  ziel: string | null;
  verweisText: string;
  /** Der Satz, der bei der Notbremse an die Stelle des Verweises tritt. */
  ohneVerweisText?: string;
}) {
  const texte = useTexte();

  return (
    <Kachel>
      <Plakette art={art} />

      {kachel.ermittelbar && kachel.anzahl !== null ? (
        <>
          {ziel === null ? (
            <Zahl wert={kachel.anzahl} />
          ) : (
            <Verweis ziel={ziel} bezeichnung={verweisText}>
              <Zahl wert={kachel.anzahl} />
            </Verweis>
          )}

          {kachel.aeltesteSekunden === null ? null : (
            <p className="text-beiwerk tabular-nums opacity-90">
              {einsetzen(texte.dashboard.kacheln.aeltesterSeit, {
                dauer: formatiereDauer(kachel.aeltesteSekunden, texte.dashboard.kacheln.dauer),
              })}
            </p>
          )}

          {ziel === null && ohneVerweisText !== undefined ? (
            <p className="text-beiwerk opacity-75">{ohneVerweisText}</p>
          ) : null}

          {/*
           * **Ohne diesen Satz widersprechen sich zwei Zahlen auf derselben
           * Seite sichtbar**, sobald 48 Stunden gewählt sind — der Normalfall:
           * Diese Kachel zählt den ganzen Bestand (Regel L9), alles andere auf
           * der Seite den gewählten Zeitraum.
           *
           * **Er steht in der Kachel und nicht als geteilte Zeile darunter.**
           * Eine geteilte Zeile müsste beide Kacheln benennen — und nennte damit
           * bei einem Mandanten ohne suspendierende Abläufe eine, die es auf
           * seiner Seite gar nicht gibt.
           */}
          <p className="text-beiwerk opacity-75">{texte.dashboard.kacheln.bestandHinweis}</p>
        </>
      ) : (
        <NichtErmittelbar />
      )}
    </Kachel>
  );
}

/**
 * Der kleine Träger mit `--status-offen` — **die Statusplakette der Liste, in
 * derselben Gestalt und über dieselbe Verwendung** (`statusKlassen`).
 *
 * **Sie steht hier und nicht in `features/nachrichten`.** Ein Feature importiert
 * nicht aus einem Nachbarfeature (`docs/frontend-grundlagen.md` §8); geteilt ist
 * deshalb die Farbe in `lib/status-farbe.ts` und nicht die Komponente. Die
 * Plakette der Liste kann mehr, als eine Kachel braucht — Rohwert, unbestätigte
 * Bedeutung, den aktuellen Schritt —, und nichts davon gibt es hier.
 *
 * **Alle drei Werte, also mit Kontur** — anders als bei der Fehlerkachel, wo
 * E‑u zwei nimmt. E‑u galt zwei Kategorien, die nebeneinander stehen und von
 * denen keine lauter sein darf; hier stehen zwei Kacheln **derselben** Rolle
 * nebeneinander, und `--status-offen-kontur` ist mit 1,35 : 1 auf `--card`
 * ohnehin die zurückhaltendste der fünf.
 */
function Plakette({ art }: { art: Extract<Statusart, "LAEUFT" | "WARTEND"> }) {
  const texte = useTexte();
  const Zeichen = ZUSTANDSZEICHEN[art];

  return (
    <Badge
      variant="outline"
      className={cn("h-auto w-fit gap-1.5 border px-2 py-0.5", statusKlassen(art))}
    >
      <Zeichen aria-hidden="true" />
      {texte.einordnung[art]}
    </Badge>
  );
}

/**
 * „Nicht ermittelbar" (Entscheidung E‑q) — **keine `0`, kein Rot, kein
 * Fehlerzustand.**
 *
 * Null hieße „es läuft nichts", und das ist in einem Überwachungswerkzeug die
 * schlimmste falsche Antwort. Die Plakette bleibt stehen, an der Stelle der Zahl
 * steht gedämpfter Text mit einem Zeichen, die Kachel ist **nicht klickbar**,
 * und ein Satz nennt den Grund. Für den Nutzer ist das eine Auskunft und kein
 * technischer Fehler — deshalb keine Fehler-Kennung und keine Schaltfläche
 * „Erneut versuchen": Die übrigen Blöcke stehen ja.
 *
 * **Die beiden Zahlen einer Kachel fallen zusammen**, die beiden **Kacheln**
 * nicht (`docs/dashboard.md` §5): *Läuft* und *Wartend* sind zwei Statements und
 * zwei Auskünfte. Fällt eine, steht die andere.
 *
 * ⚠️ **Sichtbar anders als die fehlende Kachel.** Diese hier steht mit Plakette
 * und Satz da; die strukturell abwesende gibt es gar nicht. Genau darin besteht
 * E‑81.
 */
function NichtErmittelbar() {
  const texte = useTexte();
  return (
    <>
      <p className="flex items-center gap-1.5 opacity-75">
        <CircleOff aria-hidden="true" className="size-5 shrink-0" />
        <span className="text-3xl leading-none font-semibold">
          {texte.dashboard.kacheln.nichtErmittelbar}
        </span>
      </p>
      <p className="text-beiwerk opacity-90">{texte.dashboard.kacheln.nichtErmittelbarHinweis}</p>
    </>
  );
}

/**
 * Die Zählkachel — **neutral und ohne Verweis.**
 *
 * Sie trägt keine Farbrolle: Eine Zahl über den Verkehr ist kein Zustand, und
 * eine Fläche daneben machte aus einer Auskunft eine Meldung
 * (`docs/visuelles-konzept.md` §3).
 */
function NachrichtenKachel({ anzahl }: { anzahl: number }) {
  const texte = useTexte();
  return (
    <Kachel>
      <Kopf zeichen={Mails} titel={texte.dashboard.kacheln.nachrichten} />
      <Zahl wert={anzahl} />
    </Kachel>
  );
}
