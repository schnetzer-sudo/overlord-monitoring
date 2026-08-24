import { aendere, hole } from "@/lib/http";

/**
 * Der Prozess-Katalog: **die Fläche, auf der aus etwas Lesbarem etwas
 * Gruppierbares wird** (`docs/prozess-katalog.md` §1).
 *
 * **Kein Endpunkt hier bekommt eine Mandanten-ID** (Regel M1). Der Mandant kommt
 * aus der Sitzung; es gibt keinen Parameter dafür und es darf keiner entstehen.
 * Auch die `ProcessID` im Pfad ist **keine Berechtigung**: Wer eine fremde errät,
 * bekommt `404` — denselben Rumpf wie bei einer erfundenen.
 *
 * **Dieses Feature importiert nicht aus `features/sitzung` und nicht aus
 * `features/nachrichten`** (`docs/frontend-grundlagen.md` §8). Gemeinsames geht
 * über `components/` oder `lib/`; die Anzeigezone etwa über
 * `components/zeitzone.tsx`.
 *
 * **`/api/katalog` verlangt die Rolle `ADMIN`.** Ein Nutzer ohne sie bekommt
 * `403` mit dem Problemtyp `zugriff-verweigert`, und darauf gibt es keinen
 * zweiten Versuch (`lib/query-client.ts`). Die Ansicht zeigt dafür einen eigenen
 * Zustand statt einer roten Meldung.
 */

/** Die geschlossene Menge aus `catalog/Richtung`. */
export const RICHTUNGEN = ["EINGEHEND", "AUSGEHEND"] as const;

export type Richtung = (typeof RICHTUNGEN)[number];

export function istRichtung(wert: string | null | undefined): wert is Richtung {
  return wert !== null && wert !== undefined && (RICHTUNGEN as readonly string[]).includes(wert);
}

/**
 * Zwei Pflegezustände, und es sind bewusst nur zwei (E4).
 *
 * Zusammen mit dem Partnerfeld tragen sie **drei** Bedeutungen: „noch nicht
 * angesehen", „Vorschlag der Heuristik" und — bei `GEPFLEGT` mit leerem Feld —
 * „hingesehen, es gibt nichts". Ein dritter Status ist ausdrücklich verworfen.
 */
export type Pflegestatus = "OFFEN" | "GEPFLEGT";

/**
 * Woher der **Partner**vorschlag stammt — nicht die Richtung.
 *
 * Eine Zeile darf `KEINE` tragen und trotzdem eine Richtung haben; das ist bei
 * 224 `NEXANS`-Prozessen der Regelfall (`docs/prozess-katalog.md` §3.5). `KEINE`
 * heißt „geprüft, nichts abgeleitet" und nicht „noch nicht gelaufen".
 */
export type VorschlagHerkunft = "REGEL_A" | "REGEL_B" | "KEINE";

/** Eine Zeile der Pflegeliste — zehn Felder, so wie das Backend sie liefert. */
export type Katalogzeile = {
  processId: string;
  projectId: string;
  /** Darf `null` sein. Steht **für die Anzeige** da; sortiert wird nach den Kennungen (E6). */
  projectName: string | null;
  /** Darf `null` sein. Kein Ersatzschlüssel und keine zweite Chance für die Heuristik. */
  processName: string | null;
  /** `null` ist ein gültiger Wert — mit `GEPFLEGT` heißt er „hingesehen, es gibt nichts" (E4). */
  partner: string | null;
  richtung: Richtung | null;
  /** Niemals `null` — ohne Katalogzeile `OFFEN`. */
  pflegestatus: Pflegestatus;
  /** Niemals `null` — ohne Katalogzeile `KEINE`. */
  vorschlagHerkunft: VorschlagHerkunft;
  /**
   * **Drei Zustände, und `null` ist einer davon** (E14).
   *
   * `null` heißt „noch nie geprüft", `false` heißt „geprüft und ohne Verkehr".
   * Die beiden werden nie zusammengefasst: Der Filter aus E20 muss die
   * ungeprüften Zeilen **zeigen**, sonst verschwindet eine nie gemessene Zeile
   * aus beiden Filterstellungen und ist über die Oberfläche nicht mehr
   * erreichbar.
   */
  traegtNachrichten: boolean | null;
  /** UTC, `null`, solange kein Bestandslauf über die Zeile ging. */
  bestandGeprueftAm: string | null;
};

export const KATALOG_SCHLUESSEL = {
  /**
   * **`nurOffene` gehört in den Schlüssel**, denn es ist ein Anfrageparameter:
   * Eine andere Anfrage ist eine andere Antwort. Der zweite Filter steht
   * bewusst **nicht** hier — er wird im Browser gerechnet (E20) und ändert die
   * Anfrage nicht.
   */
  prozesse: (nurOffene: boolean) => ["katalog", "prozesse", nurOffene] as const,
  /**
   * Der Präfix beider Listenschlüssel.
   *
   * Nach einer Zuordnung wird **jede** zwischengespeicherte Fassung der Liste
   * aktualisiert — die mit und die ohne `nurOffene`. Nur die gerade sichtbare zu
   * setzen hieße, dass ein Haken hin und zurück die alte Zeile wieder
   * hervorholt, und zwar ohne Anfrage und ohne Hinweis.
   */
  prozesseBereich: ["katalog", "prozesse"] as const,
  /** Die Partnervorschläge. Stammdaten des Mandanten, entsprechend länger gehalten. */
  partner: ["katalog", "partner"] as const,
};

/**
 * Die Pflegeliste. **Ein Fetch, keine Paginierung, kein Zeitfenster** (E8).
 *
 * Bis zu 733 Zeilen beim größten Mandanten; L12 misst dafür 8,0 ms in der
 * Datenbank. Die Fortschrittszahl bekommt deshalb keine eigene Abfrage — die
 * volle Liste kommt zurück und wird vorne gezählt.
 */
export function holeKatalogzeilen(nurOffene: boolean): Promise<Katalogzeile[]> {
  return hole<Katalogzeile[]>(`/katalog/prozesse${nurOffene ? "?nurOffene=true" : ""}`);
}

/**
 * Die Partner-Auswahlliste, abgeleitet aus den Katalogzeilen des aktiven
 * Mandanten — **keine Tabelle `partner`** (E2).
 *
 * Leere Werte bleiben draußen: Ein leerer Partner ist ein gültiger
 * *Pflegezustand*, aber kein wählbarer *Wert*.
 */
export function holePartner(): Promise<string[]> {
  return hole<string[]>("/katalog/partner");
}

/**
 * Was ein `PUT` setzt — **beide Felder, immer**.
 *
 * `null` heißt leer und nicht „unverändert": Der Endpunkt setzt die Zeile als
 * Ganzes und macht sie damit `GEPFLEGT`. Ein leerer Partner ist genau deshalb
 * ein gültiger Wert und bedeutet „hingesehen, es gibt nichts" (E4) — ohne ihn
 * stünden Auffangprozesse dauerhaft auf „offen" und der Fortschritt erreichte
 * nie sein Ende.
 */
export type ZuordnenAnfrage = {
  partner: string | null;
  richtung: Richtung | null;
};

/**
 * Setzt Partner und Richtung **einer** Zeile (E19) — ein Aufruf je Zeile, kein
 * Sammelspeichern.
 *
 * Die Antwort ist die geänderte Zeile. Damit wird der Zwischenspeicher
 * aktualisiert und **nicht** die ganze Liste neu geholt: 733 Zeilen wegen einer
 * einzigen Änderung noch einmal zu ziehen, wäre teuer und nähme dem Nutzer
 * obendrein seine Seitenposition.
 *
 * **Die `processId` wird kodiert.** Sie kommt aus der Antwort des Backends und
 * darf trotzdem alles enthalten; ein roher Pfad wäre eine Annahme über fremde
 * Zeichenketten.
 *
 * Ein `404` heißt „gibt es nicht" und sagt **niemals** etwas über Berechtigung —
 * das Backend hält eine erfundene und eine fremde Kennung ununterscheidbar.
 */
export function zuordne(processId: string, anfrage: ZuordnenAnfrage): Promise<Katalogzeile> {
  return aendere<Katalogzeile>(`/katalog/prozesse/${encodeURIComponent(processId)}`, anfrage);
}
