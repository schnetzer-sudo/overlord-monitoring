package de.kraftwerkone.overlord.monitor.common;

import java.util.List;
import java.util.function.Function;

/**
 * Die einheitliche Huelle jeder paginierten Antwort (Richtlinie §5.4).
 *
 * <p><b>Es gibt kein {@code total}.</b> Eine Gesamtzahl ueber {@code Message} waere genau die
 * Live-Aggregation, die Regel L2 verbietet — und sie kostet mehr als die Seite selbst: Ein {@code
 * COUNT} ueber ein Jahresfenster liest den ganzen Bereich, waehrend die Seite nach {@code limit}
 * Zeilen abbricht. Stattdessen wird {@code limit + 1} gelesen: Kommt die Zusatzzeile, gibt es eine
 * weitere Seite. Der Aufrufer erfaehrt damit „es geht weiter", nicht „es sind 412.038" — und mehr
 * braucht er zum Blaettern nicht.
 *
 * @param items die Zeilen dieser Seite, hoechstens {@code limit}
 * @param nextCursor undurchsichtige Position der naechsten Seite, {@code null} wenn es keine gibt
 * @param hasMore ob es eine naechste Seite gibt
 */
public record Seite<T>(List<T> items, String nextCursor, boolean hasMore) {

  /**
   * Baut die Seite aus dem Ergebnis einer Abfrage mit {@code LIMIT limit + 1}.
   *
   * @param gelesen bis zu {@code limit + 1} Zeilen in Sortierreihenfolge
   * @param limit die angefragte Seitengroesse
   * @param position wie aus der letzten Zeile der Seite ihre Cursor-Position wird
   */
  public static <T> Seite<T> aus(List<T> gelesen, int limit, Function<T, Seitenposition> position) {
    boolean hasMore = gelesen.size() > limit;
    List<T> seite = hasMore ? List.copyOf(gelesen.subList(0, limit)) : List.copyOf(gelesen);
    String cursor = hasMore ? position.apply(seite.get(seite.size() - 1)).kodiere() : null;
    return new Seite<>(seite, cursor, hasMore);
  }
}
