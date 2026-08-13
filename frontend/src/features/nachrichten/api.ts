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

/*
 * Hier stand bis zum 11.08.2026 der Typ `Merkmale` samt `holeMerkmale` — die
 * Auskunft aus `GET /api/nachrichten/merkmale`, an der die Oberfläche entschied,
 * ob sie den Ausblende-Schalter überhaupt anbietet. Endpunkt und Schalter sind
 * gemeinsam entfallen (`docs/nachrichtenliste.md` §5).
 */

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
 *
 * **`WARTET_IN` ist am 10.08.2026 dazugekommen** (M29). Bis dahin verglich diese
 * Oberfläche `naechsterSchritt` mit den Namen der gelaufenen Schritte und
 * entschied daraus, welchen Satz sie schreibt — eine Ableitung, die hier nichts
 * zu suchen hat. Jetzt liefert das Backend zwei Werte:
 *
 * | Wert | Bedeutung |
 * |---|---|
 * | `WARTET_IN` | wartet **in** dem Schritt, der sie schlafen gelegt hat — 538 von 538 |
 * | `WARTET_VOR` | wartet **vor** einem Schritt, der noch nicht begonnen hat — 0 von 538 |
 *
 * **`OHNE_SCHRITT` ist am selben Tag verschwunden** und in zwei Werte aufgeteilt.
 * Er trug zwei Fälle, die verschiedene Fragen beantworten:
 *
 * | Wert | Bedeutung |
 * |---|---|
 * | `EMPFANGEN` | im System angekommen, seitdem ist kein Schritt gelaufen — eine Auskunft über die **Plattform** |
 * | `OHNE_AKTION` | zu dieser Nachricht ist gar kein Ablauf protokolliert — eine Auskunft über die **Datenlage** |
 *
 * Ein gemeinsamer Text müsste so vage sein, dass er beides abdeckt, und wäre
 * dann für keinen der beiden brauchbar. Der alte Name wird für keinen der beiden
 * weiterverwendet, damit die unscharfe Bedeutung nicht überlebt.
 */
export type OffenerZustand =
  "LAEUFT_AUF" | "WARTET_IN" | "WARTET_VOR" | "EMPFANGEN" | "OHNE_AKTION" | "KEINER";

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

/**
 * Die Stellung einer Nachricht in der Verkettung — **eine von vier**, und eine
 * Zeile kann mehrere davon tragen (514 von 214.330, M28‑1c; nie mehr als zwei).
 *
 * Die vier Werte sind nicht vier Sichten auf dieselbe Beziehung, sondern **zwei
 * Beziehungen mal zwei Richtungen** (M25‑2): `Source`/`SourceMessageID` tragen
 * die Aufteilung, `Target`/`TargetMessageID` die Zusammenführung.
 */
export type Kettenrolle = "SPLIT_WURZEL" | "SPLIT_KIND" | "MERGE_EINGANG" | "MERGE_ERGEBNIS";

/**
 * Wodurch ein Glied mit der angefragten Nachricht zusammenhängt.
 *
 * **Sie steht ausdrücklich in jeder Antwortzeile**, obwohl sie sich aus Rollen
 * und Ebene ableiten ließe — genau dieser Unterschied ist das, was die
 * Oberfläche dem Nutzer sagen muss. Sie entscheidet hier über den Abschnitt, in
 * dem ein Glied erscheint (`kette.ts`).
 */
export type Kettenbeziehung = "AUFTEILUNG" | "ZUSAMMENFUEHRUNG";

/** Ein Glied der Kette — genug für eine Zeile, nicht mehr. */
export type Kettenglied = {
  messageId: string;
  status: string;
  statusKind: string;
  /** `MessageLastUpdate` als UTC. Zugleich die erste Hälfte des Sortierschlüssels. */
  zeitpunkt: string;
  sosName: string | null;
  rollen: Kettenrolle[];
  /** **Negativ aufwärts, `+1` abwärts.** Der Aufstieg ist ein Weg, der Abstieg eine Ebene. */
  ebene: number;
  beziehung: Kettenbeziehung;
};

/**
 * Was an dieser Nachricht hängt — der Aufstieg vollständig, der Abstieg eine
 * Ebene.
 *
 * ## Die beiden Listen heißen nach dem Mechanismus
 *
 * `aufwaerts` und `abwaerts` beschreiben, **wie** ein Glied erreicht wurde, und
 * behaupten nicht, was es bedeutet. Die Bedeutung tragen `beziehung` und
 * `ebene` — und genau die wertet `kette.ts` aus. Frühere Namen benannten die
 * Bedeutung und lagen bei jedem Merge-Eingang daneben: Dort steht im Aufstieg
 * das *Ergebnis*, also das, was aus ihm wurde (`docs/verkettung.md` §2, §8.3).
 */
export type Kette = {
  messageId: string;
  rollen: Kettenrolle[];
  /** Der Aufstieg, Ebene `-1` zuerst. Leer, wenn die Nachricht am oberen Ende steht. */
  aufwaerts: Kettenglied[];
  /** Kinder **und** Merge-Eingänge gemeinsam, nach `(zeitpunkt, messageId)` sortiert. */
  abwaerts: Kettenglied[];
  /**
   * Wie viele es insgesamt sind — **die genaue Zahl**, nicht „mehr als 50".
   *
   * Die Entscheidung hängt an einer Messung: Die Zählung kostet an der
   * breitesten Wurzel des gesamten Bestands 19,8 ms (M30‑1), also nicht die
   * Hälfte der Grenze, ab der auf eine Ersatzform umgestellt worden wäre.
   * Deshalb darf die Überschrift die Zahl nennen.
   */
  abwaertsGesamt: number;
  /**
   * Die Position, ab der `GET …/kette/abwaerts?cursor=…` weiterblättert — die
   * **letzte hier gelieferte** Abwärtszeile.
   *
   * **Mit ihm kostet der erste Klick auf „Mehr laden" eine Anfrage und nicht
   * zwei.** Ohne ihn musste der Block die erste Seite ein zweites Mal holen,
   * nur um an eine Position zu kommen.
   *
   * `null`, wenn `weitereVorhanden` falsch ist. Und `null` im Sonderfall aus
   * M30‑6: Trägt die letzte Zeile keinen Zeitpunkt, hat sie in der Ordnung
   * keine Position — dann gibt es **keine** Schaltfläche zum Nachladen, auch
   * wenn `weitereVorhanden` wahr ist. Gemessen kommt das 0 von 3.341.519 Mal
   * vor.
   */
  abwaertsCursor: string | null;
  /**
   * Ob es mehr Glieder gibt als geliefert. **Beim Merge ist das der Regelfall:**
   * 11,38 Prozent der Merge-Ergebnisse haben mehr als 50 Eingänge, gegen 1,07
   * Prozent der Wurzeln (M30‑2).
   */
  weitereVorhanden: boolean;
  /** Der Aufstieg ist an der Tiefengrenze abgebrochen — die Kette ist länger als gezeigt. */
  tiefeErreicht: boolean;
  /** Der Aufstieg ist auf eine bereits besuchte Kennung gestoßen — die Kette führt im Kreis. */
  zyklusErkannt: boolean;
};

/** Kopf, Schrittfolge und kuratierte Eigenschaften in einem Stück. */
export type Nachrichtendetail = {
  messageId: string;
  status: string;
  statusKind: string;
  processId: string;
  processName: string | null;
  projectName: string | null;
  sosName: string | null;
  /**
   * Die Stellung dieser Nachricht in der Verkettung — **immer vorhanden, leer
   * statt fehlend**.
   *
   * Ein fehlendes Feld hieße „unbekannt", ein leeres heißt „nicht in einer
   * Kette". Daran entscheidet die Oberfläche, ob sie den Kettenblock zeigt und
   * ob sie `/kette` überhaupt ruft: Rund 60 Prozent aller Zeilen tragen keine
   * Kette, und für sie entsteht keine zweite Anfrage.
   */
  rollen: Kettenrolle[];
  /** `MessageLastUpdate`. Es gibt kein Anlagedatum (Regel Q2). */
  zeitpunkt: string;
  /** Der **fachliche** Start: `MIN(MessageActionStart)` über alle Aktionen. */
  start: string | null;
  /**
   * Vom fachlichen Start bis `zeitpunkt`, in ganzen Sekunden — für **jede**
   * Nachricht.
   *
   * Sie ist die Abdeckung für Zeit, die *zwischen* zwei Schritten steckt und in
   * keiner Schrittdauer auftaucht. Genau dafür war die gestrichene Lückenzeile
   * gedacht; die Gesamtdauer leistet es ohne ein Element, das nie jemand
   * ausgelöst hat.
   */
  gesamtdauerSekunden: number | null;
  /**
   * `Message.MessageTimeout` in **Sekunden** (M8 — nicht Minuten). `null`, wenn
   * keine Frist gesetzt ist; eine `0` wird nicht durchgereicht.
   */
  fristSekunden: number | null;
  /**
   * Wie viele technische Eigenschaften die Nachricht hat.
   *
   * **Sie steht im Kopf, damit der eingeklappte Block beschriftet werden kann,
   * ohne ihn zu laden.** Ohne sie verlöre der zweite Endpunkt seinen Zweck.
   */
  eigenschaftenAnzahl: number;
  /**
   * Wie viele Belegnummern auf der Nachricht stehen — **immer vorhanden, `0`
   * statt fehlend**.
   *
   * **Daran entscheidet die Oberfläche, ob sie den BAM-Block überhaupt zeichnet
   * und ob sie `/bam` ruft.** Bei **80,6 Prozent** der Nachrichten ist die Zahl
   * `0` (M41), bei Merge-Eingängen bei *allen* — für sie entsteht damit weder
   * ein leerer Rahmen noch eine zweite Anfrage. Dieselbe Bauform wie `rollen`
   * beim Kettenblock und `eigenschaftenAnzahl` beim Eigenschaftenblock.
   *
   * Sie beschriftet zugleich die Überschrift des eingeklappten Blocks, damit
   * erkennbar ist, ob sich das Aufklappen lohnt.
   */
  bamAnzahl: number;
  offenerZustand: OffenerZustand;
  /**
   * Der Schritt, auf den `Message.SOSID`/`SOSActionID` zeigen — nur in den beiden
   * Wartezuständen gesetzt und auch dort nullbar (M13).
   *
   * Bei `WARTET_IN` steht er im Tooltip und nicht als Zeile: Sein Name steht
   * ohnehin schon in der Leiste.
   */
  naechsterSchritt: string | null;
  /**
   * Wie lange die Nachricht schon steht, in ganzen Sekunden. **Im Backend gegen
   * die Anwendungsuhr gerechnet, niemals hier.**
   *
   * Im Profil `dev` steht die Anwendungsuhr Monate zurück. Eine Oberfläche, die
   * `Date.now()` gegen `zeitpunkt` rechnete, zeigte dort Monate statt Stunden —
   * genau dafür gibt es die Uhr.
   *
   * Bei `EMPFANGEN` rechnet sie ab dem Metadaten-Schritt — die Nachricht ist
   * angekommen und hängt seitdem, und genau das ist die Auskunft. `null` ist sie
   * nur bei `OHNE_AKTION` und `KEINER`: Dort gibt es keinen Anker.
   */
  wartetSeitSekunden: number | null;
  /**
   * Problemkategorie 2 aus `PROJEKTBESCHREIBUNG.md` §4.2: nicht in einem
   * Endstatus **und** Frist abgelaufen. Kommt fertig aus dem Backend
   * (`MessageStatusClassifier.istUeberfaellig`) und wird hier nicht nachgerechnet.
   */
  ueberfaellig: boolean;
  schritte: Schritt[];
  kuratierteEigenschaften: KuratierteEigenschaft[];
};

/* ─────────────────────────────────────────────────────────────────────────────
   Die Belegdaten einer Nachricht (Schritt 7, Teil 1)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Eine Typgruppe der Belegdaten — **die wahre Zahl neben den gezeigten Werten.**
 *
 * Die Zweiteilung im Backend ist hier sichtbar: `gesamt` kommt aus einer
 * Zählung über den Index, `werte` aus einer zweiten, im Statement gedeckelten
 * Abfrage. Ohne diese Trennung hinge die Antwortgröße an einer Zahl, die
 * niemand gemessen hat — auf **einer** Nachricht stehen bis zu 9.296 Werte
 * (M41), und über den ganzen Bestand ist das Maximum unbekannt.
 */
export type BamGruppe = {
  /**
   * Die Typnummer. Sie wird nicht angezeigt — sie ist zusammen mit dem Wert der
   * **Schlüssel der Liste**. Der Wert allein genügt nicht: Bei 4,17 Prozent der
   * Paare steht derselbe Wert unter mehreren Typen (M37), und doppelte
   * React-Schlüssel sind für Vitest unsichtbar.
   */
  typ: number;
  /**
   * `MessageBAMType.MessageBAMTypeDescription`, unverändert — **nie `null`**.
   * Fehlt die Zeile im Altsystem, steht hier die Typnummer: sichtbar unfertig
   * statt lautlos leer.
   *
   * **Die Endungen `_K_SAP`, `_L_SAP` und `_FORS` bleiben stehen.** Ob die
   * Beschreibungen ohne sie noch eindeutig sind, ist nicht gemessen — eine
   * Kürzungsregel wäre nach Regel Q4 geraten (`docs/bam-werte.md`).
   */
  bezeichnung: string;
  /** Die **wahre** Zahl der Werte dieses Typs, unabhängig von der Deckelung. */
  gesamt: number;
  /** Höchstens 20, aufsteigend nach Wert. Schon in der Datenbank gedeckelt. */
  werte: string[];
  /**
   * `gesamt > werte.length`. Daraus schreibt die Oberfläche die **ehrliche
   * Restangabe** — „und 2.987 weitere". **Kein „mehr laden":** Das bräuchte
   * einen Cursor und kommt erst, wenn jemand es braucht.
   */
  weitereVorhanden: boolean;
};

/** Die Belegnummern einer Nachricht, nach Typ gruppiert. */
export type BamWerte = {
  messageId: string;
  /**
   * Je Typ eine Gruppe — **immer vorhanden, leer statt fehlend**. Zuerst die
   * für diesen Mandanten konfigurierten Typen in ihrer Sortierreihenfolge,
   * danach die übrigen nach Typnummer.
   *
   * **Die Konfiguration ordnet und siebt nicht.** `WOC` trägt 2.067 BAM-Zeilen
   * unter einem Typ, den seine Konfiguration nicht kennt (M40); folgte der
   * Block ihr als Filter, sähe dieser Mandant nichts. Ein unkonfigurierter Typ
   * wird deshalb gezeigt — hinten und **ohne Markierung**.
   */
  gruppen: BamGruppe[];
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
  /**
   * Das Detail hängt an der Kennung und **nicht am Filter der Liste**. Ein
   * tiefer Link auf eine Nachricht außerhalb des aktuellen Zeitfensters zeigt sie
   * deshalb auch dann, wenn die Liste dahinter leer ist.
   */
  detail: (messageId: string) => ["nachrichten", "detail", messageId] as const,
  eigenschaften: (messageId: string) => ["nachrichten", "eigenschaften", messageId] as const,
  /** Die Belegdaten einer Nachricht — nur geladen, wenn der Block aufgeklappt wird. */
  bam: (messageId: string) => ["nachrichten", "bam", messageId] as const,
  /** Die Kette einer Nachricht — nur geladen, wenn `rollen` nicht leer ist. */
  kette: (messageId: string) => ["nachrichten", "kette", messageId] as const,
  /**
   * Die nachgeladenen Seiten der Abwärtsglieder. **Ein eigener Schlüssel**, damit
   * ein erneutes Aufklappen derselben Nachricht nicht die Kette selbst neu holt.
   */
  kettenAbwaerts: (messageId: string) => ["nachrichten", "kette", messageId, "abwaerts"] as const,
};

export function holeNachrichten(abfrage: string): Promise<Seite<Nachricht>> {
  return hole<Seite<Nachricht>>(`/nachrichten${abfrage}`);
}

export function holeProzesse(): Promise<Prozess[]> {
  return hole<Prozess[]>("/prozesse");
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

/**
 * Die Belegnummern einer Nachricht.
 *
 * **Kein Zeitfenster und keine Seitengröße.** Die Menge ist durch einen
 * Primärschlüssel benannt, und die Deckelung sitzt im Statement des Backends —
 * je Typgruppe höchstens zwanzig Werte. Der Aufrufer hat hier nichts zu
 * stellen.
 */
export function holeBamWerte(messageId: string): Promise<BamWerte> {
  return hole<BamWerte>(`/nachrichten/${encodeURIComponent(messageId)}/bam`);
}

export function holeKette(messageId: string): Promise<Kette> {
  return hole<Kette>(`/nachrichten/${encodeURIComponent(messageId)}/kette`);
}

/**
 * Eine weitere Seite der Abwärtsglieder — cursor-basiert **innerhalb der festen
 * Wurzel**.
 *
 * **Kein Sprung in die Liste.** Naheliegend wäre ein Filter
 * `?wurzel=…` an `/api/nachrichten`; das bräche Regel L1, weil die Liste ein
 * Pflicht-Zeitfenster verlangt und die Kinder einer drei Monate alten Wurzel
 * außerhalb jedes vernünftigen Fensters lägen. Dieser Endpunkt braucht keines:
 * Die Menge ist durch die Wurzel benannt, und er steigt über
 * `SourceMessageIDIDX` ein statt über `MessageLastUpdateIDX` (E5, M30‑1).
 *
 * **Ohne `limit`.** Die Seitengröße gehört dem Backend; ein zweiter Wert hier
 * liefe dem ersten irgendwann hinterher.
 *
 * @param cursor die Position, hinter der weitergelesen wird. Der Block setzt
 *   hier `kette.abwaertsCursor` ein und bekommt damit **die zweite** Seite —
 *   `null` holte die erste noch einmal und wäre der Umweg, den es seit dem
 *   11.08.2026 nicht mehr gibt.
 */
export function holeKettenAbwaerts(
  messageId: string,
  cursor: string | null,
): Promise<Seite<Kettenglied>> {
  const abfrage = cursor === null ? "" : `?cursor=${encodeURIComponent(cursor)}`;
  return hole<Seite<Kettenglied>>(
    `/nachrichten/${encodeURIComponent(messageId)}/kette/abwaerts${abfrage}`,
  );
}
