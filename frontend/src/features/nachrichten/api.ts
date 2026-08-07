import { hole } from "@/lib/http";

/**
 * Die Nachrichtenliste, ihr Detail und die Prozessauswahl dazu.
 *
 * **Kein Endpunkt hier bekommt eine Mandanten-ID.** Der Mandant kommt aus der
 * Sitzung (Regel M1); es gibt keinen Parameter dafür und es darf keiner
 * entstehen. Auch die `MessageID` im Pfad ist **keine Berechtigung**: Wer eine
 * fremde errät, bekommt `404` — denselben Rumpf wie bei einer erfundenen.
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
  /**
   * Der Schritt, auf dem die Nachricht **gerade steht** — **nur** bei `WARTEND`
   * und `LAEUFT`, sonst `null`.
   *
   * Dass die Auswahl schon im Backend getroffen ist, ist Absicht: `SOSActionID`
   * ist auf jeder Zeile gesetzt, benennt bei abgeschlossenen aber den *letzten*
   * Schritt. Die Oberfläche entscheidet hier nicht, was sie zeigt, sondern nur
   * wie — was sie zeigen darf, steht schon fest.
   */
  schritt: string | null;
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

/**
 * Was der Bestand des aktiven Mandanten hergibt — **Stammdaten der Ansicht, nicht
 * Inhalt einer Seite**.
 *
 * Die Oberfläche entscheidet daran, welche Bedienelemente sie überhaupt anbietet.
 * Ein Schalter, der etwas ausblendet, das es beim eigenen Mandanten gar nicht
 * gibt, kündigt eine Wirkung an, die ausbleibt.
 */
export type Merkmale = {
  /**
   * Messung M12: Fünf von neun Mandanten mit Nachrichten haben über den gesamten
   * Bestand nicht eine einzige `SPLITTED`- oder `MERGED`-Zeile.
   */
  zwischenschritteVorhanden: boolean;
};

/* ─────────────────────────────────────────────────────────────────────────────
   Das Detail einer einzelnen Nachricht (Schritt 5)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Woher der Name eines Schritts stammt — **Nachweis, keine Warnung.**
 *
 * Die Oberfläche zeigt ihn ausschließlich im Tooltip: Ein Nutzer, der „Send File
 * by FTP" liest, soll nicht mit der Frage belastet werden, wie wir darauf
 * gekommen sind. Wer nachsehen will, findet es.
 */
export type Namensherkunft = "DIREKT" | "HERGELEITET" | "ROHWERT";

/**
 * Woran eine **offene** Nachricht steht — vom Backend benannt, nicht hier
 * errechnet.
 *
 * Die Oberfläche stellt das Feld dar und leitet nichts ab. Der Unterschied
 * zwischen „steht auf" und „wartet davor" ist gemessen (M16 3) und gehört ins
 * Backend, nicht in eine Bedingung in einer Komponente.
 */
export type OffenerZustand = "LAEUFT_AUF" | "WARTET_VOR" | "OHNE_SCHRITT" | "KEINER";

/** Ein ausgeführter Prozessschritt. Der Metadaten-Schritt ist nicht dabei. */
export type Schritt = {
  /** `MessageActionID` — laufende Nummer je Nachricht, nicht die Kennung im Ablauf. */
  position: number;
  name: string;
  namensherkunft: Namensherkunft;
  /** `SOSActionServiceProperties`, unverändert. Kommt immer mit, auch bei `DIREKT`. */
  rohwert: string | null;
  start: string;
  /** `null`, solange kein Ende protokolliert ist. */
  ende: string | null;
  /**
   * Ganze Sekunden. **`null` statt einer negativen Zahl** — und `null`, solange
   * das Ende fehlt. Die Anzeige schreibt dann keine Dauer, keinen Balken und
   * keine erfundene Null.
   */
  dauerSekunden: number | null;
  /**
   * `SOSActionTimeout` in Sekunden. **Wird heute nicht gedeutet** — ob ein
   * Schritt über seiner Frist gekennzeichnet wird, ist offen (Frage 7 in
   * `messungen-schritt5.md`) und wird in diesem Schritt nicht entschieden.
   */
  timeoutSekunden: number | null;
  /**
   * Ob die Nachricht **gerade auf diesem Schritt steht**. Bewusst nicht dasselbe
   * wie `ende === null`: Ein fehlendes Ende gibt es auch auf abgeschlossenen
   * Nachrichten, dort ist es eine Protokolllücke und kein Hänger (M22).
   */
  laeuftAuf: boolean;
};

/**
 * Eine kuratierte Eigenschaft im Kopf.
 *
 * **Rohname, Wert, Rang** — die deutsche Beschriftung kommt aus der Sprachdatei
 * (`texte.nachrichten.detail.kuratiert`). Ohne Übersetzung erscheint der
 * Rohname, sichtbar unfertig: Ein neuer Name aus dem Altsystem fällt so beim
 * ersten Blick auf, statt lautlos zu fehlen.
 */
export type KuratierteEigenschaft = { name: string; wert: string; rang: number };

/** Kopf, Schrittfolge und kuratierte Eigenschaften in einem Stück. */
export type Nachrichtendetail = {
  messageId: string;
  status: string;
  statusKind: string;
  processId: string;
  processName: string | null;
  projectName: string | null;
  sosName: string | null;
  /** `MessageLastUpdate`. Es gibt kein Anlagedatum (Regel Q2). */
  zeitpunkt: string;
  /** Der **fachliche** Start: `MIN(MessageActionStart)` über alle Aktionen. */
  start: string | null;
  timeoutSekunden: number | null;
  /**
   * Wie viele technische Eigenschaften die Nachricht hat.
   *
   * **Sie steht im Kopf, damit der eingeklappte Block beschriftet werden kann,
   * ohne ihn zu laden.** Ohne sie verlöre der zweite Endpunkt seinen Zweck.
   */
  eigenschaftenAnzahl: number;
  offenerZustand: OffenerZustand;
  /** Nur bei `WARTET_VOR` gesetzt — und auch dort nullbar (M13). */
  naechsterSchritt: string | null;
  schritte: Schritt[];
  kuratierteEigenschaften: KuratierteEigenschaft[];
};

/** Eine rohe `MessageProperty`-Zeile, auf Abruf geladen. */
export type Eigenschaft = {
  name: string;
  wert: string;
  /** `MessageActionID` — der Schritt, an dem die Eigenschaft hängt. */
  position: number;
  /**
   * Ob der Wert die Byte-Grenze des Backends gerissen hat. **Ein gekappter Wert
   * wird als gekappt gekennzeichnet**: Sonst läse jemand eine halbe Belegnummer
   * als ganze.
   */
  gekappt: boolean;
  /** Die volle Länge in Bytes; `null`, wenn nichts gekappt wurde. */
  originalLaengeBytes: number | null;
};

export const NACHRICHTEN_SCHLUESSEL = {
  /** Der Filter gehört in den Schlüssel: Andere Filter sind andere Daten. */
  liste: (abfrage: string) => ["nachrichten", "liste", abfrage] as const,
  prozesse: ["nachrichten", "prozesse"] as const,
  merkmale: ["nachrichten", "merkmale"] as const,
  /**
   * Das Detail hängt an der Kennung und **nicht am Filter der Liste**. Ein
   * tiefer Link auf eine Nachricht außerhalb des aktuellen Zeitfensters zeigt sie
   * deshalb auch dann, wenn die Liste dahinter leer ist.
   */
  detail: (messageId: string) => ["nachrichten", "detail", messageId] as const,
  eigenschaften: (messageId: string) => ["nachrichten", "eigenschaften", messageId] as const,
};

export function holeNachrichten(abfrage: string): Promise<Seite<Nachricht>> {
  return hole<Seite<Nachricht>>(`/nachrichten${abfrage}`);
}

export function holeProzesse(): Promise<Prozess[]> {
  return hole<Prozess[]>("/prozesse");
}

export function holeMerkmale(): Promise<Merkmale> {
  return hole<Merkmale>("/nachrichten/merkmale");
}

/**
 * `encodeURIComponent`, obwohl eine `MessageID` eine UUID ist: Sie kommt aus
 * der URL und damit von außen. Was von außen kommt, wird kodiert — sonst
 * entscheidet ein Schrägstrich in der Kennung, welcher Endpunkt gerufen wird.
 */
export function holeNachrichtendetail(messageId: string): Promise<Nachrichtendetail> {
  return hole<Nachrichtendetail>(`/nachrichten/${encodeURIComponent(messageId)}`);
}

export function holeEigenschaften(messageId: string): Promise<Eigenschaft[]> {
  return hole<Eigenschaft[]>(`/nachrichten/${encodeURIComponent(messageId)}/eigenschaften`);
}
