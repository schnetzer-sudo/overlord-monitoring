package de.kraftwerkone.overlord.monitor.message;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Die Schalter der Nachrichtenliste.
 *
 * <table>
 *   <caption>Schluessel unter {@code overlord.nachrichtenliste}</caption>
 *   <tr><th>Schluessel</th><th>Bedeutung</th></tr>
 *   <tr>
 *     <td>{@code verengung}</td>
 *     <td>Ob die Liste ihr Zeitfenster vorab ueber {@code message_rollup} verengt
 *         ({@link Fensterverengung}). Vorgabe in {@code application.yml}: {@code true}.</td>
 *   </tr>
 * </table>
 *
 * <h2>Warum es diesen Schalter gibt</h2>
 *
 * <p>Die Verengung ist die <b>erste Stelle, an der eine Kernabfrage von einer Tabelle abhaengt, die
 * dieses Projekt selbst fortschreibt</b>. Bis hierher war {@code message_rollup} ausschliesslich
 * die Datenquelle des Dashboards; faellt sie aus, faellt das Dashboard aus, und das ist
 * ertraeglich. Die Liste ist es nicht: Sie ist die Frage, wegen der es dieses Werkzeug gibt („wo
 * steht mein Beleg?"). Ist der Rollup kaputt, hinterher oder schlicht leer, muss die Liste ohne ihn
 * weiterlaufen.
 *
 * <p><b>Der Schalter ist nicht der einzige Schutz, sondern der letzte.</b> Ein hinterherhinkender
 * Rollup faengt sich schon ueber den Wasserstand ab (oberhalb wird unverengt gefragt), ein leerer
 * ueber die fehlende Untergrenze. Der Schalter ist fuer den Fall gedacht, den niemand vorhergesehen
 * hat — und dafuer, dass man ihn umlegen kann, ohne ein neues Programmstueck auszuliefern.
 *
 * <h2>Ein fehlender Schluessel schaltet ab, nicht ein</h2>
 *
 * <p>{@code boolean} ohne Eintrag ist {@code false}. Das ist die sichere Richtung und dieselbe
 * Entscheidung wie bei {@code RollupEigenschaften.aktiv}: Wer die Konfiguration verliert, bekommt
 * das Verhalten von vor dem 30.08.2026 und keine halbe Verengung. In {@code application.yml} steht
 * der Wert trotzdem ausdruecklich — ein Vorgabewert, den man nur durch Weglassen bekommt, ist beim
 * naechsten Umbau wieder weg.
 */
@ConfigurationProperties("overlord.nachrichtenliste")
public record NachrichtenlisteEigenschaften(boolean verengung) {}
