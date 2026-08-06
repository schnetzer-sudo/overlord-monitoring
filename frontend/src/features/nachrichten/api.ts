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

/** Eine BAM-Spalte einer Zeile. Jede Zeile trägt so viele, wie der Mandant hat. */
export type BamWerte = {
  typ: number;
  /**
   * Der lesbare Name des Typs — **er ist zugleich die Spaltenüberschrift**. Sie
   * kommt aus der Antwort und nicht aus einer festen Liste: Welche zwei BAM-Typen
   * ein Mandant sieht, entscheidet seine Konfiguration.
   */
  beschreibung: string;
  werte: string[];
  /** Wie viele Werte nicht mitgekommen sind. Eine stumm gekürzte Liste sieht aus wie eine ganze. */
  weitere: number;
};

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
  /** Darf `null` sein — nicht zugeordnet heißt nicht zugeordnet (Regel Q4). */
  processName: string | null;
  projectName: string | null;
  bamWerte: BamWerte[];
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
