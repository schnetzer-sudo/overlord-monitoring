package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * <b>Die Stundenbildung des Rollups</b> — {@code DATE_FORMAT(zeitstempel, '%Y-%m-%d %H:00:00')},
 * woertlich die Fassung aus M88, an genau einer Stelle.
 *
 * <p><i>(seit 17.09.2026, Live-Rest Teil A.)</i> Bis dahin stand der Ausdruck als private Konstante
 * in {@code rollup/RollupLeseRepository}. Der Live-Rest zaehlt den Verkehr seit dem letzten Lauf
 * aus {@code Message} mit <b>derselben</b> Stundenbildung — sie darf nicht nachgebaut werden, sonst
 * liefen Rollup und Live-Zaehlung an einem Eimerrand auseinander, und ein Fachpaket importiert
 * nicht aus {@code rollup}. Der Ausdruck liegt deshalb hier; {@code RollupLeseRepository} ruft ihn
 * und rendert Zeichen fuer Zeichen denselben Text wie vorher ({@code RollupStatementsTest}).
 *
 * <p><b>Ohne generierte Typen.</b> Das Feld kommt vom Aufrufer, wie bei {@code
 * MessageStatusClassifier.bedingung(Field)} — so bleibt {@code common} frei von {@code
 * jooq.glassfish}, das nur Repository-Klassen anfassen ({@code PaketstrukturTest}).
 *
 * <p><b>Warum ein {@code VARCHAR} und kein {@code DATETIME}:</b> {@code DATE_FORMAT} liefert eine
 * Zeichenkette. Ein {@code CAST(... AS DATETIME)} darum herum waere lesbarer, aber es waere eine
 * Abweichung von dem, was gemessen ist (M88, {@code docs/rollup.md} §7) — und der Gewinn waere
 * null, weil die Umwandlung in Java eine Zeile kostet ({@link #lies(String)}).
 */
public final class Stundeneimer {

  /** Das Format, das {@link #ausdruck(Field)} liefert. */
  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private Stundeneimer() {}

  /**
   * Der Stundeneimer eines Zeitstempels, als SQL-Ausdruck — im {@code SELECT} wie im {@code GROUP
   * BY}.
   */
  public static Field<String> ausdruck(Field<LocalDateTime> zeitstempel) {
    return DSL.field("date_format({0}, '%Y-%m-%d %H:00:00')", SQLDataType.VARCHAR, zeitstempel);
  }

  /** Die Gegenrichtung: der gelieferte Text als Stundenanfang. */
  public static LocalDateTime lies(String text) {
    return LocalDateTime.parse(text, FORMAT);
  }
}
