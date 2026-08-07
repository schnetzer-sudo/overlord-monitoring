import { hole } from "@/lib/http";

/**
 * Die Nachrichtenliste und die Prozessauswahl dazu.
 *
 * **Kein Endpunkt hier bekommt eine Mandanten-ID.** Der Mandant kommt aus der
 * Sitzung (Regel M1); es gibt keinen Parameter dafür und es darf keiner
 * entstehen.
 *
 * **Warum die Prozessauswahl hier liegt und nicht in einem eigenen Feature.**
 * Ein Feature importiert nicht aus einem Nachbarfeature. `/api/prozesse` wird
 * heute ausschließlich für den Prozessfilter dieser Liste gebraucht; ein Feature
 * `prozesse` entstünde allein für einen Fetch und müsste sofort von hier
 * importiert werden. Kommt in Schritt 10 eine eigene Prozessansicht, wandert der
 * gemeinsame Teil nach `components/` oder `lib/` — nicht ins Nachbarfeature.
 */

export type Nachricht = {
  messageId: string;
  /** ISO 8601 in UTC. Angezeigt wird er in der Anzeigezone — siehe `lib/format.ts`. */
  zeitpunkt: string;
  /** Der Rohwert des Altsystems, damit er sich gegen die alte Oberfläche halten lässt. */
  status: string;
  /** Die fachliche Einordnung. An ihr macht die Oberfläche Farbe und Symbol fest. */
  statusKind: string;
  /** Bei `UNGEKLAERT`: Der Wert kommt so aus dem Altsystem, seine Bedeutung ist nicht belegt. */
  bedeutungNichtVerifiziert: boolean;
  processId: string;
  /**
   * Darf `null` sein — nicht zugeordnet heißt nicht zugeordnet (Regel Q4).
   *
   * **Ohne eigene Spalte.** `processName` ist nur zufällig lesbar und steht seit
   * der Nachbesserung zu Schritt 4 im Tooltip des Ablaufs; die Spalte trägt
   * `sosName`. Der Freitextfilter durchsucht ihn weiterhin.
   */
  processName: string | null;
  projectName: string | null;
  /**
   * Der Anzeigename des Ablaufs (`SOS.SOSName`) — die Spalte „Ablauf".
   *
   * Durchgängig in Klartext gepflegt (Messung L14), aber trotzdem nullable: Die
   * Produktion muss sich nicht daran halten, was die Testkopie enthält.
   */
  sosName: string | null;
};

/**
 * Die einheitliche Hülle jeder paginierten Antwort.
 *
 * **Es gibt kein `total`.** Eine Gesamtzahl über `Message` wäre die
 * Live-Aggregation, die Regel L2 verbietet. Deshalb auch keine Seitenzahlen: Eine
 * erfundene Gesamtzahl wäre schlimmer als keine.
 */
export type Seite<T> = {
  items: T[];
  /** Undurchsichtig. Gehört **nicht** in die geteilte URL — siehe `filter.ts`. */
  nextCursor: string | null;
  hasMore: boolean;
};

export type Prozess = {
  processId: string;
  processName: string | null;
  projectName: string | null;
};

export const NACHRICHTEN_SCHLUESSEL = {
  /** Der Filter gehört in den Schlüssel: Andere Filter sind andere Daten. */
  liste: (abfrage: string) => ["nachrichten", "liste", abfrage] as const,
  prozesse: ["nachrichten", "prozesse"] as const,
};

export function holeNachrichten(abfrage: string): Promise<Seite<Nachricht>> {
  return hole<Seite<Nachricht>>(`/nachrichten${abfrage}`);
}

export function holeProzesse(): Promise<Prozess[]> {
  return hole<Prozess[]>("/prozesse");
}
