package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Die Stellschrauben des Rollup-Laufs. Sie stehen in {@code application.yml} und nicht als Literal
 * im Code, damit im Betrieb sichtbar ist, was gilt, ohne dass jemand eine Klasse aufschlagen muss.
 * Es sind keine Zugangsdaten; sie duerfen deshalb in der versionierten Datei stehen.
 *
 * <p><b>Die beiden Zeitplaene stehen nicht hier</b>, sondern werden von {@link RollupPlaner} als
 * Platzhalter gelesen ({@code ${overlord.rollup.delta-plan}} und {@code
 * ${overlord.rollup.voll-plan}}) — {@code @Scheduled} nimmt eine Zeichenkette und kein
 * Datensatzfeld. Sie sind hier trotzdem genannt, damit die Liste der Schluessel an einer Stelle
 * vollstaendig ist:
 *
 * <table border="1">
 *   <caption>Alle Schluessel unter {@code overlord.rollup}</caption>
 *   <tr><th>Schluessel</th><th>Vorgabe</th><th>Wirkung</th></tr>
 *   <tr><td>{@code aktiv}</td><td>{@code false}, wenn nicht gesetzt</td>
 *       <td>schaltet die zeitgesteuerte Ausloesung ab, ohne das Profil zu aendern</td></tr>
 *   <tr><td>{@code delta-plan}</td><td>{@code 0 5 * * * *}</td>
 *       <td>Cron des stuendlichen Laufs</td></tr>
 *   <tr><td>{@code voll-plan}</td><td>{@code 0 0 3 * * *}</td>
 *       <td>Cron des naechtlichen Laufs — <b>ungemessen</b>, siehe unten</td></tr>
 *   <tr><td>{@code scheiben-pause}</td><td>{@code 1s}</td>
 *       <td>Drosselung zwischen zwei Monatsscheiben (Leistungsregel L6)</td></tr>
 *   <tr><td>{@code beim-start}</td><td>—</td>
 *       <td>{@code DELTA} oder {@code VOLL}: ein einmaliger Lauf beim Start, von Hand</td></tr>
 * </table>
 *
 * @param aktiv Ob die zeitgesteuerte Ausloesung laeuft. <b>Zweiter Riegel neben dem Profil {@code
 *     rollup}</b>: Ohne das Profil gibt es {@link RollupPlaner} gar nicht; mit dem Profil und
 *     {@code aktiv=false} gibt es ihn, aber er feuert nicht. Der zweite Riegel steht da, weil das
 *     Profil im Betrieb aus einer Umgebungsvariablen kommt und eine Abschaltung dann eine
 *     Neubereitstellung kostet.
 *     <p><b>Fehlt der Schluessel, ist die Ausloesung AUS</b> — ein {@code boolean} ohne Angabe ist
 *     {@code false}, und das ist hier die richtige Richtung: Ein Job, der wegen eines vergessenen
 *     Schluessels unaufgefordert in eine geteilte Datenbank schreibt, waere die Ueberraschung, die
 *     dieser Riegel verhindern soll. {@code application.yml} setzt ihn ausdruecklich
 * @param scheibenPause Wie lange der Volllauf zwischen zwei Monatsscheiben wartet.
 *     <b>Leistungsregel L6 („Der Rollup-Job laeuft gedrosselt. Er teilt sich die Instanz mit der
 *     Produktion."), und sie greift genau hier</b> — beim <b>Delta</b>-Lauf ist sie Zeremonie: Er
 *     hat genau eine Scheibe, wartet also nie, und mit 88 ms je Stunde belegt er die Instanz zu
 *     0,0024 % (M88).
 *     <p><b>Der Wert ist ungemessen (Regel Q4).</b> Wie viel Last die Produktionsinstanz nachts
 *     vertraegt, ist nicht erhoben. Eine Sekunde je Scheibe verlaengert den Volllauf ueber den
 *     Bestand der Testkopie um 22 Sekunden — nachts folgenlos — und halbiert die anhaltende
 *     Belegung. Der offene Punkt steht in {@code docs/rollup.md}
 * @param beimStart Ein <b>einmaliger</b> Lauf beim Start der Anwendung, ausgeloest ueber
 *     Konfiguration oder Startparameter ({@code --overlord.rollup.beim-start=VOLL}). {@code null}
 *     heisst: nichts.
 *     <p><b>Bewusst kein HTTP-Endpunkt.</b> Ein Endpunkt, der die Rolluptabelle neu schreibt, waere
 *     eine Schreibflaeche im Anfragepfad — und die gaebe es dann auch fuer den, der sie nicht
 *     bedienen soll. Der Startparameter ist der Weg, der nur dem offensteht, der die Anwendung
 *     startet
 */
@ConfigurationProperties("overlord.rollup")
public record RollupEigenschaften(boolean aktiv, Duration scheibenPause, LaufArt beimStart) {

  /**
   * Das Profil, unter dem die zeitgesteuerte Ausloesung ueberhaupt entsteht.
   *
   * <p><b>Ein Profil und kein eigenes Wurzelpaket</b> ({@code PROJEKTBESCHREIBUNG.md} §6: „Der
   * Rollup-Job bekommt trotz separater Startbarkeit kein eigenes Wurzelpaket. Die Trennung laeuft
   * ueber das Spring-Profil, nicht ueber den Namensraum."). Im Betrieb laesst sich damit dieselbe
   * Anwendung ein zweites Mal starten — {@code SPRING_PROFILES_ACTIVE=prod,rollup} —, ohne dass ein
   * zweites Artefakt gebaut werden muesste.
   */
  public static final String PROFIL = "rollup";

  /** Vorgaben fuer den Fall, dass nichts konfiguriert ist. */
  public RollupEigenschaften {
    if (scheibenPause == null || scheibenPause.isNegative()) {
      scheibenPause = Duration.ofSeconds(1);
    }
  }
}
