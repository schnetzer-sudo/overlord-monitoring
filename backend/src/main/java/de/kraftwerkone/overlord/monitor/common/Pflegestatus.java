package de.kraftwerkone.overlord.monitor.common;

/**
 * Die zwei Pflegezustaende einer Katalogzeile — und es sind bewusst nur zwei (E4).
 *
 * <p>Zusammen mit dem Partnerfeld tragen sie <b>drei</b> Bedeutungen, ohne dass ein dritter Zustand
 * noetig waere:
 *
 * <table border="1">
 *   <caption>Zustand und Bedeutung</caption>
 *   <tr><td>{@code OFFEN}, Feld leer</td><td>noch nicht angesehen, Heuristik hat nichts
 *       gefunden</td></tr>
 *   <tr><td>{@code OFFEN}, Feld gefuellt</td><td><b>Vorschlag</b> der Heuristik,
 *       unbestaetigt</td></tr>
 *   <tr><td>{@code GEPFLEGT}, Feld gefuellt</td><td>kuratiert</td></tr>
 *   <tr><td>{@code GEPFLEGT}, Feld leer</td><td><b>hingesehen, es gibt nichts</b></td></tr>
 * </table>
 *
 * <p>Ein dritter Zustand „nicht zuordenbar" ist ausdruecklich verworfen ({@code
 * docs/prozess-katalog.md} §9): „gepflegt mit leerem Partner" sagt dasselbe mit den Feldern, die
 * ohnehin da sind.
 *
 * <p><b>Warum diese Aufzaehlung in {@code common} steht und nicht in {@code catalog}</b> <i>(seit
 * 31.08.2026, Schritt 10b-2)</i>. Der Verteilungsblock des Dashboards braucht sie: Ob ein Prozess
 * als <i>zugeordnet</i> gilt, haengt nach Entscheidung E-i unter anderem daran, ob seine
 * Katalogzeile {@code GEPFLEGT} traegt. <b>Fachpakete kennen einander nicht</b> — braucht ein
 * zweites einen Typ, wandert der Typ nach {@code common} und nicht ins Nachbarpaket ({@code
 * PaketstrukturTest.fachpakete_kennen_einander_nicht}). Die Alternative waere ein
 * Zeichenkettenliteral {@code "GEPFLEGT"} im Dashboard gewesen — dieselbe Bedingung an zwei
 * Stellen, und die driftet.
 */
public enum Pflegestatus {
  OFFEN,
  GEPFLEGT
}
