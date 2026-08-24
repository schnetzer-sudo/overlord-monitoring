"use client";

import { Fragment } from "react";

import { useTexte } from "@/i18n/provider";

import type { Nutzerzeile } from "../api";
import { BenutzerZeile } from "./benutzer-zeile";

/**
 * Die Kontenliste als Tabelle — **dreißig Zeilen, kein Blättern, keine Suche**
 * (E16).
 *
 * ## Gebaut wie die Pflegeliste, und das ist kein Zufall
 *
 * `docs/prozess-katalog-frontend.md` §3 hat die Bauform für dieses Projekt
 * festgelegt, und sie gilt hier unverändert — der Auftrag zu 9a sagt
 * ausdrücklich, dass **ergänzt und nicht danebengebaut** wird:
 *
 * 1. **Kein zweites `overflow-y-auto`.** Der einzige Scrollbereich bleibt das
 *    `main` des Anwendungsrahmens. Die Kopfzeile klebt trotzdem, weil `sticky`
 *    keinen eigenen Scrollcontainer braucht — sie hängt sich an den nächsten.
 * 2. **Kein `overflow-x-auto` darüber.** Das ist die Stelle, an der es sonst
 *    still kippt: `overflow-x: auto` stuft `overflow-y` auf `auto` hoch, der
 *    Container wird selbst zum Scrollbereich, und die klebende Kopfzeile klebt an
 *    ihm statt am Fenster. Deshalb steht hier **kein `<Table>` aus
 *    `components/ui`** — der Baustein bringt genau diesen Container mit.
 * 3. **Keine Höhe am Fenster**, kein `h-dvh`, kein `min-h-screen`.
 * 4. **`border-separate`**, weil mit `border-collapse` die Rahmen der Tabelle
 *    gehören und die klebende Kopfzeile ihren Trennstrich zurückließe.
 * 5. **`-top-4` und nicht `top-0`** — `main` trägt `py-4`, und eine klebende
 *    Zelle mit `top-0` bliebe einen Innenabstand zu tief stehen. Am 24.08.2026
 *    an der Pflegeliste nachgemessen und dort begründet; die Zahl gehört zum
 *    Rahmen und nicht zu dieser Tabelle.
 *
 * ## Welche Spalte wann weicht — und die drei, die nie weichen
 *
 * Die Frage ist nicht „was passt", sondern „was sucht der Nutzer"
 * (`docs/frontend-grundlagen.md` §8). Diese Seite wird geöffnet, weil jemand
 * anruft und nicht hineinkommt. Sichtbar bleiben deshalb bei **jeder** Breite
 * die drei Spalten, die genau diese Frage beantworten — **Sperre**,
 * **Zeitsperre** und **Aktiv** —, dazu der Benutzername und der Zugang zum
 * Formular.
 *
 * | Spalte | ab |
 * |---|---|
 * | Benutzer, Sperre, Zeitsperre, Aktiv, Bearbeiten | immer |
 * | Rolle, Mandanten | `md` |
 * | Passwortwechsel, letzte Anmeldung | `lg` |
 *
 * **Nichts wird dadurch unerreichbar.** Alles Bedienbare steht im Formular unter
 * der Zeile und nicht in den Zellen — genau aus diesem Grund
 * (`zeilen-formular.tsx`). Ausgeblendet wird nur Anzeige.
 *
 * ## Die Reihenfolge kommt vom Backend
 *
 * Nach Benutzername, und er ist eindeutig — damit ist die Reihenfolge auch ohne
 * Paginierung stabil. **Hier wird nicht umsortiert.** Eine Spaltensortierung wäre
 * eine neue Entscheidung und keine Ausbaustufe.
 */
const KOPFZELLE =
  "bg-background border-border sticky -top-4 z-10 border-b px-2 py-1.5 text-left align-bottom font-medium";

export function BenutzerTabelle({
  zeilen,
  aktionenFuer,
  formularFuer,
}: {
  zeilen: readonly Nutzerzeile[];
  /** Was rechts in der Zeile steht. In Teil 3 nichts, ab Teil 4 der Öffnen-Knopf. */
  aktionenFuer?: (zeile: Nutzerzeile) => React.ReactNode;
  /** Das aufgeklappte Formular unter genau einer Zeile — oder `null`. */
  formularFuer?: (zeile: Nutzerzeile) => React.ReactNode;
}) {
  const texte = useTexte();

  return (
    <table className="text-basis [&_td]:border-border w-full table-fixed border-separate border-spacing-0 [&_td]:border-b">
      <caption className="sr-only">{texte.benutzer.tabelle}</caption>
      <thead>
        <tr>
          <th scope="col" className={`${KOPFZELLE} w-[8rem] md:w-[11rem]`}>
            {texte.benutzer.spalten.benutzer}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[9rem] md:table-cell`}>
            {texte.benutzer.spalten.rolle}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden md:table-cell`}>
            {texte.benutzer.spalten.mandanten}
          </th>
          <th scope="col" className={`${KOPFZELLE} w-[6rem] md:w-[7.5rem]`}>
            {texte.benutzer.spalten.sperre}
          </th>
          <th scope="col" className={`${KOPFZELLE} w-[7rem] md:w-[11rem]`}>
            {texte.benutzer.spalten.zeitsperre}
          </th>
          <th scope="col" className={`${KOPFZELLE} w-[6rem] md:w-[7.5rem]`}>
            {texte.benutzer.spalten.aktiv}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[9rem] lg:table-cell`}>
            {texte.benutzer.spalten.passwort}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[11rem] lg:table-cell`}>
            {texte.benutzer.spalten.letzteAnmeldung}
          </th>
          <th scope="col" className={`${KOPFZELLE} w-[3.5rem] text-right`}>
            <span className="sr-only">{texte.benutzer.spalten.aktionen}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {zeilen.map((zeile) => {
          const formular = formularFuer?.(zeile) ?? null;
          return (
            /*
             * Der Schlüssel ist die Konto-`id` und nicht der Benutzername: Er ist
             * in der Datenbank zwar eindeutig, aber ohne Rücksicht auf Groß- und
             * Kleinschreibung — und was React vergleicht, ist die Zeichenkette.
             * Die `id` ist der Schlüssel, den das Backend selbst benutzt.
             */
            <Fragment key={zeile.id}>
              <tr className={formular === null ? "hover:bg-muted/50" : "bg-muted/50"}>
                <BenutzerZeile zeile={zeile} aktionen={aktionenFuer?.(zeile)} />
              </tr>
              {formular === null ? null : (
                <tr className="bg-muted/50">
                  <td colSpan={9}>{formular}</td>
                </tr>
              )}
            </Fragment>
          );
        })}
      </tbody>
    </table>
  );
}
