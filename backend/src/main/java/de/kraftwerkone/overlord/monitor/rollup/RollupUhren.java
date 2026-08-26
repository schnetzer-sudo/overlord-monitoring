package de.kraftwerkone.overlord.monitor.rollup;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * <b>Die zwei Uhren des Rollup-Laufs, und die Zuordnung ist nicht verhandelbar.</b>
 *
 * <p>In derselben Zeile von {@code rollup_lauf} stehen zwei Zeitpunkte aus <b>verschiedenen</b>
 * Uhren. Das ist Absicht und die Stelle, an der bei diesem Schritt am ehesten etwas schiefgeht —
 * deshalb steht die Zuordnung in einer eigenen, ohne Datenbank pruefbaren Klasse und nicht verteilt
 * im Job.
 *
 * <table border="1">
 *   <caption>Welche Uhr wofuer</caption>
 *   <tr><th>Spalte</th><th>Uhr</th><th>Warum</th></tr>
 *   <tr>
 *     <td>{@code fenster_von}, {@code fenster_bis}</td>
 *     <td><b>Anwendungsuhr</b> ({@code Clock}, {@code @Primary})</td>
 *     <td><b>Datenzeit.</b> Sie beschreiben, welchen Ausschnitt aus {@code GlassfishDB} der Lauf
 *         verarbeitet hat, und werden gegen {@code Message.MessageLastUpdate} gehalten. Im Profil
 *         {@code dev} liegt die Anwendungsuhr beim Anker der Testkopie — und genau so soll der Lauf
 *         dort auch rechnen. Mit der Systemuhr rechnete er lokal ueber einem leeren Zeitraum und
 *         schriebe null Zeilen</td>
 *   </tr>
 *   <tr>
 *     <td>{@code gestartet_am}, {@code beendet_am}</td>
 *     <td><b>{@code systemClock}</b> (echte Uhr, UTC)</td>
 *     <td><b>Protokollzeit</b>, wie {@code geaendert_am} und das {@code audit_log} (Regel A5). Eine
 *         Protokollzeile mit zurueckversetzter Uhr waere im Betrieb unlesbar: Sie behauptete, der
 *         naechtliche Lauf habe im Dezember stattgefunden</td>
 *   </tr>
 * </table>
 *
 * <p><b>Im Profil {@code dev} liegen die beiden Paare Monate auseinander</b> — Stand 26.08.2026
 * rund 214 Tage. Das ist der erwartete Anblick und kein Fehler. In Produktion ist die Anwendungsuhr
 * die Systemuhr; dort unterscheiden sich die Paare nur noch um die Zone.
 *
 * <p><b>Regel Z1 ist eingehalten:</b> {@code LocalDateTime.now()} wird nicht aufgerufen. Beide
 * Werte entstehen aus {@link Clock#instant()} — dieselbe Form wie in {@code audit/AuditLogWriter}
 * und {@code admin/BenutzerverwaltungService}.
 */
@Component
public class RollupUhren {

  private final Clock anwendungsuhr;
  private final Clock systemuhr;

  RollupUhren(Clock anwendungsuhr, @Qualifier("systemClock") Clock systemuhr) {
    this.anwendungsuhr = anwendungsuhr;
    this.systemuhr = systemuhr;
  }

  /**
   * <b>Datenzeit:</b> der Referenzzeitpunkt, gegen den Fenstergrenzen gebildet werden.
   *
   * <p>Wanduhrzeit in der Zone der Anwendungsuhr — dieselbe Zone, mit der {@code
   * common/Zeitfenster} relative Fenster bildet und {@code common/Zeitpunkte} zwischen Quelle und
   * API umrechnet. Es kommt also keine neue Annahme hinzu.
   */
  public LocalDateTime datenzeit() {
    return LocalDateTime.ofInstant(anwendungsuhr.instant(), anwendungsuhr.getZone());
  }

  /**
   * <b>Protokollzeit:</b> der Zeitpunkt, zu dem der Lauf tatsaechlich gelaufen ist, in UTC.
   *
   * <p>Niemals die Anwendungsuhr — auch dann nicht, wenn beide im Profil {@code prod} dasselbe
   * liefern. Die Zuordnung haengt an der Bedeutung der Spalte und nicht daran, ob der Unterschied
   * gerade sichtbar waere.
   */
  public LocalDateTime protokollzeit() {
    return LocalDateTime.ofInstant(systemuhr.instant(), ZoneOffset.UTC);
  }
}
