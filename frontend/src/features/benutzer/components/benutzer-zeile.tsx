"use client";

import { Marke } from "@/components/marke";
import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt } from "@/lib/format";
import { cn } from "@/lib/utils";

import { istRolle, type Nutzerzeile } from "../api";

/**
 * Eine Zeile der Kontenliste in ihrer **Anzeigeform** — die Zellen, nicht die
 * Bedienung. Was sich ändern lässt, steht im Formular unter der Zeile
 * (`zeilen-formular.tsx`).
 *
 * ## Die zwei Sperren stehen in zwei Spalten und werden nie zu einer
 *
 * Das ist die tragende Entscheidung dieser Zeile (E14, E20):
 *
 * | Spalte | Was sie sagt | Wie sie endet |
 * |---|---|---|
 * | **Sperre** | ein **Verwaltungsakt** — jemand hat dieses Konto gesperrt | nur durch einen zweiten Verwaltungsakt |
 * | **Zeitsperre** | **fünf Fehlversuche** — das Konto hat sich selbst ausgesperrt | von selbst, nach fünfzehn Minuten |
 *
 * Sie zu einer Spalte zusammenzufassen wäre nicht knapper, sondern falsch: Ein
 * Admin, den ein Nutzer anruft, weil er nicht hineinkommt, muss zwischen „ich
 * habe dich gesperrt" und „du hast dich fünfmal vertippt" unterscheiden können —
 * die beiden haben verschiedene Ursachen und verschiedene Behebungen. Eine
 * gemeinsame Spalte nähme genau die Auskunft weg, für die `lockedUntil`
 * überhaupt gebaut wurde.
 *
 * **Und nie durch Farbe allein.** Diese Zeile führt gar keine Farbrolle: Die
 * vier Statusfarben des Projekts sind fachlich an den **Nachrichtenstatus**
 * vergeben (`docs/visuelles-konzept.md` §3), und *„es gibt keine zweite
 * Bedeutung von Grün oder Rot"*. Die Unterscheidung liegt vollständig im Wort;
 * gedämpft wird nur der ruhige Fall.
 *
 * ## `lastLogin = null` heißt „nie angemeldet" und wird ausgeschrieben
 *
 * Keine leere Zelle. Bei über zwanzig externen Nutzern ist *„hat der sich
 * überhaupt je angemeldet"* die häufigste Supportfrage (E17), und eine leere
 * Zelle beantwortet sie nicht — sie sieht aus wie eine fehlende Angabe. Das ist
 * dieselbe Regel, nach der die Katalogpflege `false` und `null` auseinanderhält:
 * **„nichts da" und „nicht erhoben" sind zwei Auskünfte.**
 *
 * ## Die Mandanten sind Kennungen und keine Anzeigenamen
 *
 * `VOTG`, nicht „VOTG Tanktainer GmbH" — bewusst, denn die Kennung *ist* der
 * sprechende Code, den auch `POST /api/admin/users` entgegennimmt, und die Namen
 * lägen in `GlassfishDB.Mandant` hinter einem schemaübergreifenden Join je Zeile.
 * Sie stehen als **Marke**, der einen Gestalt des Projekts
 * (`components/marke.tsx`) — kein Knopf, keine Statusfarbe.
 */
export function BenutzerZeile({
  zeile,
  aktionen,
}: {
  zeile: Nutzerzeile;
  aktionen?: React.ReactNode;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const zelle = "px-2 py-1.5 align-top";

  return (
    <>
      <td className={cn(zelle, "font-mono break-all")}>{zeile.username}</td>

      {/*
       * Ein unbekannter Rollenwert wird **roh** gezeigt und nicht auf einen der
       * beiden bekannten gebogen. Das Backend führt genau zwei; käme je ein
       * dritter, wäre eine stille Zuordnung auf „Mandant" die schlechteste
       * denkbare Auskunft an einer Berechtigungsanzeige.
       */}
      <td className={cn(zelle, "hidden md:table-cell")}>
        {istRolle(zeile.role) ? texte.rolle[zeile.role] : zeile.role}
      </td>

      <td className={cn(zelle, "hidden md:table-cell")}>
        {zeile.tenants.length === 0 ? (
          <span className="text-muted-foreground">{texte.benutzer.ohneMandanten}</span>
        ) : (
          <span className="flex flex-wrap gap-1">
            {zeile.tenants.map((kennung) => (
              <Marke key={kennung} className="font-mono">
                {kennung}
              </Marke>
            ))}
          </span>
        )}
      </td>

      <td className={zelle}>
        <span className={cn(!zeile.locked && "text-muted-foreground")}>
          {zeile.locked ? texte.benutzer.sperre.gesperrt : texte.benutzer.sperre.offen}
        </span>
      </td>

      {/*
       * Der Zeitpunkt wird nur **dargestellt**. Ob die Sperre noch läuft, hat das
       * Backend schon entschieden — mit der Systemuhr und nicht mit der des
       * Browsers (`docs/PROJEKTBESCHREIBUNG.md` §7). Ein `lockedUntil` in der
       * Antwort heißt: sie läuft. Hier wird deshalb nichts gegen `Date.now()`
       * verglichen; ein verstellter Rechner soll keine falsche Auskunft erzeugen.
       */}
      <td className={zelle}>
        {zeile.lockedUntil === null ? (
          <span className="text-muted-foreground">{texte.benutzer.zeitsperre.keine}</span>
        ) : (
          <span>
            {einsetzen(texte.benutzer.zeitsperre.bis, {
              zeitpunkt: formatiereZeitpunkt(zeile.lockedUntil, sprache, zone),
            })}
          </span>
        )}
      </td>

      <td className={zelle}>
        <span className={cn(zeile.active && "text-muted-foreground")}>
          {zeile.active ? texte.benutzer.aktiv.aktiv : texte.benutzer.aktiv.deaktiviert}
        </span>
      </td>

      <td className={cn(zelle, "hidden lg:table-cell")}>
        <span className={cn(!zeile.mustChangePassword && "text-muted-foreground")}>
          {zeile.mustChangePassword
            ? texte.benutzer.passwort.wechselNoetig
            : texte.benutzer.passwort.keinWechsel}
        </span>
      </td>

      <td className={cn(zelle, "hidden lg:table-cell")}>
        {zeile.lastLogin === null ? (
          <span className="text-muted-foreground">{texte.benutzer.anmeldung.nie}</span>
        ) : (
          formatiereZeitpunkt(zeile.lastLogin, sprache, zone)
        )}
      </td>

      <td className={cn(zelle, "text-right")}>{aktionen}</td>
    </>
  );
}
