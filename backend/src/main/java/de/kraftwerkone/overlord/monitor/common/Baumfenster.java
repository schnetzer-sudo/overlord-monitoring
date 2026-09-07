package de.kraftwerkone.overlord.monitor.common;

import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

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

  /**
   * Die Wahl des Aufrufers aus den drei Parametern der Anfrage — <b>{@code zeitraum} oder {@code
   * von}/{@code bis}</b>, nie beides, und sieben Arten, es falsch zu machen.
   *
   * <p><b>Die Codes sind aus der Nachrichtenliste uebernommen und nicht neu erfunden</b> ({@code
   * common/Zeitfenster}, {@code docs/nachrichtenliste.md} §2), bis auf {@code
   * zeitfenster-zu-genau}. In dieser Reihenfolge geprueft:
   *
   * <ol>
   *   <li>{@code zeitraum-unbekannt} — der Code ist keines der drei Paare ({@link
   *       Rollupzeitraum#ausCode}, unveraendert)
   *   <li>{@code zeitpunkt-ungueltig} — ein Zeitpunkt ist nicht als ISO-Zeitpunkt lesbar ({@link
   *       Zeitpunkte#ausIso}, unveraendert)
   *   <li>{@code zeitfenster-mehrdeutig} — {@code zeitraum} <b>und</b> {@code von}/{@code bis}.
   *       <b>Keine stille Vorrangregel</b>, dieselbe Festlegung wie in der Liste: Wer beides
   *       schickt, hat eine Vorstellung davon, welches gewinnt; raet das Backend, bekommt er ohne
   *       Hinweis ein anderes Fenster als gedacht
   *   <li>{@code zeitfenster-unvollstaendig} — nur einer der beiden Zeitpunkte
   *   <li>{@code zeitfenster-zu-genau} — ein Zeitpunkt liegt <b>nicht auf einer vollen Stunde</b>.
   *       <b>Abgewiesen, nicht gerundet:</b> Nach unten runden weitete das Fenster ({@code von})
   *       beziehungsweise beschnitte es ({@code bis}). Beides verstoesst gegen die tragende Vorgabe
   *       — alles kommt aus dem abgefragten Zeitraum, und alles aus ihm kommt vor. Die Oberflaeche
   *       laesst den Zustand ueber {@code step=3600} gar nicht erst entstehen; die Pruefung faengt
   *       die von Hand gebaute Adresse. <b>Geprueft wird der in die Zone der Anwendungsuhr
   *       umgerechnete Wert</b>, nicht der UTC-Eingang: Bei einer Zone mit halbstuendigem Versatz
   *       waeren das zwei verschiedene Aussagen
   *   <li>{@code zeitfenster-ungueltig} — {@code von} liegt hinter {@code bis}. Gleich ist erlaubt
   *       und heisst <i>ein Stundeneimer</i>, weil {@code bis} einschliessend ist
   *   <li>{@code zeitfenster-zu-gross} — die Spanne uebersteigt ein <b>Kalenderjahr</b> ({@code
   *       von.isBefore(bisAusschliessend.minusYears(1))}), nicht 365 Tage; sonst hinge die Grenze
   *       am Schaltjahr
   * </ol>
   *
   * <h2>{@code bis} ist in der Anfrage einschliessend, {@code fenster.bis} in der Antwort
   * ausschliessend</h2>
   *
   * <p>In der Anfrage ist {@code bis} die <b>letzte enthaltene Stunde</b> — beidseitig geschlossen,
   * genau wie {@code von}/{@code bis} der Nachrichtenliste. Wer {@code bis = 30.12. 23:00}
   * eintraegt, bekommt den Eimer 23:00 bis 24:00 mit. Das Fenster, das gelesen wird, endet
   * <b>ausschliessend</b> bei {@code bis + 1 Stunde}, und diese Stunde wird <b>hier</b> gerechnet,
   * in der Zone der Anwendungsuhr — nicht im Browser, weil eine Stunde am Umstellungstag keine
   * Stunde ist. Jede Seite haelt damit die Konvention ihrer Nachbarn: {@code von}/{@code bis} sind
   * im Projekt beidseitig geschlossen, {@code fenster} ist im Baum seit 10c-1 ausschliessend.
   *
   * <p><b>Kein Fehler ueber die Zeit hinaus.</b> Ein Fenster in der Zukunft wird nicht abgewiesen —
   * es liefert Nullen, und das ist eine richtige Antwort. Ein freies Fenster wird absolut
   * eingegeben und gegen keine Uhr aufgeloest.
   *
   * @param zone die Zone der Anwendungsuhr — die eine Umrechnung zwischen UTC der API und der
   *     Wanduhrzeit des Quellservers ({@link Zeitpunkte})
   * @return {@code null}, wenn nichts angegeben ist — dann waehlt der Dienst die Vorgabe
   * @throws FachlicheAusnahme {@code 400} in den sieben Faellen oben
   */
  public static Baumfenster ausAnfrage(String zeitraum, String von, String bis, ZoneId zone) {
    Rollupzeitraum paar = Rollupzeitraum.ausCode(zeitraum);
    LocalDateTime vonWanduhr = hatWert(von) ? Zeitpunkte.ausIso(von.trim(), zone, "von") : null;
    LocalDateTime bisWanduhr = hatWert(bis) ? Zeitpunkte.ausIso(bis.trim(), zone, "bis") : null;
    boolean absolut = vonWanduhr != null || bisWanduhr != null;

    if (paar != null && absolut) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-mehrdeutig",
          "Zeitfenster mehrdeutig",
          "Gib entweder einen Zeitraum oder die beiden Zeitpunkte an, nicht beides.",
          "zeitraum und von/bis gleichzeitig gesetzt");
    }
    if (paar != null) {
      return paar(paar);
    }
    if (!absolut) {
      return null;
    }
    if (vonWanduhr == null || bisWanduhr == null) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-unvollstaendig",
          "Zeitfenster unvollstaendig",
          "Ein freies Zeitfenster braucht beide Zeitpunkte: von und bis.",
          "Nur eine der beiden Grenzen gesetzt");
    }
    if (!istVolleStunde(vonWanduhr) || !istVolleStunde(bisWanduhr)) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-zu-genau",
          "Zeitfenster zu genau",
          "Beide Zeitpunkte muessen auf einer vollen Stunde liegen; die Kennzahlen werden"
              + " stundenweise gefuehrt und nicht gerundet.",
          "von oder bis liegt nicht auf einer vollen Stunde der Anwendungszone");
    }
    if (vonWanduhr.isAfter(bisWanduhr)) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-ungueltig",
          "Zeitfenster ungueltig",
          "Der Zeitpunkt bis darf nicht vor von liegen.",
          "von liegt hinter bis");
    }
    // bis ist einschliessend: Die letzte enthaltene Stunde endet eine Stunde spaeter, und die
    // Stunde wird hier in Wanduhrzeit gerechnet — am Umstellungstag ist sie keine Stunde.
    LocalDateTime bisAusschliessend = bisWanduhr.plusHours(1);
    // Ein Jahr als Kalenderjahr, nicht als 365 Tage — sonst haengt die Grenze am Schaltjahr.
    if (vonWanduhr.isBefore(bisAusschliessend.minusYears(1))) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "zeitfenster-zu-gross",
          "Zeitfenster zu gross",
          "Das Zeitfenster darf hoechstens ein Jahr umfassen.",
          "Zeitfenster ueber ein Jahr angefragt");
    }
    return frei(vonWanduhr, bisAusschliessend);
  }

  private static boolean hatWert(String wert) {
    return wert != null && !wert.isBlank();
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
