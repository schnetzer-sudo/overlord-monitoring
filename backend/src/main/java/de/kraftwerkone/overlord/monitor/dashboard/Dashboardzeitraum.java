package de.kraftwerkone.overlord.monitor.dashboard;

import de.kraftwerkone.overlord.monitor.common.Zeitfenster;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Die drei Zeitraumpaare des Dashboards — <b>Fensterbreite und Eimerbreite zusammen</b>, weil das
 * eine ohne das andere keine Ansicht ergibt.
 *
 * <p><b>Jedes Paar liest eine eigene Rollup-Ebene</b>, und das ist der ganze Grund, warum es drei
 * Ebenen gibt ({@code docs/rollup.md} §2, §9a, §9d):
 *
 * <table>
 *   <caption>Paar, Ebene, Kosten</caption>
 *   <tr><th>Paar</th><th>Eimer</th><th>Ebene</th><th>Verlauf / Verteilung, {@code NEXANS}</th></tr>
 *   <tr><td>{@code 48H}</td><td>48 Stunden</td><td>{@code message_rollup}</td>
 *       <td>6,8 / 10,7 ms (M94)</td></tr>
 *   <tr><td>{@code 30T}</td><td>30 Tage</td><td>{@code message_rollup_tag}</td>
 *       <td>46,0 / 62,5 ms (rollup.md §9a)</td></tr>
 *   <tr><td>{@code 12M}</td><td>12 Monate</td><td>{@code message_rollup_monat}</td>
 *       <td>65,4 / 88,7 ms (M107)</td></tr>
 * </table>
 *
 * <p><b>Ueber die Stundenebene kostete {@code 12M} 2,2 bis 2,5 Sekunden</b> — das Vier- bis
 * Fuenffache des Budgets der ganzen Landingpage. Wer hier eine Ebene aendert, aendert eine
 * gemessene Entscheidung.
 *
 * <h2>Die Fenstergrenzen: ganze Eimer, obere Grenze ausschliessend</h2>
 *
 * <p>Beide Grenzen liegen auf einer <b>Eimergrenze</b>, und die obere ist der Anfang des
 * <i>naechsten</i> Eimers — der angebrochene Eimer, in dem {@code jetzt} liegt, gehoert dazu.
 * Genauso setzt der Rollup-Lauf sein Fenster ({@code RollupFenster.ausgedehnt}), und genauso hat
 * M94 gemessen.
 *
 * <p><b>Das unterscheidet sich absichtlich vom Listen-Endpunkt.</b> Der loest {@code zeitraum}
 * gegen die Anwendungsuhr auf, auf die Sekunde genau ({@code Zeitfenster.aufloesen}). Fuer eine
 * Liste ist das richtig; fuer ein Diagramm waere es falsch: Der erste und der letzte Balken waeren
 * angebrochen und trotzdem so hoch gezeichnet wie ein ganzer.
 *
 * <h2>Der Code steht in der URL und nicht der Java-Name</h2>
 *
 * <p>{@code 48H} laesst sich nicht als Java-Bezeichner schreiben. Die Aufzaehlung heisst deshalb
 * {@link #STUNDEN_48} und traegt ihren Code als Feld — dieselbe Bauform wie {@code
 * common/Zeitraum}, und aus demselben Grund: Der Wert in der URL und der Name im Code duerfen sich
 * unabhaengig voneinander aendern.
 */
public enum Dashboardzeitraum {

  /** 48 Stunden in Stundeneimern, gelesen aus {@code message_rollup}. */
  STUNDEN_48("48H", 48),

  /** 30 Tage in Tageseimern, gelesen aus {@code message_rollup_tag}. */
  TAGE_30("30T", 30),

  /** 12 Monate in Monatseimern, gelesen aus {@code message_rollup_monat}. */
  MONATE_12("12M", 12);

  private final String code;
  private final int eimer;

  Dashboardzeitraum(String code, int eimer) {
    this.code = code;
    this.eimer = eimer;
  }

  /** Der Wert, wie er in der URL und in der Antwort steht. */
  public String code() {
    return code;
  }

  /**
   * Wie viele Eimer das Paar umfasst — der Nenner der Belegungsprobe ({@code DashboardService}).
   */
  public int eimer() {
    return eimer;
  }

  /**
   * Die Reihe, in der das Standardfenster gesucht wird: erst das engste, dann das naechstweitere.
   *
   * <p>Sie ist die Reihenfolge der Aufzaehlung und keine zweite Liste — eine zweite Liste liefe
   * irgendwann auseinander.
   */
  public static List<Dashboardzeitraum> reihe() {
    return List.of(values());
  }

  /**
   * Das Fenster dieses Paares, gegen die <b>Anwendungsuhr</b> aufgeloest (Regel Z1).
   *
   * <p>Gerechnet wird rueckwaerts vom Eimer, in dem {@code jetzt} liegt: Er ist der letzte und
   * gehoert dazu, davor kommen {@code eimer - 1} weitere. Die obere Grenze ist der Anfang des
   * naechsten Eimers und <b>ausschliessend</b>.
   *
   * @param jetzt Zeitpunkt aus der Anwendungsuhr, in Wanduhrzeit des Quellservers
   */
  public Zeitfenster fenster(LocalDateTime jetzt) {
    return switch (this) {
      case STUNDEN_48 -> {
        LocalDateTime letzter = jetzt.truncatedTo(ChronoUnit.HOURS);
        yield new Zeitfenster(letzter.minusHours(eimer - 1L), letzter.plusHours(1));
      }
      case TAGE_30 -> {
        LocalDateTime letzter = jetzt.toLocalDate().atStartOfDay();
        yield new Zeitfenster(letzter.minusDays(eimer - 1L), letzter.plusDays(1));
      }
      case MONATE_12 -> {
        LocalDateTime letzter = jetzt.toLocalDate().withDayOfMonth(1).atStartOfDay();
        yield new Zeitfenster(letzter.minusMonths(eimer - 1L), letzter.plusMonths(1));
      }
    };
  }

  /**
   * Der Code aus der URL. {@code null} bedeutet <b>nicht angegeben</b> — dann waehlt der Endpunkt
   * selbst ({@code DashboardService.standardfenster}).
   *
   * @throws FachlicheAusnahme {@code 400}, wenn der Code keinem Paar entspricht
   */
  public static Dashboardzeitraum ausCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    return Arrays.stream(values())
        .filter(paar -> paar.code.equalsIgnoreCase(code.trim()))
        .findFirst()
        .orElseThrow(
            () ->
                new FachlicheAusnahme(
                    HttpStatus.BAD_REQUEST,
                    "zeitraum-unbekannt",
                    "Zeitraum unbekannt",
                    "Erlaubt sind 48H, 30T und 12M.",
                    "Unbekannter Dashboard-Zeitraum: " + code));
  }
}
