import type { Rolle } from "./api";

/**
 * **Was ein Vorgang ist, und wann er das eigene Konto trifft.**
 *
 * Frei von React, wie `features/nachrichten/filter.ts` und
 * `features/katalog/zuordnung.ts`: Die Regeln hier sind reine Funktionen und
 * werden als solche geprüft. Eine Bedingung, die nur im Markup steht, ist bei
 * der nächsten Umstellung still weg.
 */

/**
 * Ein schreibender Vorgang auf **eine** Zeile — und zugleich der Rumpf, den er
 * schickt.
 *
 * **Ein Typ und nicht fünf**, weil genau das die Reihenfolge trägt, um die es
 * hier geht: Erst steht fest, *was* getan werden soll, dann wird geprüft, ob es
 * eine Vorwarnung braucht, dann läuft es. Fünf getrennte Aufrufwege hätten fünf
 * Stellen, an denen die Prüfung vergessen werden kann — und E19 wäre dann eine
 * Aufzählung statt einer Regel.
 */
export type Vorgang =
  | { art: "sperre"; gesperrt: boolean }
  | { art: "aktiv"; aktiv: boolean }
  | { art: "rolle"; rolle: Rolle }
  | { art: "mandanten"; mandanten: readonly string[] }
  | { art: "passwort"; passwort: string };

/**
 * Ob eine Zeile das Konto des Angemeldeten ist — **verglichen über den
 * Benutzernamen, ohne Rücksicht auf Groß- und Kleinschreibung** (E19).
 *
 * ## Warum über den Namen und nicht über die `id`
 *
 * Weil es keine gibt: **`GET /api/auth/me` liefert keine `id`.** Die
 * Selbstauskunft trägt `username`, `role`, `mandant`, `mustChangePassword` und
 * `anzeigezone` — die Verwaltungsliste führt daneben eine `id`, aber es gibt
 * keinen Wert, über den sich die beiden verknüpfen ließen außer dem Namen. Die
 * Selbstauskunft dafür zu erweitern ist verworfen (E19): Sie ist seit Schritt 3
 * ein Vertrag, der in 9a schon einmal gebrochen worden ist.
 *
 * ## Warum ohne Rücksicht auf die Schreibweise
 *
 * **Weil die Anmeldung sie ebenfalls nicht beachtet.** `app_user.username`
 * trägt `utf8mb4_general_ci` und vergleicht in der Datenbank ohne
 * Schreibungsrücksicht — ein `===` in JavaScript tut das nicht. Ein Konto
 * `Admin`, das sich als `admin` anmeldet, sähe seine eigene Zeile sonst als
 * fremde und bekäme die Warnung **genau im Grenzfall nicht, für den sie da
 * ist.**
 *
 * Die Kleinschreibung beider Seiten ist unter jeder möglichen Quelle richtig
 * und braucht keine Vertragsänderung an `/api/auth/me`.
 *
 * `toLowerCase` und **nicht** `toLocaleLowerCase`: Letzteres hängt an der
 * Spracheinstellung — im Türkischen wird aus `I` ein `ı` und nicht `i`. Der
 * Vergleich soll überall dasselbe ergeben wie die Datenbank, und nicht das,
 * was der Browser gerade eingestellt hat.
 *
 * @param angemeldet der Name aus der Selbstauskunft. `undefined`, solange sie
 *   lädt — dann ist die Antwort **`false`**, und der Hinweis bleibt aus, statt
 *   falsch zu erscheinen.
 */
export function istEigenesKonto(username: string, angemeldet: string | undefined): boolean {
  return angemeldet !== undefined && username.toLowerCase() === angemeldet.toLowerCase();
}

/**
 * Ob dieser Vorgang **am eigenen Konto überhaupt durchläuft** — am Backend
 * abgelesen und nicht geraten.
 *
 * `BenutzerverwaltungService.pruefeEntwertung` wirft `409 selbstschutz`, sobald
 * das Ziel das eigene Konto ist. Sie läuft aber **nicht bei jedem Aufruf**,
 * sondern nur in der *entwertenden* Richtung — und das ist der ganze Inhalt
 * dieser Funktion:
 *
 * | Vorgang | Richtung | am eigenen Konto |
 * |---|---|---|
 * | `sperre` | sperren | **409** — `selbstschutz` (oder `letzter-admin`, siehe unten) |
 * | `sperre` | entsperren | läuft durch |
 * | `aktiv` | deaktivieren | **409** |
 * | `aktiv` | reaktivieren | läuft durch |
 * | `rolle` | auf `MANDANT` (herabstufen) | **409** |
 * | `rolle` | auf `ADMIN` | läuft durch |
 * | `mandanten` | — | läuft durch, **kein Selbstschutz** |
 * | `passwort` | — | läuft durch, **kein Selbstschutz** (ausdrücklich: für das eigene Passwort gibt es `POST /api/auth/password`, und wer hier tippt, kennt das eben getippte) |
 *
 * > **Zwei Anmerkungen, die am Bestand hängen und nicht an dieser Datei.**
 * >
 * > **Erstens:** Wer angemeldet ist, ist per Definition weder administrativ
 * > gesperrt noch deaktiviert — `AnmeldeService` weist beides vor der
 * > Sitzung ab. Die eigene Zeile trägt deshalb im Betrieb immer
 * > `locked: false` und `active: true`, und die *durchlaufenden* Richtungen
 * > `entsperren` und `reaktivieren` sind dort Leerläufe: Sie schreiben den
 * > Zustand, der ohnehin gilt. Spürbar ist an ihnen allein E5 — der
 * > Sitzungsentzug. Genau deshalb stehen sie hier trotzdem als „läuft durch":
 * > Die Regel folgt der **Richtung** und nicht der Erreichbarkeit, und sie
 * > bleibt damit auch dann richtig, wenn eine veraltete Liste einmal etwas
 * > anderes zeigt.
 * >
 * > **Zweitens:** Am eigenen Konto können *beide* `409` fallen. `pruefeEntwertung`
 * > prüft **zuerst** den letzten nutzbaren Administrator und **danach** das
 * > eigene Konto; wer als einziger nutzbarer Admin angemeldet ist, bekommt
 * > `letzter-admin`. Das ist der Grund, warum die Oberfläche diese Fälle
 * > **auffängt statt sie vorwegzunehmen**: Ein selbst gebauter Riegel „nicht am
 * > eigenen Konto" zeigte den *falschen* der beiden Sätze — nämlich den, der
 * > die Bedingung verschweigt, unter der es ginge.
 */
export function amEigenenKontoZulaessig(vorgang: Vorgang): boolean {
  switch (vorgang.art) {
    case "sperre":
      return !vorgang.gesperrt;
    case "aktiv":
      return vorgang.aktiv;
    case "rolle":
      return vorgang.rolle !== "MANDANT";
    case "mandanten":
    case "passwort":
      return true;
  }
}

/**
 * **Braucht dieser Vorgang die Vorwarnung aus E19?**
 *
 * Genau dann, wenn er das eigene Konto trifft **und** dort durchläuft.
 *
 * Der Grund ist E5, und die Regel hat keine Fallunterscheidung: **Jeder** der
 * fünf schreibenden Vorgänge verwirft *alle* Sitzungen des betroffenen Kontos —
 * auch die des Handelnden. Trifft es das eigene Konto, meldet der Admin sich mit
 * dem Klick selbst ab und landet **wortlos** auf der Anmeldung: Bei `401` wird
 * umgeleitet und nicht gemeldet (`docs/frontend-grundlagen.md` §5). Ohne
 * Vorwarnung sähe das aus wie ein Absturz.
 *
 * **Für die verbotenen Richtungen gibt es bewusst keine Warnung**, sondern die
 * Übersetzung des Problemtyps aus der Antwort. Eine Warnung dort verspräche,
 * dass es nach dem Bestätigen passiert — und es passiert nicht.
 */
export function brauchtVorwarnung(
  vorgang: Vorgang,
  username: string,
  angemeldet: string | undefined,
): boolean {
  return istEigenesKonto(username, angemeldet) && amEigenenKontoZulaessig(vorgang);
}

/** Die Mindestlänge eines Einmalpassworts (E13) — dieselbe Zahl wie im Backend. */
export const PASSWORT_MINDESTLAENGE = 12;

/**
 * Ob das getippte Einmalpasswort abgeschickt werden darf.
 *
 * **Nur die Länge**, und das ist die Grenze zwischen den beiden Prüfungen: Ob es
 * sich vom bisherigen unterscheidet, kann der Browser nicht wissen — das prüft
 * das Backend per BCrypt-Vergleich gegen den gespeicherten Hash und antwortet
 * mit `passwort-unveraendert`. Ein Nachbau hier wäre nicht nur unmöglich, er
 * wäre auch die zweite Stelle für dieselbe Regel.
 */
export function passwortBrauchbar(passwort: string): boolean {
  return passwort.length >= PASSWORT_MINDESTLAENGE;
}
