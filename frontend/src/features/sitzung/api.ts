import { hole, sende } from "@/lib/http";

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

export type Mandant = {
  /** Sprechender Code aus dem Altsystem, etwa `VOTG` — keine UUID. */
  id: string;
  name: string;
};

/**
 * Antwort von `GET /api/auth/me` — und dieselbe Antwort nach Anmeldung,
 * Passwortänderung und Mandantenwechsel. Der Zustand muss nie aus mehreren
 * Antworten zusammengesetzt werden.
 */
export type Selbstauskunft = {
  username: string;
  /** `ADMIN` oder `MANDANT`. Steuert ausschließlich Bequemlichkeit im Menü. */
  role: string;
  /** `null`, solange keiner gewählt ist — kein Fehler, sondern eine offene Auswahl. */
  mandant: Mandant | null;
  mustChangePassword: boolean;
  downloadAllowed: boolean;
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
  mandanten: ["sitzung", "mandanten"] as const,
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

/** Genau die Menge, aus der dieser Nutzer wählen darf — die Liste ist selbst eine Auskunft. */
export function holeMandanten(): Promise<Mandant[]> {
  return hole<Mandant[]>("/mandanten");
}

export function wechsleMandant(mandantId: string): Promise<Selbstauskunft> {
  return sende<Selbstauskunft>("/auth/mandant", { mandantId });
}
