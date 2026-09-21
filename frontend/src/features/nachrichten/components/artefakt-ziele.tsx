"use client";

import Link from "next/link";
import { FileText, ScrollText } from "lucide-react";

import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { artefaktAnsicht } from "@/lib/routen";
import { cn } from "@/lib/utils";

import type { Eigenschaft } from "../api";
import { zusatzzeile, type EigenschaftenOhneZeile } from "../detail";
import type { Artefaktziel } from "../rohdaten";
import { AufklappZeile } from "./aufklapp-zeile";
import { EigenschaftenListe } from "./eigenschaft-zeile";

/**
 * **Die Ziele an einer Zeile** — ein Zeichen je Artefakt, sonst nichts.
 *
 * ## Warum ein Zeichen und keine Zeile
 *
 * Bis zum 18.08.2026 standen die Artefakte in einem eigenen Block unter der
 * Zeitleiste, zweigeteilt nach Nutzdaten und Protokollen und beide Teile nach
 * Schritt geordnet. Im gebauten Zustand ergab das **neun Zeilen mit vier sich
 * wiederholenden Schrittnamen** — denselben, die drei Zeilen darüber in der
 * Zeitleiste schon standen, dort mit Dauer und Balken. Ein Nutzer will nicht
 * „alle Nutzdaten", er will **einen Schritt aufmachen** und sehen, was dort
 * liegt (`docs/rohdaten.md` §3, Entscheidung 6 in der Fassung vom 18.08.2026).
 *
 * Die Ziele sind deshalb **klein und ruhig**: Sie dürfen Name, Dauer und Balken
 * nicht verdrängen. Der Name steht für Vorleseprogramme im `sr-only`-Text und
 * kommt aus derselben Funktion wie die Überschrift der Ansicht (`../rohdaten.ts`
 * `artefaktziel`).
 *
 * ## Der Ausschnitt hat seine sichtbare Marke verloren, und das ist Absicht
 *
 * `beschnittMoeglich` ist **wahr nur bei Protokollen und nur für `MANDANT`**
 * (`api.ts`). Für einen gegebenen Nutzer trifft es damit entweder auf *jedes*
 * Protokoll zu oder auf keines — eine sichtbare Marke an jedem Protokollzeichen
 * sagte dasselbe wie das Zeichen selbst. Angekündigt wird der Ausschnitt
 * weiterhin, und weiterhin **bevor jemand klickt**: im Namen für
 * Vorleseprogramme und im `title` für den Zeiger.
 *
 * ## Es wird nichts ausgegraut
 *
 * Ob hinter einem Protokoll für `MANDANT` etwas Anzeigbares liegt, weiß erst der
 * Abruf — bei `FTPSender` und `HTTPSender` in aller Regel nicht (M63). **Das
 * Ziel wird trotzdem angeboten**; der Zustand erscheint beim Öffnen als einer
 * der vier benannten Texte (`docs/rohdaten.md` §8). Ein ausgegrautes Ziel wäre
 * eine Aussage, die das Backend nicht gemacht hat.
 */
export function Ziele({
  messageId,
  ziele,
  className,
}: {
  messageId: string;
  ziele: Artefaktziel[];
  /** An der aufklappbaren Zeile `pointer-events-auto` (`aufklapp-zeile.tsx`). */
  className?: string;
}) {
  if (ziele.length === 0) {
    return null;
  }

  return (
    // `shrink-0`: Die Ziele geben keine Breite ab. Was bei wenig Platz weicht,
    // ist der gekürzte Name daneben — die Regel aus `nachrichtenliste.md` §8.1.
    <span className={cn("flex shrink-0 items-center gap-0.5", className)}>
      {ziele.map((ziel) => (
        <Ziel key={ziel.artefaktId} messageId={messageId} ziel={ziel} />
      ))}
    </span>
  );
}

/**
 * Ein Ziel: ein Verweis auf die eigene Route der Datei.
 *
 * **Ein echter Verweis, keine Schaltfläche** — „verlinkbar" ist der ganze Grund
 * für die eigene Route (`docs/rohdaten.md` §3, Entscheidung 7), und ohne `<a>`
 * gäbe es weder mittlere Maustaste noch „Adresse kopieren". Ohne
 * Abfragezeichenkette, aus denselben drei Gründen wie bisher
 * (`docs/rohdaten-frontend.md` §8).
 */
function Ziel({ messageId, ziel }: { messageId: string; ziel: Artefaktziel }) {
  const Zeichen = ziel.art === "PROTOKOLL" ? ScrollText : FileText;

  return (
    <Link
      href={artefaktAnsicht(messageId, ziel.artefaktId)}
      title={ziel.titel}
      // `size-8` = 2 rem = `--dichte-bedienelement` am Zeigergerät, und zugleich
      // das Größte, was in eine Zeile von `--dichte-zeile` (2.25 rem) passt.
      // **Am Berührungsgerät bleibt es darunter**, weil die Zeilenhöhe fest ist
      // — dieselbe Lage wie bei den anklickbaren Zeilen der Nachrichtenliste
      // (`nachrichten-tabelle.tsx`, ebenfalls `h-zeile`). Eine Zeile, die am
      // Finger auf 2.75 rem wüchse, wäre eine Leiste mit springenden
      // Zeilenhöhen, und die lässt sich nicht mehr überfliegen.
      className="text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:ring-ring flex size-8 items-center justify-center rounded-md focus-visible:ring-2 focus-visible:outline-none"
    >
      <Zeichen aria-hidden="true" className="size-4" />
      <span className="sr-only">{ziel.name}</span>
    </Link>
  );
}

/**
 * **Eine Zeile für das, was zu keinem Schritt der Zeitleiste gehört.**
 *
 * Zwei Aufrufer, und beide brauchen genau dieselbe Gestalt:
 *
 * | Zeile | Was darin hängt |
 * |---|---|
 * | **Eingang**, über der Zeitleiste | alles auf Schritt `0` — das Paar des Lesedienstes (M57, M73) und seit dem 21.09.2026 die Eigenschaften von Schritt `0`, die nicht mit `Message.` beginnen |
 * | **Ohne Schritt in der Zeitleiste**, darunter | der Rest, den es gemessen nicht gibt (M57, Befund 1) — Artefakte wie Eigenschaften |
 *
 * **Gestrichelte Kontur statt durchgezogener** — dasselbe Vokabular wie die
 * erwartete Zeile am Ende der Zeitleiste (`zeitleiste.tsx`): Was gestrichelt
 * ist, ist kein ausgeführter Schritt. Kein Balken, keine Dauer, keine Farbe;
 * eine Farbrolle gehört den Statusaussagen (`docs/visuelles-konzept.md` §3).
 *
 * ## Seit dem 21.09.2026 trägt sie auch Eigenschaften (E‑222, E‑223)
 *
 * **Es gibt die Zeile, wenn dort ein Artefakt oder eine Eigenschaft liegt;
 * aufklappbar ist sie nur mit Eigenschaften** (`../detail.ts` `zusatzzeile`).
 * Erscheint der Eingang erst mit den Eigenschaften, rutscht die Leiste einmal
 * nach unten — hingenommen und nicht mit einem Platzhalter kaschiert.
 *
 * @param eigenschaften je Position ein Eintrag. Der Eingang reicht genau einen
 *   ohne Überschrift; der Rest je Position einen, und jeder trägt dann den
 *   Rückfall *Schritt N* — **kein erfundener Name**.
 */
export function Zielzeile({
  messageId,
  beschriftung,
  hinweis,
  ziele,
  eigenschaften = [],
  mitUeberschrift = false,
}: {
  messageId: string;
  beschriftung: string;
  hinweis?: string;
  ziele: Artefaktziel[];
  eigenschaften?: readonly EigenschaftenOhneZeile[];
  mitUeberschrift?: boolean;
}) {
  const texte = useTexte();
  const anzahl = eigenschaften.reduce((summe, teil) => summe + teil.eintraege.length, 0);
  const lage = zusatzzeile(ziele.length, anzahl);

  if (!lage.vorhanden) {
    return null;
  }

  return (
    <AufklappZeile
      als="div"
      gestrichelt
      zeilenklasse="min-h-zeile text-muted-foreground"
      namensklasse="text-beiwerk"
      name={beschriftung}
      hinweis={hinweis}
      zieleAnzahl={ziele.length}
      zieleKnoten={<Ziele messageId={messageId} ziele={ziele} className="pointer-events-auto" />}
    >
      {lage.aufklappbar ? (
        <div className="flex min-w-0 flex-col gap-2">
          {eigenschaften.map((teil) => (
            <Teil
              key={teil.position}
              eintraege={teil.eintraege}
              ueberschrift={
                mitUeberschrift
                  ? einsetzen(texte.nachrichten.detail.eigenschaften.gruppeSchritt, {
                      nummer: teil.position,
                    })
                  : null
              }
            />
          ))}
        </div>
      ) : null}
    </AufklappZeile>
  );
}

function Teil({
  eintraege,
  ueberschrift,
}: {
  eintraege: readonly Eigenschaft[];
  ueberschrift: string | null;
}) {
  return (
    <div className="flex min-w-0 flex-col">
      {ueberschrift === null ? null : (
        <p className="text-muted-foreground text-beiwerk font-medium">{ueberschrift}</p>
      )}
      <EigenschaftenListe eintraege={eintraege} />
    </div>
  );
}
