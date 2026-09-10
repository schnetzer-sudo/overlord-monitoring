package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.Instant;
import java.util.List;

/**
 * Die <b>eine</b> Kachel fuer die Ablagen (E‑119).
 *
 * <p>Sie fasst zusammen, was {@link Ablagenpruefung} im letzten Durchgang festgestellt hat; die
 * Regeln der Zusammenfassung stehen in {@link Ablagenkachel}.
 *
 * @param zustand das zusammengefasste Ergebnis
 * @param grund <b>nur bei {@link Ablagenzustand#UNGEKLAERT} gesetzt</b>, dort aber immer:
 *     abgeschaltet, noch kein Durchgang, Stand veraltet, kein Ziel eingetragen, oder ein Ziel hat
 *     Daten geliefert. Ein „ungeklaert" ohne Grund waere ein Achselzucken
 * @param ziele je Ziel eine Zeile. <b>Leer, solange es keinen Stand gibt</b> — dann traegt {@link
 *     #grund} die Auskunft
 * @param geprueftAm der Zeitpunkt des letzten Durchgangs in UTC, aus der Anwendungsuhr (Regel Z1).
 *     {@code null}, wenn es keinen gibt
 * @param alterSekunden sein Abstand zu {@code jetzt} in ganzen Sekunden, gerechnet wie bei den
 *     Lampen. <b>Er ist der Beleg der Kachel</b>: Wer wissen will, ob das gruene Ergebnis noch
 *     etwas wert ist, liest hier nach, statt es zu glauben
 */
public record AblagenResponse(
    Ablagenzustand zustand,
    Ablagengrund grund,
    List<AblagenzielResponse> ziele,
    Instant geprueftAm,
    Long alterSekunden) {

  public AblagenResponse {
    ziele = List.copyOf(ziele);
  }
}
