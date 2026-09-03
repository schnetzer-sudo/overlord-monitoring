import { hole } from "@/lib/http";
import type { Fenster, Rollupzeitraum } from "@/lib/rollupzeitraum";
import type { Statusart } from "@/lib/status-farbe";

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
 * Die einzigen beiden Felder der ganzen Antwort, die *nicht ermittelbar*
 * zurückgeben dürfen (`docs/dashboard.md` §5).
 *
 * **Sie fallen zusammen.** Fällt eine, ist auch die andere `null` — sie stehen
 * als Paar nebeneinander, und eine Kachel mit einer Zahl und einer Lücke lädt zu
 * einer Rechnung ein, die nicht aufgeht. **`ermittelbar: false` ist nicht `0`:**
 * Null hieße „es hängt nichts", und das ist in einem Überwachungswerkzeug die
 * schlimmste falsche Antwort.
 */
export type Ueberfaelligkachel = {
  imFenster: number | null;
  /** Ohne Zeitfenster, und das ist der Sinn: Gefragt ist, was *außerhalb* hängt. */
  insgesamt: number | null;
  ermittelbar: boolean;
};

export type Kacheln = {
  nachrichten: number;
  fehler: Fehlerkachel;
  ueberfaellig: Ueberfaelligkachel;
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

/** Die beiden Problemkategorien, die dieser Block führt — nie zu „Problem" vereint (Regel Q3). */
export type Auffaelligkeit = "FEHLER" | "UEBERFAELLIG";

export type AuffaelligeNachricht = {
  messageId: string;
  zeitpunkt: string;
  /** Der Rohwert des Altsystems. */
  status: string;
  statusKind: Statusart;
  kategorie: Auffaelligkeit;
  processId: string | null;
  /** Darf `null` sein — „nicht zugeordnet heißt nicht zugeordnet". */
  sosName: string | null;
};

/**
 * Der Stand des Rollups. **`null`, solange es keinen abgeschlossenen,
 * fehlerfreien Lauf gibt** — und auch dann darf `beendetAm` fehlen.
 */
export type Stand = { beendetAm: string | null; art: string | null };

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
  zuletztAufgefallen: AuffaelligeNachricht[];
  stand: Stand | null;
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
