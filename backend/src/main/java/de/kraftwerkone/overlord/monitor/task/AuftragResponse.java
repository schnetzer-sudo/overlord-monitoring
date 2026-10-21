package de.kraftwerkone.overlord.monitor.task;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Ein Auftrag, wie Liste, Einzelabruf und Anlegen ihn liefern ({@code docs/tasks.md} §6.5).
 *
 * <p>{@code name} und das Lesen der Rueckgabe kommen aus der <b>Kopie</b> der Definition im
 * Auftrag, nie aus der aktuellen Typzeile (E‑1023). Zeitpunkte in UTC, ISO 8601 mit {@code Z}.
 *
 * @param status der gespeicherte Status, roh
 * @param einordnung abgeleitet im {@link TaskStatusClassifier}
 * @param felder je Feld der Kopie Name und {@code label}, in der Reihenfolge der Definition — seit
 *     A4, damit die Uebersicht die Werte beschriften kann, ohne die aktuelle Typzeile zu lesen
 *     (E‑1120)
 */
public record AuftragResponse(
    String taskId,
    String typ,
    String name,
    String aktion,
    String status,
    Einordnung einordnung,
    Instant eingestelltAm,
    Instant abgeholtAm,
    Instant ausgefuehrtAm,
    String fehlercode,
    String fehlertext,
    List<EintragResponse> eintraege,
    List<FeldResponse> felder) {

  /**
   * @param werte Feldname auf Wert, nur Felder mit Wert, in der Reihenfolge der Definition
   * @param schluessel der Schluessel als Text nach der Kopie; {@code null} ohne Schluessel
   * @param rueckgabe die Zeile der Rueckgabe mit demselben Index, ohne CR und Leerraum am Rand;
   *     {@code null}, wenn es sie nicht gibt
   * @param ergebnis wie die Zeile gegen die Kopie steht; {@code null} ohne Zeile
   */
  public record EintragResponse(
      Map<String, String> werte, String schluessel, String rueckgabe, Zeilenergebnis ergebnis) {}

  /** Ein Feld der Kopie, nur Name und {@code label} — Regeln und Muster gehen nicht hinaus. */
  public record FeldResponse(String name, String label) {}
}
