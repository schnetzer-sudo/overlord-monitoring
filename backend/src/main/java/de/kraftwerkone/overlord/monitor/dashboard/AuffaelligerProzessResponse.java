package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;

/**
 * Eine Zeile des Blocks „Zuletzt aufgefallen" (Block 6) — <b>ein Prozess je Zeile</b>.
 *
 * <h2>⚠️ Der Antwortrumpf dieses Blocks hat sich am 04.09.2026 geaendert (E‑90)</h2>
 *
 * <p>Hier stand {@code AuffaelligeNachrichtResponse} mit {@code messageId}, {@code status}, {@code
 * statusKind} und {@code sosName} — eine Zeile je <b>Nachricht</b>. Der Block zeigte damit zehnmal
 * dieselbe Auskunft, sobald ein Prozess mehr als zehn Fehler im Fenster hatte, und das ist der
 * Normalfall: Bei {@code NEXANS} ueber 48 Stunden stammen 49 der 50 Fehler aus <b>einem</b>
 * Prozess.
 *
 * <p><b>Was dabei verloren geht, ist benannt und nicht uebersehen:</b> die Kennung der einzelnen
 * Nachricht und ihr Rohstatus. Beides fuehrt in die <b>Nachrichtenliste</b>, gefiltert auf diesen
 * Prozess — einen Klick entfernt und dort vollstaendig, statt hier in zehn gleichen Zeilen.
 * <b>Offener Punkt 136 ist damit gegenstandslos</b>: Er verlangte den Rohstatus je Zeile zurueck;
 * eine Zeile, die einen ganzen Prozess zusammenfasst, hat keinen.
 *
 * <p><b>{@code kategorie} bleibt.</b> Regel Q3 verlangt, dass Problemkategorien getrennt und nie zu
 * „Problem" zusammengefasst werden. Heute ist es eine Aufzaehlung mit einem Wert (offener Punkt
 * 133); kommt je eine zweite zurueck, steht hier ihr Platz.
 *
 * @param processId die Prozesskennung — der Filterwert fuer die Nachrichtenliste
 * @param processName der Klarname aus {@code Process.ProcessName}; darf {@code null} sein (Regel
 *     Q4). <b>Kein Ersatztext im Backend</b>
 * @param anzahl wie viele auffaellige Nachrichten dieser Prozess im Fenster hat
 * @param zuletzt der <b>juengste</b> Zeitpunkt darunter, als UTC-Zeitpunkt
 * @param kategorie warum die Zeile hier steht
 */
public record AuffaelligerProzessResponse(
    String processId, String processName, int anzahl, Instant zuletzt, Auffaelligkeit kategorie) {}
