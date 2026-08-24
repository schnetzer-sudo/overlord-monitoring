import { hole, sende } from "@/lib/http";
import type { Mandant } from "@/lib/mandanten";

/**
 * Anmeldung, Sitzung, Passwortwechsel und Mandantenwahl.
 *
 * Ein Feature, weil im Backend beides im Paket `security` liegt und die
 * Selbstauskunft den aktiven Mandanten mitbringt. Zwei Features müssten sich
 * genau diesen Typ teilen — und ein Feature importiert nicht aus einem
 * Nachbarfeature.
 *
 * **Kein Endpunkt hier bekommt eine Mandanten-ID als fachlichen Parameter.** Die
 * einzige Ausnahme ist der Wechsel selbst: Er wählt aus der ohnehin zulässigen
 * Menge aus und bestimmt nicht, *was* gelesen werden darf, sondern nur, *welcher*
 * erlaubte Ausschnitt aktiv ist.
 */

/**
 * Der Typ liegt seit dem 24.08.2026 in `lib/mandanten.ts` und wird hier nur
 * weitergereicht: Die Benutzerverwaltung pflegt die Mandantenmenge fremder
 * Konten und braucht dieselbe Liste, und ein Feature importiert nicht aus einem
 * Nachbarfeature (`docs/frontend-grundlagen.md` §8).
 */
export type { Mandant };

/**
 * Antwort von `GET /api/auth/me` — und dieselbe Antwort nach Anmeldung,
 * Passwortänderung und Mandantenwechsel. Der Zustand muss nie aus mehreren
 * Antworten zusammengesetzt werden.
 *
 * > **`downloadAllowed` ist am 24.08.2026 gestrichen worden.** Hier stand
 * > `downloadAllowed: boolean;`, und die Zeile war seit dem 21.08.2026 eine
 * > Zusage, die das Backend nicht mehr einhält: Schritt 9a hat die Spalte per
 * > Migration entfernt (`docs/benutzerverwaltung.md` E18), und `GET
 * > /api/auth/me` liefert das Feld seither nicht — ein **Vertragsbruch aus
 * > Schritt 3**, dort datiert vermerkt.
 * >
 * > Sie stehen zu lassen war beim Backend-Auftrag richtig, weil der Frontend-Teil
 * > nicht zu seinem Umfang gehörte; sie **jetzt** stehen zu lassen wäre es nicht.
 * > Gelesen wurde sie nie — nicht in einer Bedingung, nicht in einem
 * > Objektliteral —, der Download hängt am Inhalt des Artefakts und nicht an
 * > einem Nutzerflag (`docs/rohdaten.md` §3 E2). Eine Typzeile ohne Wert
 * > dahinter ist genau die Art Angabe, auf die sich später jemand verlässt.
 */
export type Selbstauskunft = {
  username: string;
  /** `ADMIN` oder `MANDANT`. Steuert ausschließlich Bequemlichkeit im Menü. */
  role: string;
  /** `null`, solange keiner gewählt ist — kein Fehler, sondern eine offene Auswahl. */
  mandant: Mandant | null;
  mustChangePassword: boolean;
  /**
   * IANA-Kennung der Zone, in der Zeitstempel **anzuzeigen** sind, etwa
   * `Europe/Berlin`.
   *
   * Sie kommt vom Backend und nicht aus einer Konstante hier: Es ist dieselbe
   * Zone, mit der dort die Wanduhrzeit der Quelle nach UTC umgerechnet wird.
   * Zwei Stellen liefen beim Umzug des Servers auseinander — und zwar lautlos,
   * weil eine um Stunden verschobene Uhrzeit plausibel aussieht. Verwendet wird
   * sie über `components/zeitzone.tsx`; warum überhaupt eine feste Zone und
   * nicht die des Browsers, steht in `lib/format.ts`.
   */
  anzeigezone: string;
};

export type Anmeldedaten = { username: string; password: string };
export type Passwortdaten = { oldPassword: string; newPassword: string };

export const SITZUNG_SCHLUESSEL = {
  selbstauskunft: ["sitzung", "selbstauskunft"] as const,
};

export function holeSelbstauskunft(): Promise<Selbstauskunft> {
  return hole<Selbstauskunft>("/auth/me");
}

export function meldeAn(daten: Anmeldedaten): Promise<Selbstauskunft> {
  return sende<Selbstauskunft>("/auth/login", daten);
}

export function meldeAb(): Promise<void> {
  return sende<void>("/auth/logout");
}

export function aenderePasswort(daten: Passwortdaten): Promise<Selbstauskunft> {
  return sende<Selbstauskunft>("/auth/password", daten);
}

export function wechsleMandant(mandantId: string): Promise<Selbstauskunft> {
  return sende<Selbstauskunft>("/auth/mandant", { mandantId });
}
