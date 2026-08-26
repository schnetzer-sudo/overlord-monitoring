package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Der Zeitschnitt eines Laufs: <b>von</b> einschliesslich, <b>bis</b> ausschliesslich, und beide
 * Grenzen liegen immer auf einem <b>vollen Stundenanfang</b>.
 *
 * <p>Beide Zeitpunkte sind <b>Datenzeit</b> — Wanduhrzeit des Quellservers, genau so, wie {@code
 * Message.MessageLastUpdate} sie fuehrt ({@code docs/datenzugriff.md} §7). Gebildet werden sie mit
 * der <b>Anwendungsuhr</b>, nie mit {@code systemClock}; siehe {@link RollupUhren}.
 *
 * <h2>Warum die Ausdehnung auf ganze Stunden der Kern ist und keine Formalie</h2>
 *
 * <p>Das Nachlauffenster von {@value #NACHLAUF_MINUTEN} Minuten ist ein Rueckgriff auf den
 * <b>Zeilenzeitstempel</b>, aber die Schreibeinheit ist der <b>ganze Stundeneimer</b>. Wuerde nur
 * der rohe Bereich gelesen und dazugezaehlt, zaehlte der Anteil zwischen Wasserstand minus Nachlauf
 * und Wasserstand <b>doppelt</b>. Deshalb: ganze Eimer, geloescht und neu geschrieben (siehe {@code
 * RollupJob}).
 *
 * <p>Daraus folgt eine Eigenschaft, auf der der ganze Ablauf ruht: Weil der Wasserstand selbst
 * immer ein Stundenanfang ist, faellt <em>Wasserstand minus Nachlauf</em> stets in die
 * <b>vorige</b> Stunde. Der zuletzt geschriebene Eimer wird also bei jedem Delta-Lauf noch einmal
 * vollstaendig gerechnet — und genau das macht es unbedenklich, dass der obere Rand eine
 * <b>angebrochene</b> Stunde einschliesst. Sie wird beim naechsten Lauf ersetzt, nicht ergaenzt.
 *
 * <h2>Das Nachlauffenster ist gemessen und nicht gewaehlt</h2>
 *
 * <p>M86 hat ueber 220.579 Nachrichten und alle elf Statuswerte geprueft, wie weit {@code
 * MessageLastUpdate} nachtraeglich wandert: <b>hoechstens vier Sekunden</b>, kein einziger Wert
 * ueber 60 Sekunden, Anteil der nachgeschriebenen Zeilen 0,62 % bis 2,17 %. Ein Nachlauffenster von
 * 48 Stunden — als Moeglichkeit mitgefuehrt — waere um den Faktor 43.200 ueberdimensioniert. Ein
 * Wasserstand <b>ohne</b> Nachlauffenster ist aber ebenfalls nicht gedeckt: Die 1.548
 * nachgeschriebenen Zeilen des Messfensters sind gemessen und nicht null.
 *
 * <p><b>Belegvermerk (Regel L10).</b> <i>Gemessen ist:</i> auf dem Bestand der Testkopie wandert
 * {@code MessageLastUpdate} um hoechstens vier Sekunden. <i>Behauptet wird:</i> fuenfzehn Minuten
 * genuegen. Die Luecke: {@code RUNNING} kommt auf der Testkopie null Mal vor, und {@code
 * MatchInterchange} — der Job, der {@code COMMIT_RECEIVED} nachtraeglich setzt — laeuft dort nicht
 * sichtbar (M31-3, Takt ungedeckt). Der zulaessige Satz lautet „auf diesem Bestand nicht
 * beobachtet", nicht „findet nicht statt". Deshalb der Sicherheitsabstand von Faktor 225 gegenueber
 * der gemessenen Obergrenze — und deshalb ist der naechtliche Volllauf keine Zierde.
 *
 * @param von untere Grenze, <b>einschliesslich</b>, immer ein voller Stundenanfang
 * @param bis obere Grenze, <b>ausschliesslich</b>, immer ein voller Stundenanfang
 */
public record RollupFenster(LocalDateTime von, LocalDateTime bis) {

  /**
   * Das Nachlauffenster in Minuten.
   *
   * <p><b>Bewusst eine Konstante und kein Konfigurationswert.</b> Es ist eine Entscheidung mit
   * einer Messung dahinter (M86) und keine Betriebseinstellung; ein Drehknopf luede dazu ein, sie
   * ohne neue Messung zu verstellen. Zeigt die Produktion eine laengere Wanderung, ist das ein
   * Befund und eine Codeaenderung wert — der offene Punkt dazu steht in {@code docs/rollup.md}.
   */
  public static final int NACHLAUF_MINUTEN = 15;

  /** Das Nachlauffenster als Dauer. */
  public static final Duration NACHLAUF = Duration.ofMinutes(NACHLAUF_MINUTEN);

  public RollupFenster {
    if (von == null || bis == null) {
      throw new IllegalArgumentException("Ein Rollup-Fenster ohne Grenzen gibt es nicht");
    }
    // Die Invariante, auf der Loeschen-und-Neuschreiben ruht: Es werden ausschliesslich GANZE
    // Stundeneimer ersetzt. Ein Fenster, das mitten in einer Stunde beginnt, loeschte einen Eimer
    // ganz und schriebe ihn nur zum Teil neu — der Fehler waere still und stuende erst im
    // Dashboard.
    verlangeStundenanfang(von, "von");
    verlangeStundenanfang(bis, "bis");
    if (von.isAfter(bis)) {
      throw new IllegalArgumentException(
          "Ein Rollup-Fenster laeuft nie rueckwaerts: von=" + von + ", bis=" + bis);
    }
  }

  private static void verlangeStundenanfang(LocalDateTime zeitpunkt, String name) {
    if (!zeitpunkt.equals(zeitpunkt.truncatedTo(ChronoUnit.HOURS))) {
      throw new IllegalArgumentException(
          "Die Grenze " + name + " muss auf einem vollen Stundenanfang liegen, war: " + zeitpunkt);
    }
  }

  /**
   * Das Fenster des <b>Delta-Laufs</b>: vom Wasserstand minus Nachlauf bis jetzt, ausgedehnt auf
   * ganze Stunden.
   *
   * @param wasserstand {@code MAX(fenster_bis)} ueber die abgeschlossenen, fehlerfreien Laeufe —
   *     oder, wenn nie gerechnet wurde, {@code MIN(Message.MessageLastUpdate)}
   * @param jetzt der Referenzzeitpunkt aus der <b>Anwendungsuhr</b>
   */
  public static RollupFenster delta(LocalDateTime wasserstand, LocalDateTime jetzt) {
    return ausgedehnt(wasserstand.minus(NACHLAUF), jetzt);
  }

  /**
   * Das Fenster des <b>Volllaufs</b>: vom fruehesten Zeitstempel des Bestands bis jetzt.
   *
   * <p><b>Kein Nachlauf.</b> Er begaenne ohnehin vor der ersten Zeile und verschoebe nur die untere
   * Grenze um eine leere Stunde.
   *
   * <p>Auf der Testkopie sind das 22 Monatsscheiben; fuenf davon (2026-01 bis 2026-05) tragen null
   * Zeilen. <b>Sie werden trotzdem mitgerechnet</b> — der Lauf laeuft ueber einen Kalender und
   * nicht ueber die vorhandenen Daten. Eine leere Scheibe kostet 1 ms (M92); sie zu ueberspringen
   * waere eine Optimierung ohne Gegenwert und genau die, die den Lauf falsch machte.
   *
   * @param fruehesteAenderung {@code MIN(Message.MessageLastUpdate)} ueber den Gesamtbestand
   * @param jetzt der Referenzzeitpunkt aus der <b>Anwendungsuhr</b>
   */
  public static RollupFenster voll(LocalDateTime fruehesteAenderung, LocalDateTime jetzt) {
    return ausgedehnt(fruehesteAenderung, jetzt);
  }

  /**
   * Rundet den Beginn auf den Anfang seiner Stunde ab und das Ende auf den Anfang der
   * <b>naechsten</b> Stunde auf.
   *
   * <p><b>Die naechste Stunde und nicht die aufgerundete:</b> Liegt {@code jetzt} genau auf {@code
   * 14:00:00}, ist das Ende {@code 15:00} und nicht {@code 14:00}. Sonst haette der Lauf genau auf
   * der Stundengrenze ein leeres Fenster und die eben begonnene Stunde bliebe bis zum naechsten
   * Lauf ungerechnet — abhaengig davon, ob der Ausloeser eine Sekunde vor oder nach der vollen
   * Stunde feuert. Ein Verhalten, das an der Sekunde des Weckers haengt, ist kein Verhalten.
   *
   * <p><b>Der Sonderfall {@code von > bis} wird zu einem leeren Fenster</b> und nicht zu einer
   * Ausnahme. Er verlangt, dass der Wasserstand <b>mehr als eine Stunde</b> vor der Anwendungsuhr
   * liegt — dass ein Lauf unmittelbar auf den vorigen folgt, genuegt dafuer <b>nicht</b>: Der
   * Rueckgriff zieht {@code von} dann in die Stunde vor {@code bis}, und der zuletzt geschriebene
   * Eimer wird wie vorgesehen noch einmal gerechnet.
   *
   * <p>Wirklich leer wird das Fenster erst, wenn die <b>Uhr zurueckspringt</b> — im Profil {@code
   * dev} der praktische Fall: Nach einer Neubefuellung der Testkopie liegt der ermittelte Anker
   * frueher als zuvor, und der Wasserstand aus den alten Laeufen steht dann in der Zukunft. Das ist
   * kein Fehler, sondern ein Zustand: Bis dorthin <em>ist</em> gerechnet. Der Lauf schreibt
   * trotzdem seine Protokollzeile, damit im Nachhinein sichtbar bleibt, dass er stattgefunden hat
   * und nichts zu tun war.
   */
  private static RollupFenster ausgedehnt(LocalDateTime rohesVon, LocalDateTime rohesBis) {
    LocalDateTime bis = rohesBis.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    LocalDateTime von = rohesVon.truncatedTo(ChronoUnit.HOURS);
    return new RollupFenster(von.isAfter(bis) ? bis : von, bis);
  }

  /**
   * Zerlegt das Fenster in <b>Monatsscheiben</b> — die Einheit, in der gelesen wird.
   *
   * <h2>Warum ueberhaupt geschnitten wird: eine gemessene Grenze, keine Vorsicht</h2>
   *
   * <p>Der Lese-Pool setzt {@code SET SESSION max_statement_time=10} ({@code docs/datenzugriff.md}
   * §1). Ein einziges Statement ueber den Gesamtbestand reisst diese Grenze — <b>gemessen am
   * 26.08.2026: Fehler 1969 „Query execution was interrupted" nach zehn Sekunden</b>. Der Volllauf
   * ist ohne Schnitt also nicht baubar, und das ist keine Auslegung.
   *
   * <p>Der Monat ist die Einheit, die M92 gemessen hat: <b>2,575 s</b> fuer die groesste Scheibe
   * (2025-07, 248.320 Zeilen, beste von fuenf am 26.08.2026; M92 nennt 2,745 s). Das laesst
   * <b>Faktor 3,9</b> Luft zur Zeitgrenze.
   *
   * <h2>Ueber den Kalender, nicht ueber die Daten</h2>
   *
   * <p>Das ist die Bauvorgabe aus M92 im Wortlaut: „Er iteriert ueber einen Kalender, nicht ueber
   * die vorhandenen Daten." Eine leere Scheibe wird also <b>mitgerechnet</b> und nicht
   * uebersprungen — auf der Testkopie sind das die fuenf Monate 2026-01 bis 2026-05. Sie kosten 1
   * ms je Scheibe; sie zu ueberspringen waere eine Optimierung ohne Gegenwert und genau die, die
   * den Lauf falsch machte: Ein Eimer, der einmal Zeilen hatte und heute keine mehr hat, wuerde
   * sonst nie geleert.
   *
   * <p>Die <b>erste</b> Scheibe ist in der Regel angebrochen (vom Fensteranfang bis zum
   * Monatsersten), die letzte ebenso. Das aendert nichts an der Eimertreue: Beide Grenzen bleiben
   * Stundenanfaenge, und ein Monatserster um Mitternacht ist einer.
   *
   * @return die Scheiben in aufsteigender Reihenfolge, lueckenlos und ohne Ueberlapp; leer, wenn
   *     das Fenster leer ist
   */
  public List<RollupFenster> monatsscheiben() {
    List<RollupFenster> scheiben = new ArrayList<>();
    LocalDateTime anfang = von;
    while (anfang.isBefore(bis)) {
      LocalDateTime monatswechsel =
          anfang.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay();
      LocalDateTime ende = monatswechsel.isBefore(bis) ? monatswechsel : bis;
      scheiben.add(new RollupFenster(anfang, ende));
      anfang = ende;
    }
    return scheiben;
  }

  /** Nichts zu rechnen — beide Grenzen fallen zusammen. */
  public boolean istLeer() {
    return von.equals(bis);
  }

  /** Wie viele Stundeneimer das Fenster umfasst. Nur fuer Protokollmeldungen. */
  public long stundeneimer() {
    return ChronoUnit.HOURS.between(von, bis);
  }

  @Override
  public String toString() {
    return von + " bis " + bis + " (" + stundeneimer() + " Stundeneimer)";
  }
}
