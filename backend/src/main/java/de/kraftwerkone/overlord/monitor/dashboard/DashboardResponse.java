package de.kraftwerkone.overlord.monitor.dashboard;

import java.util.List;

/**
 * Die Antwort der Landingpage — <b>eine Anfrage, eine Antwort</b>.
 *
 * <p><b>Kein Block wird nachgeladen.</b> Das ist keine Bequemlichkeit, sondern das Leistungsbudget:
 * Sechs Anfragen mit je einer Sitzungspruefung und je einem Verbindungsgriff kosten mehr als sechs
 * Abfragen auf einer Verbindung — und auf der Testkopie schreibt jede Anfrage zusaetzlich die
 * Sitzung fort.
 *
 * @param zeitraum das <b>gewaehlte</b> Paar als Code ({@code 48H}, {@code 30T}, {@code 12M}).
 *     <b>Immer gesetzt, auch wenn der Aufrufer keines genannt hat</b> — sonst wuesste die
 *     Oberflaeche nicht, was sie hervorheben und in die URL schreiben soll
 * @param fenster die tatsaechlich gelesenen Grenzen, in UTC
 * @param verlauf Block 1 — je Eimer die Aufschluesselung nach Einordnung
 * @param kacheln Bloecke 2 und 3 — Nachrichten und Fehler
 * @param verteilung Block 5 — Partner oder Richtung, Top 10 und zwei Restzeilen
 */
public record DashboardResponse(
    String zeitraum,
    FensterResponse fenster,
    List<VerlaufspunktResponse> verlauf,
    KachelnResponse kacheln,
    VerteilungResponse verteilung) {

  public DashboardResponse {
    verlauf = List.copyOf(verlauf);
  }
}
