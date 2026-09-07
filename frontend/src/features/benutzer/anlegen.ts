import { istRolle, type Anlegedaten } from "./api";
import { passwortBrauchbar } from "./selbstschutz";

/**
 * **Der Entwurf der Anlegemaske und seine Prüfung** — reine Funktionen, frei von
 * React und frei vom Netz, wie `selbstschutz.ts` und `zuordnung.ts` daneben.
 *
 * Eine Bedingung, die nur im Markup steht, ist bei der nächsten Umstellung still
 * weg. Deshalb steht hier, *wann* ein Entwurf abgeschickt werden darf, und im
 * Formular steht nur noch, dass jemand danach fragt.
 *
 * ## Genau ein Mandant (E23)
 *
 * `POST /api/admin/users` nimmt **einen** Mandanten, und seine Signatur wird
 * nicht angefasst (E4). Die Maske bildet das ab, statt intern zu tricksen:
 * Verworfen ist die Mehrfachauswahl, die anschließend `PUT {id}/tenants`
 * nachschöbe — das wären zwei Vorgänge und zwei Protokollzeilen für eine
 * Handlung, und scheiterte der zweite, stünde ein halb angelegtes Konto in der
 * Liste. Weitere Mandanten kommen danach an der Zeile hinzu, und die Maske sagt
 * das selbst.
 *
 * ## Was hier **nicht** geprüft wird
 *
 * **Das Passwort nur auf seine Länge** (E13) — dieselbe Grenze und dieselbe
 * Funktion wie beim Zurücksetzen ({@link passwortBrauchbar}), denn im Backend
 * ist es derselbe Codepfad (`AdminUserService.kodiereEinmalpasswort`). Ob es
 * sich von einem bisherigen unterscheidet, gibt es beim Anlegen gar nicht: Es
 * existiert noch kein gespeicherter Hash.
 *
 * **Und nicht, ob es den Benutzernamen schon gibt.** Das weiß nur die
 * Datenbank, und sie sagt es über `409 benutzername-vergeben`. Eine Vorabprüfung
 * im Browser wäre die zweite Stelle für dieselbe Regel — und sie wäre zwischen
 * Prüfung und Abschicken ohnehin schon veraltet.
 */
export type Entwurf = {
  username: string;
  /** Leer, solange nichts gewählt ist — ein ungewählter Wert ist kein Wert. */
  role: string;
  /** Die Kennung, nicht der Anzeigename: `VOTG`, nicht „VOTG Tanktainer GmbH". */
  mandantId: string;
  initialPassword: string;
};

/**
 * Der leere Entwurf — der Anfangszustand und zugleich der Zustand **nach einem
 * Erfolg** (E25).
 *
 * **Das Passwort steht zuerst, und das ist kein Zufall.** Es ist das eine Feld,
 * das nach dem Abschicken an keiner Stelle mehr stehen darf — nicht in der
 * Antwort, nicht im Protokoll und auch nicht mehr in der Maske, in der ein
 * zweites Konto angelegt wird. Ein Objekt wird in einem Zug gesetzt; die
 * Reihenfolge im Literal sagt, worauf es dabei ankommt.
 */
export const LEERER_ENTWURF: Entwurf = {
  initialPassword: "",
  username: "",
  role: "",
  mandantId: "",
};

/**
 * Der Entwurf als Rumpf der Anfrage — **oder `null`, solange er unvollständig
 * ist.**
 *
 * **Eine Funktion und nicht zwei.** Ein `entwurfBrauchbar` neben einem
 * `rumpfAus` wären zwei Fassungen derselben Bedingung, und die zweite liefe der
 * ersten irgendwann hinterher. So gibt es die Antwort nur einmal: Wer den Rumpf
 * bekommt, darf senden; wer `null` bekommt, nicht — und die Schaltfläche fragt
 * dasselbe ab wie das Absenden.
 *
 * Vier Bedingungen, und jede entspricht einer im Backend:
 *
 * | Feld | hier | dort |
 * |---|---|---|
 * | `username` | nicht leer, auch nicht aus Leerzeichen | `@NotBlank` |
 * | `role` | einer der beiden bekannten Werte | `400 unbekannte-rolle` |
 * | `mandantId` | gewählt | `@NotBlank`, dazu `404` bei unbekannter Kennung |
 * | `initialPassword` | mindestens zwölf Zeichen | `400 passwort-zu-kurz` |
 *
 * **Der Benutzername geht so hinaus, wie er getippt wurde.** Geprüft wird gegen
 * die getrimmte Fassung — das ist genau, was `@NotBlank` verlangt —, gesendet
 * wird der Wert selbst: Eine Eingabe stillschweigend zu verändern wäre die
 * schlechtere der beiden Ehrlichkeiten.
 */
export function anfrageAus(entwurf: Entwurf): Anlegedaten | null {
  if (
    entwurf.username.trim().length === 0 ||
    !istRolle(entwurf.role) ||
    entwurf.mandantId.length === 0 ||
    !passwortBrauchbar(entwurf.initialPassword)
  ) {
    return null;
  }
  return {
    username: entwurf.username,
    role: entwurf.role,
    mandantId: entwurf.mandantId,
    initialPassword: entwurf.initialPassword,
  };
}
