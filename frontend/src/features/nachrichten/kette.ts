import type { Kette, Kettenglied } from "./api";

/**
 * Die Kettenfläche des Detailpanels — **als reine Funktion, ohne React.**
 *
 * Sie beantwortet eine einzige Frage: **was hängt an dieser Nachricht.** Alles
 * hier ist Einteilung und Abzählung, und beides ist deshalb prüfbar
 * (`tests/kette.test.ts`) statt in einer Komponente verstreut.
 *
 * ## Eingeteilt wird nach der Flussrichtung, nicht nach der Richtung der API
 *
 * Das ist die eine Entscheidung, die dieses Modul trägt, und sie ist nicht
 * naheliegend. Der Endpunkt liefert zwei Listen — `aufwaerts` (Aufstieg) und
 * `abwaerts` (Abstieg) —, und für die **Aufteilung** decken sie sich mit dem,
 * was ein Nutzer erwartet: Der Aufstieg führt zur Wurzel, der Abstieg zu den
 * Teilen.
 *
 * **Für die Zusammenführung ist es genau andersherum** (`verkettung.md` §1):
 *
 * | Ich bin | API-Richtung | Was dort steht | Im Fluss ist das |
 * |---|---|---|---|
 * | Split-Kind | `aufwaerts` | meine Wurzel | woher ich komme |
 * | Split-Wurzel | `abwaerts` | meine Teile | was aus mir wurde |
 * | **Merge-Eingang** | `aufwaerts` | **das Ergebnis** | **was aus mir wurde** |
 * | **Merge-Ergebnis** | `abwaerts` | **meine Eingänge** | **woher ich komme** |
 *
 * Der Aufstieg folgt `TargetMessageID`, und die zeigt vom Eingang auf das
 * Ergebnis — also **mit** dem Datenfluss und nicht gegen ihn. Wer die beiden
 * API-Listen unverändert als „Kommt von" und „Wurde zu" beschriftete, schriebe
 * bei jedem Merge-Eingang das Gegenteil dessen hin, was passiert ist. Über
 * Fenster B sind das 38.628 Zeilen — kein Randfall.
 *
 * **Die Namen der beiden Listen sagen das inzwischen selbst.** Seit dem
 * 11.08.2026 heißen sie nach dem *Mechanismus* und nicht nach einer Bedeutung,
 * die nur bei der Aufteilung stimmt. Die Einteilung hier ändert sich dadurch
 * **nicht** — sie war nie falsch, sie stand nur allein gegen zwei irreführende
 * Feldnamen.
 *
 * Entschieden wird über **`beziehung` zusammen mit der Ebene**. Genau dafür
 * liefert Teil 1 dieses Feld ausdrücklich mit, statt es ableiten zu lassen.
 */

/**
 * Die beiden Abschnitte des Blocks.
 *
 * Mehr als zwei gibt es nicht: „woher" und „wohin" sind die Fragen, die ein
 * Nutzer vor einem Beleg stellt. Die Rolle selbst — Wurzel, Kind, Eingang,
 * Ergebnis — ist eine Aussage über die *Struktur* und bekommt keinen eigenen
 * Abschnitt.
 */
export type Abschnittsart = "kommtVon" | "wurdeZu";

export type Kettenabschnitt = {
  art: Abschnittsart;
  glieder: Kettenglied[];
  /**
   * Die Gesamtzahl für die Überschrift — oder `null`, wenn es keine gibt, die
   * dieser Abschnitt behaupten darf.
   *
   * Sie steht nur an dem Abschnitt, der die **Abwärtsrichtung** trägt: Für ihn
   * liefert der Endpunkt mit `abwaertsGesamt` eine gemessene, exakte Zahl
   * (M30‑1). Für den Aufstieg gibt es keine — seine Länge ist die der Liste,
   * und die ist bei `tiefeErreicht` gerade *nicht* die Gesamtzahl. Eine Zahl,
   * die der Endpunkt nicht liefert, behauptet die Überschrift nicht.
   */
  gesamt: number | null;
};

/**
 * In welchen Abschnitt ein Glied gehört.
 *
 * Aufwärts + Aufteilung und abwärts + Zusammenführung zeigen **entgegen** dem
 * Datenfluss, die beiden anderen **mit** ihm. Das ist der ganze Inhalt der
 * Bedingung: Stimmen Richtung und Beziehung überein, kommt das Glied her; sonst
 * ist es geworden.
 */
export function abschnittFuer(glied: Kettenglied): Abschnittsart {
  const aufwaerts = glied.ebene < 0;
  const aufteilung = glied.beziehung === "AUFTEILUNG";
  return aufwaerts === aufteilung ? "kommtVon" : "wurdeZu";
}

/**
 * Der Abschnitt, in dem die **Abwärtsglieder** stehen — oder `null`, wenn sie
 * sich auf beide verteilen.
 *
 * Nur dann trägt `abwaertsGesamt` an einer Überschrift die Wahrheit: Die Zahl
 * zählt beide Abwärtsrichtungen zusammen (Kinder **und** Merge-Eingänge), und
 * eine Zeile kann beides haben — 25 Zeilen sind zugleich Split-Wurzel und
 * Merge-Ergebnis (M30‑4). Verteilen sich die Glieder, ließe sich die Summe
 * keiner der beiden Überschriften zuordnen, ohne sie zu erfinden.
 */
export function abwaertsAbschnitt(abwaerts: readonly Kettenglied[]): Abschnittsart | null {
  const arten = new Set(abwaerts.map(abschnittFuer));
  return arten.size === 1 ? [...arten][0] : null;
}

/**
 * Die beiden Abschnitte in Anzeigereihenfolge — **„Kommt von" zuerst.**
 *
 * Die Herkunft steht oben, weil der Blick von der Nachricht aus rückwärts
 * beginnt: Erst wenn klar ist, woher ein Beleg kommt, ist die Frage, was aus
 * ihm wurde, überhaupt gestellt.
 *
 * **Innerhalb eines Abschnitts bleibt die gelieferte Reihenfolge stehen** —
 * erst die Aufwärtsglieder in der Ordnung des Aufstiegs (Ebene `-1` zuerst),
 * dann die Abwärtsglieder in ihrer Sortierung `(zeitpunkt, messageId)`. Hier
 * wird nicht ein zweites Mal sortiert: Die Ordnung des Abstiegs ist der
 * Sortierschlüssel des Cursors, und eine zweite Ordnung an einer zweiten Stelle
 * wäre genau die Drift, die das Blättern zerlegt.
 *
 * @param abwaerts die gezeigten Abwärtsglieder. Das ist entweder `kette.abwaerts`
 *   oder — nach dem Nachladen — das Ergebnis von {@link gezeigteAbwaertsglieder}.
 */
export function kettenabschnitte(
  kette: Kette,
  abwaerts: readonly Kettenglied[] = kette.abwaerts,
): Kettenabschnitt[] {
  const traegtGesamt = abwaertsAbschnitt(abwaerts);
  const glieder = [...kette.aufwaerts, ...abwaerts];

  return (["kommtVon", "wurdeZu"] as const).map((art) => ({
    art,
    glieder: glieder.filter((glied) => abschnittFuer(glied) === art),
    gesamt: traegtGesamt === art ? kette.abwaertsGesamt : null,
  }));
}

/**
 * Die Abwärtsglieder, die der Block zeigt: **die erste Seite aus `/kette` plus
 * die nachgeladenen** — in dieser Reihenfolge.
 *
 * Das ist die ganze Wirkung von `abwaertsCursor`. Vorher lieferte die erste
 * nachgeladene Seite dieselben fünfzig Zeilen noch einmal, weshalb der Block
 * `kette.abwaerts` **verwarf**, sobald eine Seite da war, und sofort eine
 * zweite nachzog. Jetzt setzt die erste nachgeladene Seite hinter der
 * gezeigten an, und ein Klick bringt fünfzig neue Zeilen in einer Anfrage.
 *
 * Nicht dedupliziert: Die Seiten überschneiden sich nicht mehr, und ein
 * `Set` hier würde eine Überschneidung stillschweigend verstecken, statt sie
 * auffallen zu lassen.
 *
 * @param seiten die Seiten des Blätter-Endpunkts, in der Reihenfolge, in der
 *   sie geholt wurden. Leer, solange niemand nachgeladen hat.
 */
export function gezeigteAbwaertsglieder(
  kette: Kette,
  seiten: readonly (readonly Kettenglied[])[],
): Kettenglied[] {
  return [...kette.abwaerts, ...seiten.flat()];
}

/**
 * Ob es eine Schaltfläche zum Nachladen gibt — **vor** dem ersten Klick.
 *
 * Zwei Bedingungen, und die zweite ist der Sonderfall aus M30‑6: Ohne Cursor
 * gibt es keine Position, ab der sich weiterblättern ließe, und eine
 * Schaltfläche ohne Ziel wäre eine Zusage, die niemand einlöst. Der Endpunkt
 * lässt den Cursor genau dann weg, wenn die letzte gelieferte Zeile keinen
 * Zeitpunkt trägt — gemessen 0 von 3.341.519 Mal.
 *
 * Nach dem ersten Klick entscheidet `hasNextPage`; dann trägt der
 * Blätter-Endpunkt die Auskunft selbst.
 */
export function nachladenMoeglich(kette: Kette): boolean {
  return kette.weitereVorhanden && kette.abwaertsCursor !== null;
}

/**
 * Ob der Block überhaupt etwas zu zeigen hat.
 *
 * **Ist nichts da, gibt es keinen Block** — keine Überschrift, kein leerer
 * Kasten, kein Platzhalter. Dieselbe Regel wie beim Eigenschaftenblock bei
 * `eigenschaftenAnzahl === 0`: Eine Fläche ohne Inhalt behauptet, es gäbe dort
 * etwas zu sehen.
 *
 * Die beiden Hinweise zählen mit: Eine Kette, die abbricht, ist etwas, das
 * gesagt werden muss — auch wenn kein einziges Glied dabei herauskam.
 */
export function hatInhalt(kette: Kette): boolean {
  return (
    kette.aufwaerts.length > 0 ||
    kette.abwaerts.length > 0 ||
    kette.tiefeErreicht ||
    kette.zyklusErkannt
  );
}
