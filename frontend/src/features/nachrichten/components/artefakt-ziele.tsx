"use client";

import Link from "next/link";
import { FileText, ScrollText } from "lucide-react";

import { artefaktAnsicht } from "@/lib/routen";

import type { Artefaktziel } from "../rohdaten";

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
export function Ziele({ messageId, ziele }: { messageId: string; ziele: Artefaktziel[] }) {
  if (ziele.length === 0) {
    return null;
  }

  return (
    // `shrink-0`: Die Ziele geben keine Breite ab. Was bei wenig Platz weicht,
    // ist der gekürzte Name daneben — die Regel aus `nachrichtenliste.md` §8.1.
    <span className="flex shrink-0 items-center gap-0.5">
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
 * **Eine Zeile für Artefakte, die zu keinem Schritt der Zeitleiste gehören.**
 *
 * Zwei Aufrufer, und beide brauchen genau dieselbe Gestalt:
 *
 * | Zeile | Was darin hängt |
 * |---|---|
 * | **Eingang**, über der Zeitleiste | alles auf Schritt `0` — seit dem 19.08.2026 das Paar des Lesedienstes, Datei und Protokoll (M57, M73) |
 * | **Ohne Schritt in der Zeitleiste**, darunter | der Rest, den es gemessen nicht gibt (M57, Befund 1) |
 *
 * **Gestrichelte Kontur statt durchgezogener** — dasselbe Vokabular wie die
 * erwartete Zeile am Ende der Zeitleiste (`zeitleiste.tsx`): Was gestrichelt
 * ist, ist kein ausgeführter Schritt. Kein Balken, keine Dauer, keine Farbe;
 * eine Farbrolle gehört den Statusaussagen (`docs/visuelles-konzept.md` §3).
 */
export function Zielzeile({
  messageId,
  beschriftung,
  hinweis,
  ziele,
}: {
  messageId: string;
  beschriftung: string;
  hinweis?: string;
  ziele: Artefaktziel[];
}) {
  if (ziele.length === 0) {
    return null;
  }

  return (
    <div className="border-border text-muted-foreground min-h-zeile flex items-center gap-2 border-l-2 border-dashed pl-2">
      <span className="text-beiwerk min-w-0 flex-1 truncate" title={hinweis}>
        {beschriftung}
      </span>
      <Ziele messageId={messageId} ziele={ziele} />
    </div>
  );
}
