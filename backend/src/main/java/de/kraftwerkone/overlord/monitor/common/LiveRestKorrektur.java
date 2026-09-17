package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Die Korrektur je {@code (stunde, process_id, message_status)}</b> — minus die Zeilen aus
 * {@code message_rollup} im Live-Bereich, plus die Zaehlung aus {@code Message} ueber denselben
 * Bereich —, verrechnet als reine Funktion ohne Datenbank.
 *
 * <h2>Warum abziehen statt neu zerlegen</h2>
 *
 * <p>Tages- und Monatsebene entstehen im selben Lauf und in derselben Transaktion aus genau diesen
 * Stundeneimern ({@code docs/rollup.md} §5). Eine Korrektur, die je Stundeneimer sagt, was zu viel
 * und was zu wenig in der Stundenebene steht, stimmt deshalb fuer <b>jede</b> Ebene: Ein
 * Verbraucher ordnet sie seinen eigenen Eimern zu (der Baum seinem Fenster, das Dashboard seinen
 * Verlaufseimern), und die gemessenen Kennzahlen-Statements bleiben unberuehrt.
 *
 * <h2>Was die Verrechnung tut, und was nicht</h2>
 *
 * <ul>
 *   <li>Eine Zeile aus dem Rollup geht mit <b>negativem</b> Vorzeichen ein, eine aus der Quelle mit
 *       positivem. Gleiche Schluessel werden summiert; was sich zu null aufhebt, faellt weg.
 *   <li><b>Ein Statuswechsel innerhalb eines Eimers</b> — der Fall, fuer den es die Korrektur gibt
 *       — ergibt zwei Zeilen: minus der alte Status, plus der neue. Die Nachrichtenzahl eines
 *       Prozesses bleibt dadurch gleich, die Fehlerzahl aendert sich, sobald der Verbraucher die
 *       Rohwerte einordnet.
 *   <li>Die <b>juengste Stunde mit Live-Verkehr</b> je Prozess kommt ausschliesslich aus der Quelle
 *       — sie ist die letzte Bewegung, die der Rollup noch nicht kennt, und sie steht auch dann da,
 *       wenn sich die Zeilen zu null aufheben.
 *   <li><b>Gruppiert wird nach dem Rohwert, Zeichen fuer Zeichen.</b> Die Spalte vergleicht mit
 *       {@code utf8mb4_general_ci}; kaeme derselbe Status aus Rollup und Quelle in zwei
 *       Schreibweisen, blieben hier zwei Zeilen. Fuer die Summe je Prozess ist das folgenlos, und
 *       die Einordnung stellt den Rohwert ohnehin hoch.
 * </ul>
 *
 * @param zeilen die Korrekturzeilen, vorzeichenbehaftet, ohne Nullzeilen, in der Reihenfolge des
 *     ersten Auftretens
 * @param juengsteStundeJeProzess je Prozess der juengste Stundeneimer, in dem die Quelle Verkehr
 *     zaehlt; Prozesse ohne Live-Verkehr fehlen
 */
public record LiveRestKorrektur(
    List<LiveRestZeile> zeilen, Map<String, LocalDateTime> juengsteStundeJeProzess) {

  /** Nichts zu korrigieren — bei {@code NICHT_NOETIG} und {@code AUSGESETZT}. */
  public static final LiveRestKorrektur KEINE = new LiveRestKorrektur(List.of(), Map.of());

  public LiveRestKorrektur {
    zeilen = List.copyOf(zeilen);
    juengsteStundeJeProzess = Map.copyOf(juengsteStundeJeProzess);
  }

  /**
   * Verrechnet die beiden Lesungen des Live-Bereichs.
   *
   * @param ausDemRollup die Zeilen aus {@code message_rollup} im Live-Bereich, positiv gezaehlt
   * @param ausDerQuelle die Zaehlung aus {@code Message} ueber denselben Bereich, positiv gezaehlt
   */
  public static LiveRestKorrektur aus(
      List<LiveRestZeile> ausDemRollup, List<LiveRestZeile> ausDerQuelle) {
    Map<Schluessel, Long> summe = new LinkedHashMap<>();
    for (LiveRestZeile zeile : ausDemRollup) {
      summe.merge(Schluessel.von(zeile), -zeile.anzahl(), Long::sum);
    }
    Map<String, LocalDateTime> juengste = new HashMap<>();
    for (LiveRestZeile zeile : ausDerQuelle) {
      summe.merge(Schluessel.von(zeile), zeile.anzahl(), Long::sum);
      if (zeile.anzahl() > 0) {
        juengste.merge(
            zeile.processId(), zeile.stunde(), (bisher, neu) -> neu.isAfter(bisher) ? neu : bisher);
      }
    }
    List<LiveRestZeile> zeilen = new ArrayList<>();
    for (Map.Entry<Schluessel, Long> eintrag : summe.entrySet()) {
      if (eintrag.getValue() != 0) {
        Schluessel schluessel = eintrag.getKey();
        zeilen.add(
            new LiveRestZeile(
                schluessel.stunde(),
                schluessel.processId(),
                schluessel.messageStatus(),
                eintrag.getValue()));
      }
    }
    return new LiveRestKorrektur(zeilen, juengste);
  }

  private record Schluessel(LocalDateTime stunde, String processId, String messageStatus) {
    static Schluessel von(LiveRestZeile zeile) {
      return new Schluessel(zeile.stunde(), zeile.processId(), zeile.messageStatus());
    }
  }
}
