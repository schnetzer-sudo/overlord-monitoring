package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;
import java.util.List;

/**
 * Ein Eimer des Verlaufs, aufgeschluesselt nach <b>Einordnung</b> (Block 1).
 *
 * @param eimer der Anfang des Eimers als UTC-Zeitpunkt — Stundenanfang, Tagesanfang oder
 *     Monatsanfang, je nach Zeitraumpaar. Umgerechnet aus der Wanduhrzeit des Quellservers ueber
 *     {@code Zeitpunkte.nachUtc}
 * @param gesamt die Summe ueber alle Einordnungen dieses Eimers — sie steht daneben, damit die
 *     Oberflaeche fuer die Hoehe eines Balkens nicht selbst addieren muss
 * @param einordnungen nur die Einordnungen, die in diesem Eimer <b>vorkommen</b>, in der
 *     Reihenfolge von {@code MessageStatusKind}. <b>Nicht</b> alle acht je Eimer: Das waeren bei 48
 *     Stunden 384 Eintraege, von denen die meisten null waeren
 */
public record VerlaufspunktResponse(
    Instant eimer, long gesamt, List<EinordnungszahlResponse> einordnungen) {

  public VerlaufspunktResponse {
    einordnungen = List.copyOf(einordnungen);
  }
}
