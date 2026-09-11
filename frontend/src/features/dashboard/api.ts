import { hole } from "@/lib/http";
import type { Fenster, Rollupzeitraum } from "@/lib/rollupzeitraum";
import type { Ablagenzustand, Dienstzustand, Statusart } from "@/lib/status-farbe";

/**
 * Die Landingpage — **ein Aufruf, eine Antwort** (`docs/dashboard.md` §1).
 *
 * **Kein Endpunkt hier bekommt eine Mandanten-ID** (Regel M1). Der Mandant kommt
 * aus der Sitzung; ein `?mandant=…` wäre nicht falsch, sondern wirkungslos.
 *
 * **Kein Block lädt nach.** Das ist keine Bequemlichkeit, sondern das
 * Leistungsbudget: Sieben Abfragen auf einer Verbindung kosten weniger als sechs
 * Anfragen mit je einer Sitzungsprüfung — und auf der Testkopie schreibt jede
 * Anfrage zusätzlich die Sitzung fort. Auch der Umschalter der Verteilung lädt
 * nichts nach: Ein Sichtwechsel ist **dieselbe Adresse mit anderem Parameter**
 * und damit ein neuer Aufruf, kein Teilnachladen.
 *
 * **Dieses Feature importiert nicht aus `features/nachrichten`** und nicht aus
 * `features/sitzung` (`docs/frontend-grundlagen.md` §8). Was beide brauchen,
 * steht in `lib/` — die Einordnungen und ihre Farbrollen in `lib/status-farbe.ts`,
 * die Anzeigezone über `components/zeitzone.tsx`.
 *
 * **Die Typen hier spiegeln die Records des Backends** und rechnen nichts nach.
 * Wo das Backend `null` schicken darf, steht `| null`; wo es eine geschlossene
 * Menge liefert, steht die Menge und keine Zeichenkette.
 */

/**
 * Die drei Paare aus Fensterbreite und Eimerbreite stehen seit dem 02.09.2026 in
 * `lib/rollupzeitraum.ts` und heißen dort `Rollupzeitraum` — wie im Backend seit
 * Entscheidung E‑44.
 *
 * **Der Grund ist die Prozessansicht**, die dieselben drei Codes braucht: Ein
 * Feature importiert nicht aus einem Nachbarfeature, also wandert der
 * gemeinsame Teil nach `lib` (`docs/frontend-grundlagen.md` §8). Hier steht
 * nichts mehr davon — auch keine Hülle, die den alten Namen weiterführte: Zwei
 * Namen für dieselbe Menge sind der Anfang zweier Mengen.
 *
 * **Ohne Angabe wählt der Endpunkt selbst** (`docs/dashboard.md` §3) und nennt
 * das gewählte Paar in der Antwort. Eine Vorgabe im Frontend wäre ein zweiter
 * Standardwert und liefe dem ersten irgendwann hinterher — deshalb steht auch in
 * `lib` keine.
 */

/**
 * Die zwei Sichten des Verteilungsblocks. **`PARTNER` ist die Vorgabe des
 * Endpunkts** und steht deshalb nicht in der URL (Ableitung aus Entscheidung
 * E‑n); `RICHTUNG` schon.
 */
export const VERTEILUNGSSICHTEN = ["PARTNER", "RICHTUNG"] as const;

export type Verteilungssicht = (typeof VERTEILUNGSSICHTEN)[number];

/** Die Vorgabe des Backends — hier nur zur Erinnerung, nicht als zweite Quelle. */
export const VERTEILUNG_VORGABE: Verteilungssicht = "PARTNER";

export function istVerteilungssicht(wert: string | null | undefined): wert is Verteilungssicht {
  return (
    wert !== null && wert !== undefined && (VERTEILUNGSSICHTEN as readonly string[]).includes(wert)
  );
}

/**
 * Die gelesenen Grenzen stehen seit dem 02.09.2026 als `Fenster` in
 * `lib/rollupzeitraum.ts` — die Prozessansicht bekommt dasselbe Feld, und ein
 * Feature importiert nicht aus einem Nachbarfeature.
 */
export type { Fenster };

/** Je Eimer nur die Einordnungen, die **vorkommen** — nie alle acht mit Nullen. */
export type Einordnungszahl = { einordnung: Statusart; anzahl: number };

export type Verlaufspunkt = {
  /** Der Anfang des Eimers, UTC. */
  eimer: string;
  gesamt: number;
  /**
   * Die Reihenfolge ist die der Aufzählung und über alle Eimer dieselbe. Eine
   * Oberfläche, die Farben **nach Position** vergäbe, bekäme trotzdem in jedem
   * Balken eine andere — deshalb tut diese es nicht (`lib/status-farbe.ts`).
   */
  einordnungen: Einordnungszahl[];
};

/**
 * Eine Fehlerart aus demselben Rohwert.
 *
 * **Beide Felder stehen da, und der Rohwert wird nicht ersetzt.** Ohne ihn wäre
 * *„Vom Partner abgelehnt"* eine Zeichenkette, an der sich nichts mehr
 * festmachen ließe.
 *
 * ⚠️ **`art` ist ein deutscher Festtext, wenn der Rohwert `COMMIT_REJECTED`
 * ist** (`MessageStatusClassifier.ABGELEHNT_VOM_PARTNER`). Beschriftet wird
 * deshalb über den **Rohwert** und nicht über dieses Feld — sonst stünde das
 * deutsche Wort auch im englischen Baum. Für alles andere ist `art` der
 * Namensteil hinter `ERROR_` beziehungsweise der unveränderte Rohwert, und
 * beides ist keine Übersetzung, sondern ein Wert (Regel Q4).
 */
export type Fehlerart = { rohwert: string; art: string; anzahl: number };

export type Fehlerkachel = { anzahl: number; arten: Fehlerart[] };

/**
 * Eine der beiden Kacheln für die **offenen** Zustände — *Läuft* und *Wartend*.
 * Beide haben dieselbe Gestalt und deshalb denselben Typ
 * (`docs/dashboard.md` §5).
 *
 * **Sie sind die einzigen Felder der ganzen Antwort, die *nicht ermittelbar*
 * zurückgeben dürfen.** Sie sind der einzige Teil, der zur Laufzeit live über
 * `Message` liest, wo die Zeitgrenze der Datenbank nach zehn Sekunden abräumt;
 * der Rest kommt aus unserer eigenen Tabelle. **Stirbt die Live-Abfrage, darf
 * nicht die ganze Seite sterben.**
 *
 * **Die beiden Kacheln fallen dabei *nicht* zusammen** — und das ist der
 * Unterschied zur alten Kachel *Überfällig*: Dort waren „im Zeitraum" und
 * „insgesamt" ein Paar, das man nebeneinander liest. *Läuft* und *Wartend* sind
 * zwei verschiedene Auskünfte aus zwei Statements. Fällt eine, steht die andere.
 *
 * **`ermittelbar: false` ist nicht `0`:** Null hieße „es läuft nichts", und das
 * ist in einem Überwachungswerkzeug die schlimmste falsche Antwort.
 */
export type OffeneKachel = {
  /** Ohne Zeitfenster, und das ist der Sinn: Gefragt ist, was *jetzt* offen ist. */
  anzahl: number | null;
  /**
   * Wie lange die älteste dieser Nachrichten schon steht, in ganzen Sekunden,
   * im Backend gegen die **Anwendungsuhr** gerechnet (Regel Z1) — **niemals
   * hier**: Im Profil `dev` steht sie Monate zurück.
   *
   * **`null` bei `anzahl = 0`** — ohne Zeile gibt es kein Alter, und eine `0`
   * hieße „seit null Sekunden". `null` auch, wenn nicht ermittelbar.
   */
  aeltesteSekunden: number | null;
  ermittelbar: boolean;
};

/**
 * Die vier Kacheln.
 *
 * ## `wartend` fehlt, wenn es den Zustand beim Mandanten nicht gibt (E‑74)
 *
 * **Das Feld ist entweder vollständig da oder gar nicht** — kein `null`, kein
 * `sichtbar: false`. Die Erscheinungsbedingung ist **strukturell**: Sie fragt
 * über `SOSAction`, ob überhaupt ein Ablauf des Mandanten suspendiert. Zeigt die
 * Kachel dann `0`, ist das eine Auskunft und kein Rauschen — *heute wartet
 * nichts* und *dieser Mandant wartet nie* sind zwei verschiedene Sätze.
 *
 * **Deshalb ist `wartend` optional und `laeuft` nicht.** Laufen kann jeder
 * Mandant.
 *
 * > ⚠️ **Fehlt der Schlüssel, wird keine Kachel gezeichnet** — kein Platzhalter,
 * > keine gedämpfte Kachel, kein „nicht verfügbar". Abwesenheit ist eine
 * > Auskunft über den **Mandanten**, `ermittelbar: false` eine über **uns**.
 * > Die beiden dürfen nie gleich aussehen; verschwände die Kachel bei einem
 * > Fehlschlag, würde ein Ausfall stillschweigend in eine strukturelle
 * > Behauptung übersetzt. **Der Endpunkt hält das ein:** Die
 * > Erscheinungsbedingung bekommt keinen Teilerfolg-Mechanismus — sie liest
 * > Stammdaten wie jede Mandantenkette, und bricht sie, ist die ganze Antwort
 * > ein Fehler (`docs/dashboard.md` §5).
 */
export type Kacheln = {
  nachrichten: number;
  fehler: Fehlerkachel;
  laeuft: OffeneKachel;
  wartend?: OffeneKachel;
};

export type Verteilungszeilenart = "WERT" | "UEBRIGE" | "NICHT_ZUGEORDNET";

/**
 * Eine Zeile des Verteilungsblocks. **Die Antwort trägt keinen Anzeigetext** für
 * die beiden Restzeilen: Das Backend stellt fest, die Oberfläche beschriftet
 * (Regel Q4).
 */
export type Verteilungszeile = {
  art: Verteilungszeilenart;
  /** Nur bei `WERT` gefüllt. */
  wert: string | null;
  anzahl: number;
  /** Nur bei `UEBRIGE` gefüllt: wie viele Werte darin stecken. */
  enthaltene: number | null;
};

export type Verteilung = { sicht: Verteilungssicht; zeilen: Verteilungszeile[] };

/**
 * Warum eine Nachricht in „Zuletzt aufgefallen" steht.
 *
 * ⚠️ **Seit dem 03.09.2026 nur noch ein Wert.** *Überfällig* ist mit E‑71
 * widerlegt und entfallen; der Block liest seither nur noch die Fehlerbedingung
 * und ist ein einziges Statement (`docs/dashboard.md` §7a). **Die Oberfläche
 * zeichnet je Zeile deshalb keine Kategoriekennzeichnung mehr** — eine Plakette,
 * die an jeder Zeile dasselbe sagt, unterscheidet nichts.
 *
 * **Das Feld bleibt trotzdem im Vertrag.** Regel Q3 verlangt, dass
 * Problemkategorien getrennt geführt und nie zu „Problem" zusammengefasst
 * werden; kommt je eine zweite zurück, steht hier ihr Platz. Dass hier heute
 * eine Aufzählung mit einem Wert steht, ist ein benannter Zwischenstand —
 * offener Punkt 133.
 */
export type Auffaelligkeit = "FEHLER";

/**
 * Eine Zeile aus „Zuletzt aufgefallen" — **ein Prozess, nicht eine Nachricht**
 * (Entscheidung **E‑90**, 04.09.2026).
 *
 * Bis dahin stand hier `AuffaelligeNachricht` mit `messageId`, `status`,
 * `statusKind` und `sosName`. Der Block zeigte damit zehnmal dieselbe Auskunft,
 * sobald ein Prozess mehr als zehn Fehler im Fenster hatte — und das ist der
 * Normalfall: Bei `NEXANS` über 48 Stunden stammen **49 der 50 Fehler aus einem
 * einzigen Prozess** (M146).
 */
export type AuffaelligerProzess = {
  processId: string;
  /** Der Klarname aus `Process.ProcessName`. Darf `null` sein (Regel Q4). */
  processName: string | null;
  /** Wie viele auffällige Nachrichten dieser Prozess im Fenster hat. */
  anzahl: number;
  /** Der **jüngste** Zeitpunkt darunter. */
  zuletzt: string;
  kategorie: Auffaelligkeit;
};

/**
 * Der Stand des Rollups. **`null`, solange es keinen abgeschlossenen,
 * fehlerfreien Lauf gibt** — und auch dann darf `beendetAm` fehlen.
 */
export type Stand = { beendetAm: string | null; art: string | null };

/**
 * Eine **Dienstlampe** — ein Dienst des Altsystems mit `ServiceTimeout > 0`
 * (`docs/dienste.md` §9).
 *
 * **Vier Spalten von `Service` stehen in keiner Antwort** (E‑122): kein
 * `ServiceName`, keine `ServiceDescription`, keine `ServiceLastStatusMessage`
 * und kein `ServiceConnectString`. Sie werden nicht einmal gelesen. Was den
 * Dienst benennt, ist allein die `serviceId`.
 */
export type Dienstlampe = {
  serviceId: string;
  zustand: Dienstzustand;
  /**
   * Der exakte Wert aus `Service.ServiceStatus`, **immer mitgeliefert** und
   * auch bei bekannten Werten (E‑118) — der Anker für `UNGEKLAERT` (Regel Q4).
   *
   * **`null` ist möglich:** Der Klassifizierer ordnet eine leere Spalte
   * `UNGEKLAERT` zu, und dann gibt es keinen Rohwert zu zeigen.
   */
  rohwert: string | null;
  /**
   * `Service.ServiceLastUpdate` — Wanduhrzeit der Quelle, im Backend nach UTC
   * gerechnet (`common/Zeitpunkte`). **Darf fehlen.**
   */
  stand: string | null;
  /**
   * Wie alt dieser Zeitpunkt ist, in ganzen Sekunden, gegen die
   * **Anwendungsuhr** des Backends gerechnet (E‑75) — **niemals hier**: Im
   * Profil `dev` steht sie Monate zurück.
   *
   * **`null`, wenn `stand` nach `jetzt` liegt.** Auf der Testkopie ist das der
   * Normalfall und kein Fehler: `MPSERVICEPROD01` trägt den 13.07.2026, der
   * Anker der Anwendungsuhr steht auf dem 30.12.2025.
   */
  alterSekunden: number | null;
};

/**
 * Ein geprüftes Ziel der Ablagenkachel — **eine Ablage, die in
 * `ServiceDefaultFileStore` eingetragen ist** (E‑119).
 *
 * > ⚠️ **Das ist eine Stichprobe und keine Zusicherung über alle benutzten
 * > Ablagen.** M53 hat gemessen, dass zu jedem Zeitpunkt **zwei** Ablagen
 * > gleichzeitig beschrieben werden; die Spalte nennt nur eine davon (M174
 * > Befund 2, **offener Punkt 166**). Deshalb nennt die Kachel ihre Ziele
 * > einzeln und sichtbar (**E‑132**), statt eine Überschrift zu tragen, die für
 * > alle Ablagen spräche.
 */
export type Ablagenziel = { serviceId: string; zustand: Ablagenzustand };

/**
 * Warum die Ablagenkachel `UNGEKLAERT` steht — **einer von fünf benannten
 * Gründen**, und bei `UNGEKLAERT` immer gesetzt (E‑125).
 *
 * Die Reihenfolge ist die ihrer Prüfung im Backend (`docs/dienste.md` §8) und
 * damit zugleich ihre Rangfolge: Die Frage nach dem **Beleg** steht vor der
 * Frage nach dem **Inhalt**. Ein Stand, der älter ist als zwei Takte, sagt
 * nichts über die Ablagen — in *jede* Richtung, und deshalb bleibt eine Kachel
 * weder auf ihrem letzten roten noch auf ihrem letzten grünen Stand stehen.
 */
export type Ablagengrund =
  | "ABGESCHALTET"
  | "NOCH_KEIN_DURCHGANG"
  | "STAND_VERALTET"
  | "KEIN_ZIEL_EINGETRAGEN"
  | "ZIEL_UNGEKLAERT";

/** Dieselben fünf als Liste zur Laufzeit — für die Vollständigkeit der Sätze. */
export const ABLAGENGRUENDE: readonly Ablagengrund[] = [
  "ABGESCHALTET",
  "NOCH_KEIN_DURCHGANG",
  "STAND_VERALTET",
  "KEIN_ZIEL_EINGETRAGEN",
  "ZIEL_UNGEKLAERT",
];

/**
 * Die Ablagenkachel. **Sie steht immer in der Antwort**, auch abgeschaltet
 * (E‑125) — eine fehlende Kachel wäre Abwesenheit, und Abwesenheit ist der
 * schwächste Kanal, den ein Zustand haben kann. Dieselbe Begründung wie bei
 * E‑74 und E‑81.
 */
export type Ablagen = {
  zustand: Ablagenzustand;
  /** Benannt bei `UNGEKLAERT`, sonst `null`. Ein „ungeklärt" ohne Grund wäre ein Achselzucken. */
  grund: Ablagengrund | null;
  /**
   * Was tatsächlich geprüft worden ist. **Leer in vier Lagen:** abgeschaltet,
   * ohne Durchgang, ohne eingetragenes Ziel — und bei einem veralteten Stand
   * dann, wenn schon der letzte Durchgang keines fand.
   */
  ziele: Ablagenziel[];
  /** Wann der letzte Durchgang war, UTC. `null`, solange es keinen gab. */
  geprueftAm: string | null;
  /**
   * **Der Beleg der Kachel** — wer wissen will, ob das grüne Ergebnis noch
   * etwas wert ist, liest hier nach, statt es zu glauben.
   */
  alterSekunden: number | null;
};

/**
 * Der plattformweite Block — **für jeden Mandanten identisch** (E‑116).
 *
 * Er sagt nichts über die Belege eines Mandanten, sondern über die **Anlage**,
 * auf der sie laufen, und das ist keine Lücke in der Mandantentrennung, sondern
 * sein Gegenstand. `DashboardIsolationDbIT` hält im Backend die **Gleichheit**
 * fest, nicht die Verschiedenheit.
 *
 * **Er kommt im selben Aufruf** und kostet ein Statement — die Lampen. Die
 * Ablagenkachel kostet keines: Ihr Zustand stammt aus einer echten Prüfung, die
 * **im Hintergrund** läuft (E‑120). Ein Abruf gegen eine abgeschaltete Ablage
 * dauert allein rund 2,7 Sekunden (M174); beim Aufruf gefragt, hinge die
 * Landingpage an den Zeitgrenzen fremder Knoten statt an den eigenen.
 */
export type Plattform = { dienste: Dienstlampe[]; ablagen: Ablagen };

export type Dashboard = {
  /** Das **gewählte** Paar, immer gesetzt — auch im Leerzustand. */
  zeitraum: Rollupzeitraum;
  fenster: Fenster;
  /**
   * Der Leerzustand, und er **unterscheidet nicht**: „im Zeitraum ist nichts
   * passiert" und „dieser Mandant hat keine Daten" sehen gleich aus
   * (`docs/dashboard.md` §6, bekannte Grenze 2). Gewollt.
   */
  leer: boolean;
  verlauf: Verlaufspunkt[];
  kacheln: Kacheln;
  verteilung: Verteilung;
  zuletztAufgefallen: AuffaelligerProzess[];
  stand: Stand | null;
  /**
   * Der neunte Block, seit dem 10.09.2026 (Schritt 10d) — **der einzige, der
   * nicht am Bestand eines Mandanten hängt**.
   */
  plattform: Plattform;
};

/**
 * Der Abfrageschlüssel.
 *
 * **Beide Parameter gehören hinein**, denn beide sind Anfrageparameter: Eine
 * andere Sicht ist eine andere Antwort. Der Mandant steht aus demselben Grund
 * **nicht** darin, aus dem er in keinem anderen Schlüssel steht — beim Wechsel
 * wird der gesamte Zwischenspeicher geleert und nicht invalidiert
 * (`lib/zwischenspeicher.ts`).
 *
 * `zeitraum` ist `null`, solange der Nutzer nicht geklickt hat. Das ist ein
 * **eigener** Schlüssel und nicht der des vom Endpunkt gewählten Paares: Der
 * Aufruf ohne Parameter ist eine andere Frage als der mit — er kostet den
 * Endpunkt zusätzlich die Belegungsprobe.
 */
export const DASHBOARD_SCHLUESSEL = {
  landingpage: (zeitraum: Rollupzeitraum | null, sicht: Verteilungssicht | null) =>
    ["dashboard", "landingpage", zeitraum, sicht] as const,
};

/**
 * Holt die ganze Landingpage.
 *
 * **Was nicht gewählt ist, wird nicht geschickt.** Ohne `zeitraum` wählt der
 * Endpunkt selbst; ein mitgeschicktes `verteilung=PARTNER` wäre die Vorgabe ein
 * zweites Mal. Dass ein Aufruf **mit** `zeitraum` den Endpunkt *weniger* kostet
 * als einer ohne, ist ein Nebeneffekt in die richtige Richtung — aber kein Grund,
 * einen Parameter zu setzen, den der Nutzer nicht ausgedrückt hat.
 */
export function holeDashboard(
  zeitraum: Rollupzeitraum | null,
  sicht: Verteilungssicht | null,
): Promise<Dashboard> {
  const parameter = new URLSearchParams();
  if (zeitraum !== null) {
    parameter.set("zeitraum", zeitraum);
  }
  if (sicht !== null) {
    parameter.set("verteilung", sicht);
  }
  const abfrage = parameter.toString();
  return hole<Dashboard>(`/dashboard${abfrage === "" ? "" : `?${abfrage}`}`);
}
