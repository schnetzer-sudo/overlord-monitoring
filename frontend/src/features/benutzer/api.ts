import { hole } from "@/lib/http";

/**
 * Die Benutzerverwaltung: **die eine Fläche, auf der Konten gepflegt werden**
 * (`docs/benutzerverwaltung.md`).
 *
 * Sie wird gebaut, weil über zwanzig externe Nutzer absehbar sind, die keinen
 * Datenbankzugang haben und niemals bekommen werden (E1). Jedes vergessene
 * Passwort, jede Sperre, jeder Austritt landete sonst per Zuruf beim Betreiber.
 *
 * ## Diese Liste ist **mandantenfrei** — die eine Ansicht, für die das gilt
 *
 * `app_user` liegt in `overlord_monitor`, und Regel M2 bindet nur Repositories
 * auf `jooq.glassfish`. Die Liste zeigt **alle** Konten, unabhängig vom aktiven
 * Mandanten (E2). Das ist nicht die bequemere, sondern die richtige Liste: Ein
 * ADMIN-Konto trägt eine wirkungslose Mandantenzuordnung und stünde gefiltert
 * unter einem Mandanten, für den es nicht gilt.
 *
 * **Regel M1 gilt trotzdem, mit genau einer benannten Ausnahme.** Kein Aufruf
 * hier bekommt eine Mandanten-ID als *fachlichen Parameter*; die Ausnahme ist
 * `PUT /api/admin/users/{id}/tenants` und sie ist die **dritte und derzeit
 * letzte** von M1 (E4). Sie ist zulässig aus demselben Grund wie die beiden
 * anderen: Dort wird eine Berechtigung *definiert* und kein Datenausschnitt
 * *abgefragt*.
 *
 * ## Dieses Feature importiert nicht aus einem Nachbarfeature
 *
 * Nicht aus `features/katalog`, nicht aus `features/sitzung`
 * (`docs/frontend-grundlagen.md` §8). Was es von dort bräuchte, kommt über die
 * Naht: die wählbaren Mandanten aus `lib/mandanten.ts`, der eigene
 * Benutzername aus `components/angemeldet.tsx`, die Anzeigezone aus
 * `components/zeitzone.tsx`.
 *
 * **`/api/admin/**` verlangt die Rolle `ADMIN`.** Ein Nutzer ohne sie bekommt
 * `403` mit dem Problemtyp `zugriff-verweigert`, und darauf gibt es keinen
 * zweiten Versuch (`lib/query-client.ts`). Die Ansicht zeigt dafür einen eigenen
 * Zustand statt einer roten Meldung — dieselbe Bauform wie die Katalogpflege.
 */

/** Die geschlossene Menge aus `security/Rolle`. */
export const ROLLEN = ["ADMIN", "MANDANT"] as const;

export type Rolle = (typeof ROLLEN)[number];

export function istRolle(wert: string): wert is Rolle {
  return (ROLLEN as readonly string[]).includes(wert);
}

/**
 * Eine Zeile der Kontenliste — **neun Felder, so wie das Backend sie liefert**,
 * und dieselbe Antwort nach jedem der fünf schreibenden Vorgänge
 * (`docs/benutzerverwaltung-backend.md` §4).
 *
 * Der Aufrufer muss seinen Zustand nie aus mehreren Antworten zusammensetzen.
 */
export type Nutzerzeile = {
  id: number;
  username: string;
  /** `ADMIN` oder `MANDANT`. Als Zeichenkette gelesen — ein unbekannter Wert bleibt lesbar. */
  role: string;
  /**
   * Die Mandanten**kennungen**, aufsteigend — keine Anzeigenamen.
   *
   * Bewusst so: Die Namen lägen in `GlassfishDB.Mandant` und kosteten einen
   * schemaübergreifenden Join je Zeile, während die Kennung selbst der
   * sprechende Code ist (`VOTG`, `NEXANS`), den auch `POST /api/admin/users`
   * entgegennimmt.
   *
   * **Bei einem ADMIN steht hier ebenfalls etwas, und es ist wirkungslos** — ein
   * Administrator ist für alle Mandanten berechtigt. Geführt wird die Zuordnung
   * trotzdem, damit sie bei einer späteren Herabstufung nicht ins Leere fällt
   * (E10).
   */
  tenants: string[];
  /**
   * Die **administrative** Sperre — die, die der Umschalter je Zeile setzt.
   * Unbefristet, aufgehoben nur durch einen zweiten Verwaltungsakt.
   */
  locked: boolean;
  /**
   * Das Ende der **automatischen** Sperre nach fünf Fehlversuchen, UTC — und
   * `null`, sobald es nicht mehr in der Zukunft liegt (E20).
   *
   * **Wird niemals mit {@link locked} verrechnet.** Zwei Sperren mit
   * verschiedener Ursache und verschiedener Behebung: Die eine läuft nach
   * fünfzehn Minuten von selbst ab, die andere hebt nur ein Verwaltungsakt auf.
   * Sie zusammenzufassen ließe E14 zusammenfallen — und nähme genau die Auskunft
   * weg, für die das Feld existiert: Ein Admin, den jemand anruft, weil er nicht
   * hineinkommt, muss zwischen „ich habe dich gesperrt" und „du hast dich
   * fünfmal vertippt" unterscheiden können.
   *
   * **Die Prüfung „liegt in der Zukunft" ist schon gelaufen, im Backend.** Ob
   * eine Sperre noch gilt, ist sicherheitsnahe Zeit und rechnet mit der
   * Systemuhr (`docs/PROJEKTBESCHREIBUNG.md` §7); ein verstellter Rechner sähe
   * sonst eine abgelaufene Sperre als laufende. **Die Oberfläche stellt nur
   * dar** und vergleicht hier nichts gegen die Browseruhr.
   */
  lockedUntil: string | null;
  active: boolean;
  mustChangePassword: boolean;
  /**
   * Die letzte **erfolgreiche** Anmeldung, UTC — und `null` heißt **„noch nie
   * angemeldet"** (E17).
   *
   * `null` wird ausgeschrieben und nicht als leere Zelle gezeigt: Bei über
   * zwanzig externen Nutzern ist *„hat der sich überhaupt je angemeldet"* die
   * häufigste Supportfrage, und eine leere Zelle beantwortet sie nicht — sie
   * sieht aus wie eine fehlende Angabe.
   */
  lastLogin: string | null;
};

export const BENUTZER_SCHLUESSEL = {
  /**
   * **Ohne Mandant im Schlüssel**, und das ist hier keine Auslassung, sondern
   * E2: Die Liste ist mandantenfrei. Beim Mandantenwechsel wird der gesamte
   * Zwischenspeicher ohnehin geleert (`lib/zwischenspeicher.ts`) — die Liste
   * wird danach neu geholt und ist dieselbe.
   */
  liste: ["benutzer", "liste"] as const,
};

/**
 * Alle Konten. **Ein Fetch, keine Paginierung, keine serverseitige Suche**
 * (E16) — Größenordnung dreißig Konten, jede Mechanik dafür wäre Beiwerk.
 *
 * **Sortiert nach Benutzername, und die Reihenfolge kommt vom Backend.** Er ist
 * eindeutig, damit ist sie auch ohne Paginierung stabil. Hier wird nicht
 * umsortiert.
 */
export function holeNutzer(): Promise<Nutzerzeile[]> {
  return hole<Nutzerzeile[]>("/admin/users");
}
