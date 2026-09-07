package de.kraftwerkone.overlord.monitor.common;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Das Fenster der Prozessansicht — <b>eines der drei Paare oder ein freies Fenster</b>, und in
 * beiden Faellen eine Liste von {@link Segment Segmenten}, je eines auf einer Rollup-Ebene.
 *
 * <p><i>(seit 07.09.2026, Schritt 10c-4b.)</i> {@link Rollupzeitraum} bleibt bei drei Werten. Ein
 * vierter Wert {@code FREI} haette weder ein {@code fenster(jetzt)} noch eine Ebene, und das
 * vollstaendige {@code switch} ohne {@code default} in den Repositories verloere genau die
 * Eigenschaft, die es traegt (offener Punkt 113). Stattdessen dieser Typ, der <b>beides</b>
 * aufnimmt.
 *
 * <h2>Die drei Paare bleiben unzerlegt — und das ist eine bewusste Ungleichbehandlung</h2>
 *
 * <p>Ein Paar ergibt <b>genau ein</b> Segment auf der Ebene des Paares, mit dem Fenster aus {@link
 * Rollupzeitraum#fenster(LocalDateTime)}. Ein 48-Stunden-Fenster wird <i>nicht</i> zerlegt, obwohl
 * es ueber zwei Tage reicht: Es ist gemessen (M116) und gebaut, und sein Statementtext ist die
 * tragende Zusage von {@code ProzessbaumStatementsTest}. Zerlegt wird nur das freie Fenster.
 *
 * <h2>Warum das freie Fenster immer zerlegt wird</h2>
 *
 * <p>Nicht nur bei krummen Eingaben. M148 hat gezeigt, dass auch ein <b>tagesgenaues</b>
 * Jahresfenster auf der Tagesebene das Tor reisst (764,233 ms bei {@code NEXANS}), weil die
 * Tagesebene keinen Index traegt, ueber den der Optimierer beim Mandanten einsteigen koennte
 * (offener Punkt 138). Die Zerlegung ist deshalb <b>der</b> Weg fuer den freien Modus und kein
 * Sonderweg; sie liest im Boesfall 14.148 statt 281.090 Zeilen (M149, Faktor 19,9).
 *
 * <h2>Gerechnet wird ueber den Kalender, niemals ueber Dauern</h2>
 *
 * <p>Alle Grenzen sind <b>Wanduhrzeit des Quellservers</b> ({@code docs/rollup.md} §2): Ein
 * Tageseimer ist der Tag, wie ihn der Quellserver sieht, und ein Monatseimer der Monat. {@link
 * java.time.LocalDate} und {@link YearMonth} schreiten weiter; ein {@code plusHours(24)} auf einem
 * Zeitpunkt mit Zone traefe am Umstellungstag daneben, weil ein Tag dort 23 oder 25 Stundeneimer
 * hat.
 */
public final class Baumfenster {

  /** Der Code, den die Antwort fuer ein freies Fenster nennt. Er verraet keine Ebene. */
  public static final String CODE_FREI = "FREI";

  private final Rollupzeitraum paar;
  private final LocalDateTime von;
  private final LocalDateTime bisAusschliessend;

  private Baumfenster(Rollupzeitraum paar, LocalDateTime von, LocalDateTime bisAusschliessend) {
    this.paar = paar;
    this.von = von;
    this.bisAusschliessend = bisAusschliessend;
  }

  /** Eines der drei Paare — sein Fenster wird erst gegen {@code jetzt} aufgeloest. */
  public static Baumfenster paar(Rollupzeitraum paar) {
    if (paar == null) {
      throw new IllegalArgumentException("Ein Paar ohne Wert gibt es nicht");
    }
    return new Baumfenster(paar, null, null);
  }

  /**
   * Ein freies Fenster, {@code von} einschliessend und {@code bisAusschliessend} ausschliessend.
   *
   * <p>Beide Grenzen muessen auf einer <b>vollen Stunde</b> liegen — das ist die feinste Ebene des
   * Rollups, und ein Fenster, das sie unterschreitet, koennte nur gerundet gelesen werden. Gerundet
   * wird nirgends; die Anfrage weist so etwas ab ({@code zeitfenster-zu-genau}), und hier ist es
   * ein Programmierfehler.
   */
  public static Baumfenster frei(LocalDateTime von, LocalDateTime bisAusschliessend) {
    if (von == null || bisAusschliessend == null) {
      throw new IllegalArgumentException("Ein freies Fenster ohne Grenzen gibt es nicht");
    }
    if (!istVolleStunde(von) || !istVolleStunde(bisAusschliessend)) {
      throw new IllegalArgumentException(
          "Die Grenzen eines Baumfensters liegen auf vollen Stunden");
    }
    if (!von.isBefore(bisAusschliessend)) {
      throw new IllegalArgumentException("von muss vor bisAusschliessend liegen");
    }
    return new Baumfenster(null, von, bisAusschliessend);
  }

  /** Liegt der Zeitpunkt auf einer vollen Stunde — keine Minute, keine Sekunde, kein Bruchteil? */
  public static boolean istVolleStunde(LocalDateTime zeitpunkt) {
    return zeitpunkt.truncatedTo(ChronoUnit.HOURS).equals(zeitpunkt);
  }

  public boolean istFrei() {
    return paar == null;
  }

  /** Das Paar, oder {@code null} im freien Modus. */
  public Rollupzeitraum paar() {
    return paar;
  }

  /** Der Code, wie er in der Antwort steht: der des Paares oder {@link #CODE_FREI}. */
  public String code() {
    return istFrei() ? CODE_FREI : paar.code();
  }

  /**
   * Die gelesenen Grenzen. Fuer ein Paar gegen {@code jetzt} aufgeloest, fuer ein freies Fenster
   * unabhaengig von jeder Uhr — ein freies Fenster wird absolut eingegeben.
   */
  public Zeitfenster fenster(LocalDateTime jetzt) {
    return istFrei() ? new Zeitfenster(von, bisAusschliessend) : paar.fenster(jetzt);
  }

  /**
   * Die Segmente, die zu lesen sind: <b>genau eines</b> fuer ein Paar, <b>ein bis fuenf</b> fuer
   * ein freies Fenster.
   */
  public List<Segment> segmente(LocalDateTime jetzt) {
    if (istFrei()) {
      return zerlegung(von, bisAusschliessend);
    }
    Zeitfenster fenster = paar.fenster(jetzt);
    return List.of(new Segment(paar.ebene(), fenster.von(), fenster.bis()));
  }

  /**
   * Die Zerlegung eines freien Fensters in hoechstens fuenf Segmente ueber die drei Ebenen.
   *
   * <p>Gierig, in dieser Reihenfolge: Stunden bis zur naechsten Tagesgrenze, Tage bis zur naechsten
   * Monatsgrenze, ganze Monate, Tage, Stunden bis {@code bisAusschliessend}. Was leer bleibt,
   * entsteht nicht — ein Fenster innerhalb eines Tages ist <b>ein</b> Stundensegment, ein Fenster
   * ueber ganze Tage ohne ganzen Monat <b>ein</b> Tagessegment, gleich ob es eine Monatsgrenze
   * ueberschreitet.
   *
   * <p>Zwei Eigenschaften, an denen die Kennzahlen haengen: Die Segmente sind <b>lueckenlos</b> und
   * <b>ueberschneidungsfrei</b>, und ihre Vereinigung ist {@code [von, bisAusschliessend)}. Kein
   * Eimer doppelt, keiner ausgelassen — sonst waeren die Summen still falsch. {@code
   * BaumfensterTest} haelt beides ueber eine Menge erfundener Fenster fest.
   *
   * @param von einschliessend, volle Stunde, Wanduhrzeit des Quellservers
   * @param bisAusschliessend ausschliessend, volle Stunde, nach {@code von}
   */
  public static List<Segment> zerlegung(LocalDateTime von, LocalDateTime bisAusschliessend) {
    // Der erste ganze Tag beginnt an der naechsten Tagesgrenze (oder an von, wenn von schon eine
    // ist); der letzte ganze Tag endet an der Tagesgrenze, die bisAusschliessend nicht
    // ueberschreitet.
    LocalDateTime ersterGanzerTag = tagesgrenzeAbAufwaerts(von);
    LocalDateTime letzterGanzerTag = bisAusschliessend.toLocalDate().atStartOfDay();
    if (!ersterGanzerTag.isBefore(letzterGanzerTag)) {
      // Kein ganzer Tag im Fenster: alles auf der Stundenebene, in einem Stueck.
      return List.of(new Segment(Rollupebene.STUNDE, von, bisAusschliessend));
    }

    LocalDateTime ersterGanzerMonat = monatsgrenzeAbAufwaerts(ersterGanzerTag);
    LocalDateTime letzterGanzerMonat = YearMonth.from(letzterGanzerTag).atDay(1).atStartOfDay();

    List<Segment> segmente = new ArrayList<>(5);
    fuegeHinzu(segmente, Rollupebene.STUNDE, von, ersterGanzerTag);
    if (!ersterGanzerMonat.isBefore(letzterGanzerMonat)) {
      // Kein ganzer Monat im Fenster: die ganzen Tage in einem Stueck, ueber Monatsgrenzen hinweg.
      fuegeHinzu(segmente, Rollupebene.TAG, ersterGanzerTag, letzterGanzerTag);
    } else {
      fuegeHinzu(segmente, Rollupebene.TAG, ersterGanzerTag, ersterGanzerMonat);
      fuegeHinzu(segmente, Rollupebene.MONAT, ersterGanzerMonat, letzterGanzerMonat);
      fuegeHinzu(segmente, Rollupebene.TAG, letzterGanzerMonat, letzterGanzerTag);
    }
    fuegeHinzu(segmente, Rollupebene.STUNDE, letzterGanzerTag, bisAusschliessend);
    return List.copyOf(segmente);
  }

  private static void fuegeHinzu(
      List<Segment> segmente, Rollupebene ebene, LocalDateTime von, LocalDateTime bis) {
    if (von.isBefore(bis)) {
      segmente.add(new Segment(ebene, von, bis));
    }
  }

  /** Der Zeitpunkt selbst, wenn er auf Mitternacht liegt, sonst die naechste Mitternacht. */
  private static LocalDateTime tagesgrenzeAbAufwaerts(LocalDateTime zeitpunkt) {
    LocalDateTime tagesanfang = zeitpunkt.toLocalDate().atStartOfDay();
    return tagesanfang.equals(zeitpunkt) ? zeitpunkt : tagesanfang.plusDays(1);
  }

  /** Der Zeitpunkt selbst, wenn er ein Monatsanfang ist, sonst der naechste Monatsanfang. */
  private static LocalDateTime monatsgrenzeAbAufwaerts(LocalDateTime mitternacht) {
    YearMonth monat = YearMonth.from(mitternacht);
    LocalDateTime monatsanfang = monat.atDay(1).atStartOfDay();
    return monatsanfang.equals(mitternacht)
        ? mitternacht
        : monat.plusMonths(1).atDay(1).atStartOfDay();
  }

  /**
   * Ein Abschnitt des Fensters auf <b>einer</b> Rollup-Ebene — {@code von} einschliessend, {@code
   * bis} ausschliessend, beide auf einer Eimergrenze dieser Ebene.
   *
   * <p>Die Grenzen sind {@link LocalDateTime} auf allen drei Ebenen; dass ein Tages- oder
   * Monatssegment auf Mitternacht liegt, stellt die Zerlegung sicher. Das Repository schneidet
   * daraus den {@code DATE}-Wert fuer {@code tag} und {@code monat}.
   */
  public record Segment(Rollupebene ebene, LocalDateTime von, LocalDateTime bis) {

    public Segment {
      if (ebene == null || von == null || bis == null) {
        throw new IllegalArgumentException("Ein Segment ohne Ebene oder Grenzen gibt es nicht");
      }
      if (!von.isBefore(bis)) {
        throw new IllegalArgumentException("Ein Segment ist nie leer: von liegt vor bis");
      }
    }
  }
}
