import { hole } from "@/lib/http";
import type { Fenster, Rollupzeitraum } from "@/lib/rollupzeitraum";

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
 * für den Prozessfilter dieser Liste gebraucht; ein Feature `prozesse` entstünde
 * allein für einen Fetch und müsste sofort von hier importiert werden.
 *
 * ## Warum die Prozess**ansicht** seit dem 02.09.2026 ebenfalls hier liegt (E‑47)
 *
 * `docs/frontend-grundlagen.md` §8 hat für diesen Tag vorgesorgt: *„Kommt in
 * Schritt 10 eine eigene Prozessansicht, wandert der gemeinsame Teil nach
 * `components/` oder `lib/` — nicht ins Nachbarfeature."* **Genau das ist
 * geschehen** — die drei Rollup-Paare stehen seither in `lib/rollupzeitraum.ts`,
 * der Zeitraumumschalter in `components/`.
 *
 * **Der Baum selbst bleibt trotzdem hier, und das ist eine Entscheidung.** Was
 * die Prozessansicht mit der Nachrichtenliste teilt, ist nicht *ein Fetch*: Ihre
 * rechte Spalte **ist** die Nachrichtenliste, und das Panel darüber **ist** das
 * Nachrichtendetail. Beide nach `components/` zu heben hieße, den halben
 * Feature-Inhalt in die Naht zu schieben, die dort für Rahmen, Kopfzeile und
 * Zustände gedacht ist.
 *
 * **Der Präzedenzfall steht daneben:** Die Belegsuche ist seit Schritt 7 eine
 * eigene Route (`/suche`) mit eigener Trefferliste und liegt aus demselben Grund
 * in diesem Feature — sie hängt das vorhandene Panel ein. Die Prozessansicht ist
 * der dritte Einstieg in dieselbe Menge und nicht eine zweite Menge.
 *
 * **Der Preis ist benannt:** Dieses Feature trägt damit drei Ansichten, und sein
 * Name sagt das nicht. Ob es später anders heißen soll, ist eine Umbenennung und
 * keine Umstellung — offener Punkt **116**.
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

/* ─────────────────────────────────────────────────────────────────────────────
   Die Prozessansicht: der Baum Partner → Richtung → Prozess (Schritt 10c)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Die gelesenen Grenzen des Baums. Der Typ steht in `lib/rollupzeitraum.ts` —
 * **dasselbe Feld wie im Dashboard, und deshalb nur einmal deklariert.**
 */
export type { Fenster };

/**
 * Die drei Zustände aus `docs/process-view.md` §4 — sie hängen **allein an der
 * letzten Bewegung** und sind fensterunabhängig (Entscheidung E‑35).
 *
 * `STILL` und `NIE` sind zwei verschiedene Aussagen und werden nie
 * zusammengefasst: Das eine ist ein **Vorfall** (es gab eine Beziehung, und sie
 * ist verstummt), das andere eine **Katalogfrage** (der Vertrag steht, und es
 * ist nie etwas darüber gelaufen). Bei `VOTG` bestünde der Baum sonst zu 90 %
 * aus Markierungen.
 */
export type Prozesszustand = "BEWEGT" | "STILL" | "NIE";

export type Prozessknoten = {
  processId: string;
  /** Darf `null` sein — „nicht zugeordnet heißt nicht zugeordnet" (Regel Q4). */
  processName: string | null;
  /** Im gewählten Zeitraum. **Null ist eine Aussage und kein fehlender Wert.** */
  nachrichten: number;
  /** Im gewählten Zeitraum, über `MessageStatusClassifier` eingeordnet. */
  fehler: number;
  /** **Ohne Zeitfenster** — der Wert, an dem die drei Zustände hängen. `null` bei `NIE`. */
  letzteBewegung: string | null;
  zustand: Prozesszustand;
};

/**
 * Eine Richtungsgruppe unter einem Partner.
 *
 * `richtung` ist `EINGEHEND`, `AUSGEHEND`, ein gepflegter aber unbekannter Wert
 * — oder **`null`: „nicht ermittelt"**. Die Antwort unterscheidet dabei bewusst
 * nicht zwischen „gepflegt und leer" und „offen" (Entscheidung E‑40); dass die
 * Unterscheidung gebraucht würde, ist nicht gemessen.
 */
export type Richtungsknoten = {
  richtung: string | null;
  anzahlProzesse: number;
  nachrichten: number;
  fehler: number;
  prozesse: Prozessknoten[];
};

/** `partner === null` heißt „nicht zugeordnet" und steht **am Ende** (E‑39). */
export type Partnerknoten = {
  partner: string | null;
  anzahlProzesse: number;
  nachrichten: number;
  fehler: number;
  richtungen: Richtungsknoten[];
};

/** Die Kopfzahlen über den ganzen Baum. Die drei Zustände sind disjunkt und vollständig. */
export type Baumsumme = {
  anzahlProzesse: number;
  bewegt: number;
  still: number;
  nie: number;
  nachrichten: number;
  fehler: number;
};

export type Prozessbaum = {
  /** Das **gewählte** Paar, immer gesetzt — auch wenn der Aufrufer keins genannt hat. */
  zeitraum: Rollupzeitraum;
  fenster: Fenster;
  /**
   * Ab wie vielen Monaten ohne Bewegung ein Prozess als `STILL` gilt.
   *
   * **Die Oberfläche beschriftet damit und rechnet nichts nach** (Entscheidung
   * E‑37): Sie muss „seit über drei Monaten" formulieren können, ohne die Drei
   * selbst zu kennen — sonst stünde dieselbe fachliche Festlegung an zwei Orten
   * und driftete.
   */
  stilleSchwelleMonate: number;
  gesamt: Baumsumme;
  partner: Partnerknoten[];
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

/* ─────────────────────────────────────────────────────────────────────────────
   Die Belegsuche (Schritt 7, Teil 3)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Eine Belegart zur Auswahl neben dem Suchfeld.
 *
 * **Die Auswahl ist ein Angebot und kein Filter.** Sie kommt aus der
 * Konfiguration des Mandanten (`MessageBAMMandant`) und sagt nichts darüber, was
 * im Bestand steht — `WOC` trägt 2.067 BAM-Zeilen unter einem Typ, den seine
 * Konfiguration nicht kennt (M40). Deshalb bleibt die Suche **ohne** Typ die
 * Vorgabe, und ein Mandant ohne konfigurierten Typ bekommt gar keine Auswahl.
 */
export type BamTyp = {
  typ: number;
  /** `MessageBAMType.MessageBAMTypeDescription`, unverändert — nie `null`. */
  bezeichnung: string;
  /** Die Ordnung des Altsystems. Sie wird nicht angezeigt; die Liste steht schon darin. */
  sortIndex: number;
};

/**
 * Worauf eine Nummer auf dieser Nachricht getroffen hat.
 *
 * **Das ist die eine Angabe, die der Nutzer nicht selbst getippt hat.** Den Wert
 * kennt er — er steht in seiner Marke; was er nicht weiß, ist, ob seine Nummer
 * dort als Lieferschein-Nr., als Charge oder als Kundenmaterialnummer steht.
 */
export type BamTrefferWert = {
  /** Zusammen mit dem Wert der Schlüssel der Liste — niemals der Wert allein (M37). */
  typ: number;
  bezeichnung: string;
  /** Der Wert, **wie er im Bestand steht** — mit führender Null, falls er eine trägt. */
  wert: string;
};

/**
 * Eine gefundene Nachricht: **dieselbe Zeilengestalt wie in der Liste**, dazu
 * `rollen` und `treffer`.
 *
 * Dass die zehn Listenfelder wortgleich sind, ist eine Zusage des Endpunkts
 * (`docs/bam-suche.md` §1) — wer aus der Suche heraus weiterarbeitet, sieht
 * dieselbe Zeile wie aus der Liste, und das Detail dahinter ist dasselbe.
 */
export type BamTreffer = Nachricht & {
  /**
   * Die Stellung in der Verkettung — **immer vorhanden, leer statt fehlend**.
   *
   * Sie steht hier, weil die Suche fast immer die **Wurzel** findet: 96,87
   * Prozent der Wurzeln tragen BAM-Werte, nur 2,42 Prozent der Kinder (M26‑1b).
   * Ohne sie hielte sich der Nutzer bei einem Endstatus für fertig, obwohl der
   * Beleg als Bündel weitergelaufen ist.
   */
  rollen: Kettenrolle[];
  /** Mehrere sind kein Randfall: 4,17 Prozent der Paare tragen denselben Wert unter mehreren Typen (M37). */
  treffer: BamTrefferWert[];
};

/**
 * Ein Begriff, so wie die Suche ihn verstanden hat — **samt der Fassungen, nach
 * denen tatsächlich gesucht wurde**.
 *
 * **Keine stille Korrektur.** Wer `4711815` tippt und `004711815` findet, muss
 * erfahren, warum; sonst sähe die Trefferliste aus, als hätte die Datenbank
 * etwas anderes enthalten als sie enthält. Die Fassung mit führendem Leerzeichen
 * steht bewusst **nicht** darin (`docs/bam-suche.md` §3).
 */
export type BamBegriffTreffer = {
  /** Die Eingabe, an den Rändern beschnitten — ein Zitat der Frage. */
  eingabe: string;
  typ: number | null;
  /** Die gesuchten Fassungen, mit der Eingabe an erster Stelle. */
  varianten: string[];
};

/**
 * Der Vergleichsmodus, **wie die Antwort ihn meldet** — großgeschrieben.
 *
 * Der gleichnamige URL-Parameter ist klein (`modus=exakt|praefix`), und das ist
 * kein Versehen: Die URL-Parameter dieses Projekts sind kleingeschrieben und
 * deutsch, die kontrollierten Vokabulare der Antwort sind es nicht (`statusKind`,
 * `rollen`) — `docs/bam-suche.md` §20. Übersetzt wird an einer Stelle,
 * `suche.ts` `modusAusAntwort`.
 */
export type BamAntwortmodus = "EXAKT" | "PRAEFIX";

/** Die Antwort der Belegsuche. */
export type BamSuchergebnis = {
  /** Die Treffer, absteigend nach Zeitpunkt — **immer vorhanden, leer statt fehlend**. */
  nachrichten: BamTreffer[];
  begriffe: BamBegriffTreffer[];
  /**
   * Das **tatsächlich verwendete** Zeitfenster, ISO 8601 in UTC.
   *
   * Es steht in der Antwort und nicht nur in der Anfrage, weil es nicht die
   * Laufzeit verändert, sondern die **Antwort**: Beim schlimmsten gemessenen Wert
   * findet ein Tagesfenster 279 von 234.159 Nachrichten (M35). Wer nicht weiß,
   * dass er durch ein Fenster schaut, hält das Gefundene für alles, was es gibt.
   */
  von: string;
  bis: string;
  /** Ob es mehr Treffer gäbe. Gezählt **nach** dem Mandantenfilter. */
  abgeschnitten: boolean;
  /**
   * Der **tatsächlich verwendete** Modus — aus demselben Grund in der Antwort wie
   * `von` und `bis`: Er verändert nicht den Preis, sondern die Antwort. M49‑3
   * misst, dass schon ein vollständig eingetippter Wert als Präfix **23
   * Nachrichten statt einer** findet; wer nicht weiß, welcher Vergleich gelaufen
   * ist, kann die Trefferliste nicht deuten.
   */
  modus: BamAntwortmodus;
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

/* ─────────────────────────────────────────────────────────────────────────────
   Rohdaten und Protokolle (Schritt 8, Teil Frontend)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Die beiden Arten von Artefakten. Abgeleitet aus dem `MessagePropertyName` und
 * aus sonst nichts — gemessen sind genau zwei Namensmuster, `<Dienst>.Payload.GUID`
 * und `<Dienst>.Log.GUID` (M54).
 *
 * **Die Unterscheidung ist nicht kosmetisch:** Der Beschnitt greift
 * ausschließlich bei `PROTOKOLL`, und die Liste setzt die beiden Teile sichtbar
 * voneinander ab (`docs/rohdaten.md` §5, Entscheidung 6).
 */
export type Artefaktart = "NUTZDATEN" | "PROTOKOLL";

/**
 * Der Zustand eines Abrufs — die vier benannten Fälle aus `docs/rohdaten.md` §8
 * plus der Regelfall.
 *
 * **Keiner davon ist ein leeres Feld.** Jeder bekommt in der Oberfläche einen
 * eigenen Text; das Backend liefert den Schlüssel und deutet ihn nicht. Die
 * Trennung ist der Punkt: „Datei weg" und „Ablage aus" sehen für den Nutzer
 * gleich aus und sind für den Betrieb völlig verschiedene Lagen.
 *
 * **Die Anzeige antwortet in allen fünf Fällen mit `200`.** Ein Fehlerstatus
 * wäre falsch — „Protokoll ohne Marken" ist bei `FTPSender` der Normalfall
 * (M63), und der hängt an rund 69 % der Nachrichten.
 */
export type Artefaktzustand =
  | "ANZEIGBAR"
  | "BINAERDATEI"
  | "KEIN_ANZEIGBARER_PROTOKOLLTEIL"
  | "DATEI_NICHT_VORHANDEN"
  | "ABLAGE_NICHT_ERREICHBAR";

/**
 * Ein Artefakt in der Liste.
 *
 * **Kein Feld trägt einen Filestore-Verweis** — weder die GUID noch die
 * Ablagenkennung. Was hier steht, reicht aus, um das Artefakt *innerhalb seiner
 * Nachricht* zu benennen, und für nichts sonst.
 *
 * ## Es steht kein lesbarer Schrittname darin, und das ist Abweichung 1 des Backends
 *
 * Geliefert werden {@link schritt} (die `MessageActionID`) und {@link familie}
 * (`FileReader`, `Converter`, `FTPSender`). Die lesbare Beschriftung entsteht
 * **hier** — durch Verbindung mit der Schrittfolge aus dem Nachrichtendetail,
 * deren `position` genau diese `MessageActionID` ist. Gerechnet wird das in
 * `../rohdaten.ts`, nicht in einer Komponente.
 */
export type Artefakt = {
  /** Die Kennung für Anzeige und Download: `<MessageActionID>-<MessagePropertyName>`. */
  artefaktId: string;
  /** `MessagePropertyName`, unverändert. */
  name: string;
  /**
   * Der Teil vor dem Namensmuster — die technische Herkunft.
   *
   * **Sie ist keine Deutung**, sondern die Zeichenkette aus dem Namen, und wird
   * nirgends übersetzt, ergänzt oder erraten (Regel Q4).
   */
  familie: string;
  art: Artefaktart;
  /**
   * `MessageActionID`. **`0` ist der Metadaten-Schritt und kein Ablaufschritt**
   * (M57, M17 3) — er kommt in `schritte[]` des Detail-Endpunkts gar nicht vor.
   */
  schritt: number;
  /**
   * Ob dieses Artefakt für den **aufrufenden** Nutzer beschnitten wird. Wahr nur
   * bei Protokollen und nur für `MANDANT`.
   *
   * Es steht in der Liste, damit die Oberfläche es **ankündigen** kann, statt
   * den Nutzer erst beim Öffnen zu überraschen.
   */
  beschnittMoeglich: boolean;
};

/**
 * Die Artefakte einer Nachricht, **zweigeteilt in der Antwort**.
 *
 * Die Aufteilung kommt aus dem Datenmodell und nicht aus einer
 * Gestaltungsentscheidung: Der Beschnitt greift ausschließlich bei
 * `PROTOKOLL`, und `beschnittMoeglich` hängt daran.
 *
 * **Das dritte Feld ist am 19.08.2026 entfallen.** Es hieß `eingang` und führte
 * `Message.Payload.GUID` als „die eingegangene Datei". Nach **M73** trägt dieser
 * Name in 6.249 von 6.249 (Fenster A) und 214.330 von 214.330 Nachrichten
 * (Fenster B) den Verweis der Nutzdatenzeile mit dem **höchsten
 * `MessageActionID`** derselben Nachricht — er benennt keine eigene Datei,
 * sondern zeigt auf eine, die ohnehin an ihrem Schritt hängt. Das Backend führt
 * ihn seither nicht mehr.
 *
 * **Die Oberfläche teilt daraus neu ein.** Seit der Nachbesserung vom
 * 18.08.2026 hängen die Artefakte an den Zeilen der Zeitleiste, geordnet nach
 * `schritt` und nicht nach Art (`docs/rohdaten.md` §3, Entscheidung 6 in ihrer
 * korrigierten Fassung). **An dieser Antwort ändert das nichts** — sie ist
 * unverändert die des Backends, und die Einteilung nach Schritt entsteht in
 * `rohdaten.ts` (`zieleJeSchritt`).
 *
 * Jede Nachricht trägt **3 bis 15** Artefakte und mindestens ein Protokoll, bei
 * jedem Mandanten (M55). Eine leere Antwort ist deshalb kein erwarteter Zustand.
 */
export type Artefaktliste = {
  messageId: string;
  /** Die Dateien, nach Schritt geordnet. */
  nutzdaten: Artefakt[];
  /** Die Protokolle je Schritt, nach Schritt geordnet. */
  protokolle: Artefakt[];
};

/**
 * Die Anzeige eines Artefakts — **JSON, niemals ein Bytestrom mit ratbarem Typ.**
 *
 * Das ist der Unterschied zum Altsystem, das für Anzeige *und* Download denselben
 * `application/octet-stream` liefert (Q4). Hier ist der Inhalt ein
 * JSON-Zeichenkettenfeld, und die Oberfläche rendert ihn als **Textknoten**.
 */
export type Artefaktanzeige = {
  artefaktId: string;
  name: string;
  art: Artefaktart;
  zustand: Artefaktzustand;
  /** Der Inhalt, nach `ISO-8859-1` dekodiert. Leer, wenn `zustand` nicht `ANZEIGBAR` ist. */
  text: string;
  /**
   * Die Größe der **vollständigen** entpackten Datei — nicht die des gezeigten
   * Ausschnitts. Nur so ist ablesbar, wie viel fehlt.
   */
  groesseBytes: number;
  /** Ob die Anzeige an der Längengrenze gekappt wurde. Bei 610 KB Maximum (M60) eine Schutzmaßnahme. */
  gekuerzt: boolean;
  /** Ob der Markenbeschnitt gegriffen hat. Wahr nur bei Protokollen und nur für `MANDANT`. */
  beschnitten: boolean;
  /** Fest `ISO-8859-1` — gemessen, nicht geraten (M61). */
  kodierung: string;
  /**
   * Wie viele Einträge das Archiv trug. In 693 geholten Dateien immer `1`; alles
   * darüber ist ein nie beobachteter Fall und wird **vermerkt** statt
   * stillschweigend verworfen wie im Altsystem (`docs/rohdaten.md` §4).
   */
  zipEintraege: number;
};

export const NACHRICHTEN_SCHLUESSEL = {
  /** Der Filter gehört in den Schlüssel: Andere Filter sind andere Daten. */
  liste: (abfrage: string) => ["nachrichten", "liste", abfrage] as const,
  prozesse: ["nachrichten", "prozesse"] as const,
  /**
   * Der Prozessbaum. **Der Zeitraum gehört in den Schlüssel**, denn er ist ein
   * Anfrageparameter: ein anderes Paar ist eine andere Antwort.
   *
   * `null` heißt „ohne Parameter geholt" und ist ein **eigener** Schlüssel — und
   * ausdrücklich nicht der des vom Endpunkt gewählten Paares. Schriebe die
   * Ansicht das gewählte Paar zurück, entstünde beim ersten Rendern ein zweiter
   * Schlüssel und damit eine zweite Anfrage für dieselbe Antwort
   * (`docs/dashboard-frontend.md` §2).
   */
  baum: (zeitraum: Rollupzeitraum | null) => ["nachrichten", "prozessbaum", zeitraum] as const,
  /**
   * Das Detail hängt an der Kennung und **nicht am Filter der Liste**. Ein
   * tiefer Link auf eine Nachricht außerhalb des aktuellen Zeitfensters zeigt sie
   * deshalb auch dann, wenn die Liste dahinter leer ist.
   */
  detail: (messageId: string) => ["nachrichten", "detail", messageId] as const,
  eigenschaften: (messageId: string) => ["nachrichten", "eigenschaften", messageId] as const,
  /** Die Belegdaten einer Nachricht — nur geladen, wenn der Block aufgeklappt wird. */
  bam: (messageId: string) => ["nachrichten", "bam", messageId] as const,
  /** Die Belegarten zur Auswahl. Reine Stammdaten des Mandanten, entsprechend lange gehalten. */
  bamTypen: ["nachrichten", "bam-typen"] as const,
  /**
   * Eine Belegsuche. Der Schlüssel trägt die ganze Abfrage — andere Begriffe und
   * ein anderes Zeitfenster sind andere Daten.
   */
  bamSuche: (abfrage: string) => ["nachrichten", "bam-suche", abfrage] as const,
  /** Die Kette einer Nachricht — nur geladen, wenn `rollen` nicht leer ist. */
  kette: (messageId: string) => ["nachrichten", "kette", messageId] as const,
  /**
   * Die nachgeladenen Seiten der Abwärtsglieder. **Ein eigener Schlüssel**, damit
   * ein erneutes Aufklappen derselben Nachricht nicht die Kette selbst neu holt.
   */
  kettenAbwaerts: (messageId: string) => ["nachrichten", "kette", messageId, "abwaerts"] as const,
  /**
   * Die Artefakte einer Nachricht. **Ein Schlüssel für Block und Ansicht** — wer
   * aus dem Detail heraus eine Datei öffnet, holt die Liste kein zweites Mal.
   */
  dateien: (messageId: string) => ["nachrichten", "dateien", messageId] as const,
  /**
   * Der Inhalt **eines** Artefakts. Eigener Schlüssel je Artefakt: Hinter jedem
   * steht ein eigener SOAP-Abruf gegen die Ablage, und zwei Artefakte derselben
   * Nachricht sind zwei verschiedene Dateien.
   */
  dateiInhalt: (messageId: string, artefaktId: string) =>
    ["nachrichten", "dateien", messageId, artefaktId] as const,
};

export function holeNachrichten(abfrage: string): Promise<Seite<Nachricht>> {
  return hole<Seite<Nachricht>>(`/nachrichten${abfrage}`);
}

export function holeProzesse(): Promise<Prozess[]> {
  return hole<Prozess[]>("/prozesse");
}

/**
 * Den ganzen Baum in **einem** Aufruf (Entscheidung E‑33).
 *
 * **Kein Knoten wird nachgeladen.** Beim größten gemessenen Mandanten sind das
 * 154 Partner, 290 Richtungsgruppen und 733 Blätter in 150,3 KiB (M117); ein
 * Nachladen je Partner wären 154 Anfragen mit je einer Sitzungsprüfung, und auf
 * der Testkopie schreibt jede Anfrage die Sitzung fort.
 *
 * **Was nicht gewählt ist, wird nicht geschickt.** Ohne `zeitraum` gilt am
 * Endpunkt `48H` (E‑38), und die Antwort nennt das gewählte Paar. Eine Vorgabe
 * im Frontend wäre ein zweiter Standardwert und liefe dem ersten hinterher.
 */
export function holeProzessbaum(zeitraum: Rollupzeitraum | null): Promise<Prozessbaum> {
  return hole<Prozessbaum>(`/prozesse/baum${zeitraum === null ? "" : `?zeitraum=${zeitraum}`}`);
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

/**
 * Die Belegarten, die dieser Mandant zur Auswahl bekommt.
 *
 * **Ohne Parameter, und ohne Mandanten-ID** (Regel M1): Der Mandant kommt aus der
 * Sitzung. Eine leere Liste ist eine Antwort und kein Fehler — drei Mandanten
 * haben keinen konfigurierten Typ und suchen typlos.
 */
export function holeBamTypen(): Promise<BamTyp[]> {
  return hole<BamTyp[]>("/bam/typen");
}

/**
 * Die Belegsuche.
 *
 * **Kein Cursor und kein `limit`.** Der Endpunkt liefert höchstens 50 Treffer und
 * sagt, wenn es mehr gäbe; wer mehr sehen will, verengt den Zeitraum oder nennt
 * eine zweite Nummer — und die zweite Nummer ist die billigere Verengung
 * (`docs/bam-suche.md` §5).
 *
 * @param abfrage die fertige Zeichenkette aus `suche.ts`; sie trägt die Begriffe
 *   als **wiederholten** Parameter, genau wie der Endpunkt sie erwartet
 */
export function holeBamSuche(abfrage: string): Promise<BamSuchergebnis> {
  return hole<BamSuchergebnis>(`/bam/suche${abfrage}`);
}

/**
 * Die Artefakte einer Nachricht — nach Nutzdaten und Protokollen geteilt, ohne
 * jeden Filestore-Verweis.
 *
 * **Kein Zeitfenster und kein Cursor.** Die Menge ist über einen
 * Primärschlüssel benannt; es sind drei bis fünfzehn Zeilen zu einer benannten
 * Nachricht (M55) und kein Ausschnitt aus einem Bestand.
 *
 * **Dieser Aufruf holt keine Datei.** Ob hinter einem Verweis noch etwas liegt,
 * sagt die Liste nicht — das zu beantworten kostete drei bis fünfzehn
 * SOAP-Abrufe je Nachricht.
 */
export function holeArtefakte(messageId: string): Promise<Artefaktliste> {
  return hole<Artefaktliste>(`/nachrichten/${encodeURIComponent(messageId)}/dateien`);
}

/**
 * Der Inhalt **eines** Artefakts, als Text.
 *
 * **Erst beim Öffnen der Ansicht**, nie mit der Liste: Dahinter steht ein
 * SOAP-Abruf gegen die Ablage (38 bis 244 ms je Datei, M66/M60), und ein
 * Vorabholen aller Artefakte einer Nachricht kostete bis zu fünfzehn davon.
 *
 * **Die `artefaktId` wird kodiert**, obwohl alle vorkommenden Zeichen in einem
 * URL-Pfad unreserviert sind: Sie kommt aus der Adresszeile und damit von
 * außen. Was von außen kommt, wird kodiert — sonst entscheidet ein Schrägstrich
 * in der Kennung, welcher Endpunkt gerufen wird.
 */
export function holeArtefaktinhalt(
  messageId: string,
  artefaktId: string,
): Promise<Artefaktanzeige> {
  return hole<Artefaktanzeige>(
    `/nachrichten/${encodeURIComponent(messageId)}/dateien/${encodeURIComponent(artefaktId)}/inhalt`,
  );
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
